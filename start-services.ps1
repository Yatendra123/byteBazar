# ByteBazar Startup Script for Windows
# This script starts all microservices in the correct order

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ByteBazar Microservices Startup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "[1/6] Checking Docker..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "✓ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Start infrastructure services
Write-Host ""
Write-Host "[2/6] Starting infrastructure (PostgreSQL, Redis, LocalStack)..." -ForegroundColor Yellow
docker-compose up -d

# Wait for services to be healthy
Write-Host ""
Write-Host "[3/6] Waiting for infrastructure to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check database initialization
Write-Host ""
Write-Host "[4/6] Verifying database setup..." -ForegroundColor Yellow
docker exec bb-postgres psql -U postgres -c "\l" | Select-String "bytebazar"
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Databases initialized successfully" -ForegroundColor Green
} else {
    Write-Host "! Database initialization may need verification" -ForegroundColor Yellow
}

# Build all services
Write-Host ""
Write-Host "[5/6] Building all services..." -ForegroundColor Yellow
mvn clean compile -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed. Please check the errors above." -ForegroundColor Red
    exit 1
}
Write-Host "✓ All services built successfully" -ForegroundColor Green

# Instructions for starting services
Write-Host ""
Write-Host "[6/6] Ready to start services!" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Start services in this order:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Auth Service (FIRST - Required):" -ForegroundColor Yellow
Write-Host "   cd auth-service" -ForegroundColor White
Write-Host "   mvn spring-boot:run" -ForegroundColor White
Write-Host "   Wait for: 'Started AuthApplication'" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Gateway Service (SECOND):" -ForegroundColor Yellow
Write-Host "   cd gateway-service" -ForegroundColor White
Write-Host "   mvn spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "3. Business Services (Can start in parallel):" -ForegroundColor Yellow
Write-Host "   Terminal 1: cd customer-service && mvn spring-boot:run" -ForegroundColor White
Write-Host "   Terminal 2: cd catalog-service && mvn spring-boot:run" -ForegroundColor White
Write-Host "   Terminal 3: cd order-service && mvn spring-boot:run" -ForegroundColor White
Write-Host "   Terminal 4: cd payment-service && mvn spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Service Endpoints:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Gateway:  http://localhost:8080" -ForegroundColor White
Write-Host "Auth:     http://localhost:8082" -ForegroundColor White
Write-Host "Catalog:  http://localhost:8081" -ForegroundColor White
Write-Host "Order:    http://localhost:8083" -ForegroundColor White
Write-Host "Payment:  http://localhost:8084" -ForegroundColor White
Write-Host "Customer: http://localhost:8085" -ForegroundColor White
Write-Host ""
Write-Host "Health Check: http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "API Docs:     http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  OAuth2 Test Credentials:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Client: catalog-client / catalog-secret" -ForegroundColor White
Write-Host "Users:  admin/admin123, seller/seller123, buyer/buyer123" -ForegroundColor White
Write-Host ""
