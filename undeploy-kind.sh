#!/bin/bash

# Undeploy MyBank from Kind cluster

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "🛑 Undeploying MyBank from Kind..."
echo ""

# Parse arguments
DELETE_CLUSTER=false
if [[ "$1" == "--delete-cluster" ]]; then
    DELETE_CLUSTER=true
fi

# Delete Kubernetes resources
echo -e "${YELLOW}🗑️  Deleting Kubernetes resources...${NC}"
kubectl delete -f k8s/services/ --ignore-not-found=true
kubectl delete -f k8s/infrastructure/ --ignore-not-found=true
kubectl delete -f k8s/namespace.yaml --ignore-not-found=true

echo -e "${GREEN}✅ Resources deleted${NC}"

# Delete Kind cluster if requested
if [ "$DELETE_CLUSTER" = true ]; then
    echo ""
    echo -e "${YELLOW}🗑️  Deleting Kind cluster...${NC}"
    if kind get clusters | grep -q "mybank-cluster"; then
        kind delete cluster --name mybank-cluster
        echo -e "${GREEN}✅ Kind cluster deleted${NC}"
    else
        echo "Kind cluster 'mybank-cluster' not found."
    fi
fi

echo ""
echo -e "${GREEN}✅ Cleanup completed!${NC}"
echo ""
if [ "$DELETE_CLUSTER" = false ]; then
    echo "To delete the Kind cluster as well, run:"
    echo "  ./undeploy-kind.sh --delete-cluster"
fi
