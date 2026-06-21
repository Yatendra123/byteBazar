# ByteBazar - Microservices E-Commerce Platform

ByteBazar is a production-ready, cloud-native e-commerce platform built with Spring Boot microservices architecture. It provides comprehensive functionality for managing customers, products, orders, and payments with full OAuth2 security, event-driven communication, and observability.

## 🏗️ Architecture Overview

ByteBazar consists of 6 main microservices:

- **Gateway Service** (Port 8080) - API Gateway with OAuth2 resource server and CORS
- **Auth Service** (Port 8082) - OAuth2 Authorization Server with JWT tokens  
- **Customer Service** (Port 8085) - Customer management with PostgreSQL + Flyway
- **Catalog Service** (Port 8081) - Product catalog with Redis caching
- **Order Service** (Port 8083) - Order orchestration with Outbox pattern + SQS
- **Payment Service** (Port 8084) - Payment processing with Resilience4j circuit breaker

## 🚀 Technology Stack

- **Framework**: Spring Boot 3.5.5, Spring Cloud 2025.0.0
- **Language**: Java 17
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis
- **Messaging**: AWS SQS (via LocalStack)
- **Security**: OAuth2/JWT with Spring Security
- **Gateway**: Spring Cloud Gateway
- **Resilience**: Resilience4j Circuit Breaker
- **Observability**: Micrometer, Actuator endpoints
- **Documentation**: SpringDoc OpenAPI
- **Testing**: Testcontainers for integration tests
- **Build**: Maven multi-module project

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+ 
- Docker and Docker Compose
- 8GB+ RAM recommended

## 🛠️ Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd byteBazar
```

### 2. Start Infrastructure Services
```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379) 
- LocalStack with SQS (port 4566)

### 3. Build All Services
```bash
mvn clean compile -DskipTests
```

### 4. Start Services in Order

**Step 1: Start Auth Service**
```bash
cd auth-service
mvn spring-boot:run
```
Wait for startup (should show "Started AuthServiceApplication")

**Step 2: Start Gateway Service**
```bash
cd gateway-service
mvn spring-boot:run
```

**Step 3: Start Business Services (in parallel)**
```bash
# Terminal 1
cd customer-service
mvn spring-boot:run

# Terminal 2  
cd byteBazar
mvn spring-boot:run

# Terminal 3
cd order-service
mvn spring-boot:run

# Terminal 4
cd payment-service
mvn spring-boot:run
```

## 🔐 Security & Authentication

### OAuth2 Configuration

The platform uses OAuth2 with JWT tokens. Default clients and users:

**OAuth2 Clients:**
- `catalog-client` / `catalog-secret` (Client Credentials)
- `ui-client` / `ui-secret` (Authorization Code + Refresh Token)

**Default Users:**
- `admin` / `admin123` (ADMIN role)
- `seller` / `seller123` (SELLER role)  
- `buyer` / `buyer123` (BUYER role)

### Getting Access Token

**Client Credentials Flow:**
```bash
curl -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "catalog-client:catalog-secret" \
  -d "grant_type=client_credentials&scope=catalog.write"
```

**Authorization Code Flow:**
Navigate to: `http://localhost:8082/oauth2/authorize?response_type=code&client_id=ui-client&redirect_uri=http://localhost:8080/login/oauth2/code/ui-client&scope=openid%20profile`

## 📚 API Documentation

Once services are running, access Swagger UI:

- **Gateway**: http://localhost:8080/swagger-ui.html
- **Auth Service**: http://localhost:8082/swagger-ui.html
- **Customer Service**: http://localhost:8085/swagger-ui.html
- **Catalog Service**: http://localhost:8081/swagger-ui.html
- **Order Service**: http://localhost:8083/swagger-ui.html
- **Payment Service**: http://localhost:8084/swagger-ui.html

## 🔄 API Usage Examples

### 1. Customer Management
```bash
# Create customer
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+1234567890"
  }'

# Get customer
curl http://localhost:8080/api/v1/customers/{id} \
  -H "Authorization: Bearer <token>"
```

### 2. Product Catalog
```bash
# Create product
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "Laptop",
    "description": "High-performance laptop",
    "price": 999.99,
    "category": "Electronics",
    "sellerId": "seller-uuid"
  }'

# Search products
curl "http://localhost:8080/api/v1/products?query=laptop&page=0&size=10"
```

### 3. Order Management
```bash
# Create order
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "customerId": "customer-uuid",
    "items": [
      {
        "productId": "product-uuid",
        "quantity": 1,
        "price": 999.99
      }
    ]
  }'
```

### 4. Payment Processing
```bash
# Process payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "orderId": "order-uuid",
    "amount": 999.99,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD"
  }'
```

## 📊 Monitoring & Health Checks

### Health Endpoints
- **Gateway**: http://localhost:8080/actuator/health
- **Auth**: http://localhost:8082/actuator/health
- **Customer**: http://localhost:8085/actuator/health
- **Catalog**: http://localhost:8081/actuator/health
- **Order**: http://localhost:8083/actuator/health
- **Payment**: http://localhost:8084/actuator/health

### Metrics
All services expose Prometheus metrics at `/actuator/metrics` and `/actuator/prometheus`

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

Integration tests use Testcontainers for database and messaging tests.

## 🐳 Docker Deployment

### Build Docker Images
```bash
mvn spring-boot:build-image
```

### Production Docker Compose
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 🔧 Configuration

### Environment Variables

**Database Configuration:**
- `POSTGRES_DB=bytebazar`
- `POSTGRES_USER=admin`
- `POSTGRES_PASSWORD=admin123`

**Redis Configuration:**
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`

**AWS Configuration (LocalStack):**
- `AWS_REGION=us-east-1`
- `AWS_ACCESS_KEY_ID=test`
- `AWS_SECRET_ACCESS_KEY=test`
- `AWS_ENDPOINT_URL=http://localhost:4566`

## 🚨 Troubleshooting

### Common Issues

1. **Port Conflicts**: Ensure ports 8080-8085, 5432, 6379, 4566 are available
2. **Database Connection**: Verify PostgreSQL is running via `docker-compose ps`
3. **OAuth2 Issues**: Check Auth service is started first and accessible
4. **Memory Issues**: Increase Docker memory allocation to 8GB+

### Logs
```bash
# View service logs
docker-compose logs -f postgres
docker-compose logs -f redis
docker-compose logs -f localstack
```

## 🏗️ Development

### Project Structure
```
byteBazar/
├── auth-service/          # OAuth2 Authorization Server
├── gateway-service/       # API Gateway
├── customer-service/      # Customer Management
├── byteBazar/            # Catalog Service  
├── order-service/        # Order Management
├── payment-service/      # Payment Processing
├── localstack/           # LocalStack initialization
├── docker-compose.yml    # Infrastructure setup
└── pom.xml              # Parent Maven POM
```

### Adding New Features

1. Create DTOs in respective service packages
2. Implement JPA entities and repositories
3. Add business logic in service classes
4. Create REST controllers with validation
5. Add integration tests
6. Update API documentation
