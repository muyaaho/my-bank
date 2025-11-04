#!/bin/bash

# Complete deployment: Build + Deploy to Kind

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🚀 Complete MyBank Deployment to Kind"
echo "=========================================="
echo ""

# Step 1: Build images
echo -e "${YELLOW}Step 1: Building Docker images...${NC}"
./build-images.sh

echo ""
echo ""

# Step 2: Deploy to Kind
echo -e "${YELLOW}Step 2: Deploying to Kind cluster...${NC}"
./deploy-kind.sh

echo ""
echo -e "${GREEN}🎉 Deployment completed successfully!${NC}"
echo ""
echo "Next steps:"
echo "1. Wait for all pods to be ready: kubectl get pods -n mybank -w"
echo "2. Test the API: curl http://localhost:8080/actuator/health"
echo "3. Access Frontend: http://localhost:30000"
echo "4. View Eureka Dashboard: http://localhost:8761"
echo "5. Monitor Kafka: http://localhost:8090"
