#!/bin/bash

# Build and load Docker images to Kind cluster
# Usage: ./build-and-load-images.sh [service-name] [--all]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PROJECT_ROOT="../../"
KIND_CLUSTER_NAME="mybank-cluster"

# All services
SERVICES=(
    "api-gateway"
    "auth-service"
    "user-service"
    "pfm-core-service"
    "payment-service"
    "investment-service"
)

# Function to build and load a service
build_and_load() {
    local service=$1
    local tag=${2:-latest}

    echo -e "${YELLOW}Building ${service}...${NC}"

    # Build with Gradle
    if [ -d "${PROJECT_ROOT}/${service}" ]; then
        cd ${PROJECT_ROOT}
        ./gradlew :${service}:build -x test
        cd - > /dev/null
    else
        echo -e "${RED}Service directory not found: ${service}${NC}"
        return 1
    fi

    # Build Docker image
    echo -e "${YELLOW}Building Docker image for ${service}...${NC}"
    docker build -t mybank/${service}:${tag} ${PROJECT_ROOT}/${service}

    # Load into Kind
    echo -e "${YELLOW}Loading ${service} into Kind cluster...${NC}"
    kind load docker-image mybank/${service}:${tag} --name ${KIND_CLUSTER_NAME}

    echo -e "${GREEN}✓ ${service} built and loaded successfully${NC}"
    echo ""
}

# Main script
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Build and Load Docker Images${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# Check if Kind cluster exists
if ! kind get clusters | grep -q ${KIND_CLUSTER_NAME}; then
    echo -e "${RED}Error: Kind cluster '${KIND_CLUSTER_NAME}' not found${NC}"
    echo -e "Create it with: kind create cluster --name ${KIND_CLUSTER_NAME}"
    exit 1
fi

# Parse arguments
if [ "$1" = "--all" ] || [ "$1" = "-a" ]; then
    # Build all services
    for service in "${SERVICES[@]}"; do
        build_and_load "$service" "latest"
    done
elif [ -n "$1" ]; then
    # Build specific service
    SERVICE_NAME=$1
    TAG=${2:-latest}

    if [[ " ${SERVICES[@]} " =~ " ${SERVICE_NAME} " ]]; then
        build_and_load "$SERVICE_NAME" "$TAG"
    else
        echo -e "${RED}Error: Unknown service '${SERVICE_NAME}'${NC}"
        echo -e "Available services: ${SERVICES[*]}"
        exit 1
    fi
else
    # Show usage
    echo "Usage: $0 [service-name] [tag] | --all"
    echo ""
    echo "Examples:"
    echo "  $0 --all                    # Build all services"
    echo "  $0 api-gateway              # Build api-gateway with 'latest' tag"
    echo "  $0 auth-service v1.0.0      # Build auth-service with 'v1.0.0' tag"
    echo ""
    echo "Available services:"
    for service in "${SERVICES[@]}"; do
        echo "  - $service"
    done
    exit 0
fi

echo -e "${GREEN}All images built and loaded successfully!${NC}"
echo ""
echo "Next steps:"
echo "  1. Deploy with Helm: ${YELLOW}./deploy-helm.sh development${NC}"
echo "  2. Deploy with Kustomize: ${YELLOW}./deploy-kustomize.sh development${NC}"
