#!/bin/bash

# Quick deployment script - assumes images are already built

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}🚀 Quick deploy to Kind...${NC}"

# Load pre-built images
echo -e "${YELLOW}Loading images to Kind...${NC}"
kind load docker-image mybank/user-service:latest --name mybank-cluster
kind load docker-image mybank/auth-service:latest --name mybank-cluster
kind load docker-image mybank/api-gateway:latest --name mybank-cluster
kind load docker-image mybank/pfm-core-service:latest --name mybank-cluster
kind load docker-image mybank/payment-service:latest --name mybank-cluster
kind load docker-image mybank/investment-service:latest --name mybank-cluster

echo -e "${GREEN}✅ Images loaded${NC}"

# Apply all k8s manifests
echo -e "${YELLOW}Deploying infrastructure...${NC}"
kubectl apply -f k8s/infrastructure/

echo -e "${YELLOW}Deploying services...${NC}"
kubectl apply -f k8s/services/

echo -e "${YELLOW}Deploying Istio configuration...${NC}"
kubectl apply -f k8s/istio/

echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""
echo -e "${YELLOW}Check status:${NC}"
echo "  kubectl get pods -n mybank"
echo ""
echo -e "${YELLOW}Port forward:${NC}"
echo "  kubectl port-forward -n istio-system svc/istio-ingressgateway 443:443 80:80"
echo ""
echo -e "${YELLOW}Access:${NC}"
echo "  https://app.mybank.local"
echo "  https://api.mybank.local"
