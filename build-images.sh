#!/bin/bash

# Build Docker images for all microservices

set -e

echo "🔨 Building MyBank Docker Images..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Build Gradle projects
echo -e "${YELLOW}📦 Building Gradle projects...${NC}"
./gradlew clean build -x test

# Build Docker images
echo ""
echo -e "${YELLOW}🐳 Building Docker images...${NC}"
echo ""

# Infrastructure services
echo "Building api-gateway..."
docker build -t mybank/api-gateway:latest \
  -f api-gateway/Dockerfile \
  api-gateway/

# Business services
echo "Building auth-service..."
docker build -t mybank/auth-service:latest \
  -f auth-service/Dockerfile \
  auth-service/

echo "Building user-service..."
docker build -t mybank/user-service:latest \
  -f user-service/Dockerfile \
  .

echo "Building pfm-core-service..."
docker build -t mybank/pfm-core-service:latest \
  -f pfm-core-service/Dockerfile \
  pfm-core-service/

echo "Building payment-service..."
docker build -t mybank/payment-service:latest \
  -f payment-service/Dockerfile \
  payment-service/

echo "Building investment-service..."
docker build -t mybank/investment-service:latest \
  -f investment-service/Dockerfile \
  investment-service/

# Frontend
echo "Building frontend..."
docker build -t mybank/frontend:latest \
  -f app/Dockerfile \
  app/

echo ""
echo -e "${GREEN}✅ All images built successfully!${NC}"
echo ""
echo "Built images:"
docker images | grep mybank
