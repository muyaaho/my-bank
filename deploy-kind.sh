#!/bin/bash

# Deploy MyBank to Kind cluster

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "🚀 Deploying MyBank to Kind..."
echo ""

# Check if kind is installed
if ! command -v kind &> /dev/null; then
    echo -e "${RED}❌ kind is not installed. Please install it first:${NC}"
    echo "  brew install kind"
    echo "  or visit: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    exit 1
fi

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}❌ kubectl is not installed. Please install it first:${NC}"
    echo "  brew install kubectl"
    exit 1
fi

# Create Kind cluster
echo -e "${YELLOW}📦 Step 1: Creating Kind cluster...${NC}"
if kind get clusters | grep -q "mybank-cluster"; then
    echo "Kind cluster 'mybank-cluster' already exists. Skipping creation."
else
    kind create cluster --config kind-config.yaml
    echo -e "${GREEN}✅ Kind cluster created${NC}"
fi

echo ""
echo -e "${YELLOW}🐳 Step 2: Loading Docker images to Kind...${NC}"
kind load docker-image mybank/service-discovery:latest --name mybank-cluster
kind load docker-image mybank/config-server:latest --name mybank-cluster
kind load docker-image mybank/api-gateway:latest --name mybank-cluster
kind load docker-image mybank/auth-service:latest --name mybank-cluster
kind load docker-image mybank/pfm-core-service:latest --name mybank-cluster
kind load docker-image mybank/payment-service:latest --name mybank-cluster
kind load docker-image mybank/investment-service:latest --name mybank-cluster
kind load docker-image mybank/frontend:latest --name mybank-cluster
echo -e "${GREEN}✅ Images loaded to Kind${NC}"

echo ""
echo -e "${YELLOW}☸️  Step 3: Deploying Kubernetes resources...${NC}"

# Create namespace
echo "Creating namespace..."
kubectl apply -f k8s/namespace.yaml

# Deploy infrastructure
echo "Deploying infrastructure services..."
kubectl apply -f k8s/infrastructure/postgres.yaml
kubectl apply -f k8s/infrastructure/mongodb.yaml
kubectl apply -f k8s/infrastructure/redis.yaml
kubectl apply -f k8s/infrastructure/kafka.yaml

echo "Waiting for infrastructure services to be ready..."
sleep 10
kubectl wait --for=condition=ready pod -l app=postgres -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=mongodb -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=redis -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=zookeeper -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=kafka -n mybank --timeout=180s

echo ""
echo "Deploying Spring Boot microservices..."
# Deploy in order: Discovery -> Config -> Gateway -> Services
kubectl apply -f k8s/services/service-discovery.yaml
echo "Waiting for service-discovery to be ready..."
sleep 15
kubectl wait --for=condition=ready pod -l app=service-discovery -n mybank --timeout=180s

kubectl apply -f k8s/services/config-server.yaml
echo "Waiting for config-server to be ready..."
sleep 15
kubectl wait --for=condition=ready pod -l app=config-server -n mybank --timeout=180s

kubectl apply -f k8s/services/api-gateway.yaml
kubectl apply -f k8s/services/auth-service.yaml
kubectl apply -f k8s/services/pfm-core-service.yaml
kubectl apply -f k8s/services/payment-service.yaml
kubectl apply -f k8s/services/investment-service.yaml

echo ""
echo "Deploying frontend..."
kubectl apply -f k8s/frontend-deployment.yaml

echo ""
echo -e "${GREEN}✅ Deployment completed!${NC}"
echo ""
echo "📊 Checking deployment status..."
kubectl get pods -n mybank
echo ""
echo "🌐 Access URLs:"
echo "  - Frontend:         http://localhost:30000"
echo "  - API Gateway:      http://localhost:8080"
echo "  - Eureka Dashboard: http://localhost:8761"
echo "  - Kafka UI:         http://localhost:8090"
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n mybank"
echo "  kubectl logs -f <pod-name> -n mybank"
echo "  kubectl describe pod <pod-name> -n mybank"
echo "  kubectl port-forward -n mybank svc/api-gateway 8080:8080"
