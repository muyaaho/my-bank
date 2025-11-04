#!/bin/bash

# MyBank - Service Startup Script

echo "🚀 Starting MyBank Services..."
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "📦 Step 1: Starting infrastructure services with Docker Compose..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 10

echo ""
echo -e "${GREEN}✅ Infrastructure services started:${NC}"
echo "  - PostgreSQL: localhost:5432"
echo "  - MongoDB: localhost:27017"
echo "  - Redis: localhost:6379"
echo "  - Kafka: localhost:9092"
echo "  - Kafka UI: http://localhost:8090"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"

echo ""
echo -e "${YELLOW}📋 Step 2: Build the project (this may take a few minutes)...${NC}"
./gradlew clean build -x test

echo ""
echo -e "${GREEN}✅ Build completed${NC}"

echo ""
echo "🎯 Step 3: Start Spring Boot microservices"
echo "Please open separate terminal windows and run these commands:"
echo ""
echo -e "${YELLOW}Terminal 1 - Service Discovery:${NC}"
echo "  ./gradlew :service-discovery:bootRun"
echo ""
echo -e "${YELLOW}Terminal 2 - Config Server (wait 30s after Terminal 1):${NC}"
echo "  ./gradlew :config-server:bootRun"
echo ""
echo -e "${YELLOW}Terminal 3 - API Gateway (wait 30s after Terminal 2):${NC}"
echo "  ./gradlew :api-gateway:bootRun"
echo ""
echo -e "${YELLOW}Terminal 4 - Auth Service:${NC}"
echo "  ./gradlew :auth-service:bootRun"
echo ""
echo -e "${YELLOW}Terminal 5 - PFM Service:${NC}"
echo "  ./gradlew :pfm-core-service:bootRun"
echo ""
echo -e "${YELLOW}Terminal 6 - Payment Service:${NC}"
echo "  ./gradlew :payment-service:bootRun"
echo ""
echo -e "${YELLOW}Terminal 7 - Investment Service:${NC}"
echo "  ./gradlew :investment-service:bootRun"
echo ""
echo "📊 Useful URLs:"
echo "  - Eureka Dashboard: http://localhost:8761"
echo "  - API Gateway: http://localhost:8080"
echo "  - Kafka UI: http://localhost:8090"
echo "  - Grafana: http://localhost:3000"
echo ""
echo "✨ For API examples, see README.md"
