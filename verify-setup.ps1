# ByteBazar Setup Verification Script
# Checks if all prerequisites and configurations are correct

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ByteBazar Setup Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allGood = $true

# Check Java
Write-Host "Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    if ($javaVersion -match "17|18|19|20|21") {
        Write-Host "✓ Java 17+ found: $javaVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ Java 17+ required. Found: $javaVersion" -ForegroundColor Red
        $allGood = $false
    }
} catch {
    Write-Host "✗ Java not found. Please install Java 17+" -ForegroundColor Red
    $allGood = $false
}

# Check Maven
Write-Host "Checking Maven..." -ForegroundColor Yellow
try {
    $mvnVersion = mvn -version 2>&1 | Select-String "Apache Maven"
    Write-Host "✓ Maven found: $mvnVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Maven not found. Please install Maven 3.6+" -ForegroundColor Red
    $allGood = $false
}

# Check Docker
Write-Host "Checking Docker..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "✓ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not running. Please start Docker Desktop" -ForegroundColor Red
    $allGood = $false
}

# Check if ports are available
Write-Host "Checking port availability..." -ForegroundColor Yellow
$ports = @(8080, 8081, 8082, 8083, 8084, 8085, 5432, 6379, 4566)
$portsInUse = @()

foreach ($port in $ports) {
    $connection = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue -InformationLevel Quiet
    if ($connection) {
        $portsInUse += $port
    }
}

if ($portsInUse.Count -gt 0) {
    Write-Host "⚠ The following ports are already in use: $($portsInUse -join ', ')" -ForegroundColor Yellow
    Write-Host "  You may need to stop existing services" -ForegroundColor Yellow
} else {
    Write-Host "✓ All required ports are available" -ForegroundColor Green
}

# Check if docker-compose.yml exists and has volume mount
Write-Host "Checking docker-compose.yml configuration..." -ForegroundColor Yellow
if (Test-Path "docker-compose.yml") {
    $dockerCompose = Get-Content "docker-compose.yml" -Raw
    if ($dockerCompose -match "./init-db:/docker-entrypoint-initdb.d") {
        Write-Host "✓ docker-compose.yml has database initialization configured" -ForegroundColor Green
    } else {
        Write-Host "✗ docker-compose.yml missing database initialization volume" -ForegroundColor Red
        $allGood = $false
    }
} else {
    Write-Host "✗ docker-compose.yml not found" -ForegroundColor Red
    $allGood = $false
}

# Check if init-db script exists
Write-Host "Checking database initialization script..." -ForegroundColor Yellow
if (Test-Path "init-db\01-init-databases.sql") {
    Write-Host "✓ Database initialization script found" -ForegroundColor Green
} else {
    Write-Host "✗ init-db\01-init-databases.sql not found" -ForegroundColor Red
    $allGood = $false
}

# Check catalog-service context-path removed
Write-Host "Checking catalog-service configuration..." -ForegroundColor Yellow
if (Test-Path "catalog-service\src\main\resources\application.yml") {
    $catalogConfig = Get-Content "catalog-service\src\main\resources\application.yml" -Raw
    if ($catalogConfig -match "context-path: /catalog") {
        Write-Host "⚠ catalog-service still has context-path configured (should be removed)" -ForegroundColor Yellow
    } else {
        Write-Host "✓ catalog-service configuration correct" -ForegroundColor Green
    }
} else {
    Write-Host "✗ catalog-service application.yml not found" -ForegroundColor Red
    $allGood = $false
}

# Check gateway routes
Write-Host "Checking gateway routes..." -ForegroundColor Yellow
if (Test-Path "gateway-service\src\main\resources\application.yml") {
    $gatewayConfig = Get-Content "gateway-service\src\main\resources\application.yml" -Raw
    if ($gatewayConfig -match "api/v1/categories") {
        Write-Host "✓ Gateway routes configured (including categories)" -ForegroundColor Green
    } else {
        Write-Host "⚠ Gateway may be missing category routes" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ gateway-service application.yml not found" -ForegroundColor Red
    $allGood = $false
}

# Check if all service modules exist
Write-Host "Checking service modules..." -ForegroundColor Yellow
$services = @("auth-service", "gateway-service", "customer-service", "catalog-service", "order-service", "payment-service")
$missingServices = @()

foreach ($service in $services) {
    if (-not (Test-Path $service)) {
        $missingServices += $service
    }
}

if ($missingServices.Count -gt 0) {
    Write-Host "✗ Missing service modules: $($missingServices -join ', ')" -ForegroundColor Red
    $allGood = $false
} else {
    Write-Host "✓ All service modules present" -ForegroundColor Green
}

# Check pom.xml exists
Write-Host "Checking parent POM..." -ForegroundColor Yellow
if (Test-Path "pom.xml") {
    Write-Host "✓ Parent pom.xml found" -ForegroundColor Green
} else {
    Write-Host "✗ Parent pom.xml not found" -ForegroundColor Red
    $allGood = $false
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Verification Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($allGood) {
    Write-Host ""
    Write-Host "✅ All checks passed! You're ready to run ByteBazar." -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Run: .\start-services.ps1" -ForegroundColor White
    Write-Host "2. Or follow instructions in QUICK_START.md" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "❌ Some checks failed. Please fix the issues above." -ForegroundColor Red
    Write-Host ""
    Write-Host "For help, see QUICK_START.md or README.md" -ForegroundColor Yellow
}

Write-Host ""
