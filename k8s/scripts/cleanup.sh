#!/bin/bash

# Cleanup MyBank Kubernetes resources
# Usage: ./cleanup.sh [--delete-cluster] [--keep-namespace]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

NAMESPACE="mybank"
KIND_CLUSTER_NAME="mybank-cluster"
DELETE_CLUSTER=false
KEEP_NAMESPACE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --delete-cluster)
            DELETE_CLUSTER=true
            shift
            ;;
        --keep-namespace)
            KEEP_NAMESPACE=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}MyBank Cleanup${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# Delete Helm releases
echo -e "${YELLOW}Uninstalling Helm releases...${NC}"
helm list -n ${NAMESPACE} --short | xargs -r helm uninstall -n ${NAMESPACE} || true
echo -e "${GREEN}✓ Helm releases uninstalled${NC}"

# Delete Kustomize resources
echo -e "${YELLOW}Deleting Kustomize resources...${NC}"
kubectl delete -k ../kustomize/overlays/development --ignore-not-found=true 2>/dev/null || true
kubectl delete -k ../kustomize/overlays/staging --ignore-not-found=true 2>/dev/null || true
kubectl delete -k ../kustomize/overlays/production --ignore-not-found=true 2>/dev/null || true
echo -e "${GREEN}✓ Kustomize resources deleted${NC}"

# Delete namespace
if [ "$KEEP_NAMESPACE" = false ]; then
    echo -e "${YELLOW}Deleting namespace ${NAMESPACE}...${NC}"
    kubectl delete namespace ${NAMESPACE} --ignore-not-found=true --timeout=60s || true
    echo -e "${GREEN}✓ Namespace deleted${NC}"
else
    echo -e "${YELLOW}Keeping namespace ${NAMESPACE}${NC}"
fi

# Delete ArgoCD applications
if kubectl get namespace argocd &> /dev/null; then
    echo -e "${YELLOW}Deleting ArgoCD applications...${NC}"
    kubectl delete -f ../argocd/applications/ --ignore-not-found=true 2>/dev/null || true
    echo -e "${GREEN}✓ ArgoCD applications deleted${NC}"
fi

# Delete Kind cluster
if [ "$DELETE_CLUSTER" = true ]; then
    echo -e "${YELLOW}Deleting Kind cluster ${KIND_CLUSTER_NAME}...${NC}"
    kind delete cluster --name ${KIND_CLUSTER_NAME} || true
    echo -e "${GREEN}✓ Kind cluster deleted${NC}"
fi

# Clean Docker volumes
echo -e "${YELLOW}Pruning Docker volumes...${NC}"
docker volume prune -f > /dev/null 2>&1 || true
echo -e "${GREEN}✓ Docker volumes pruned${NC}"

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Cleanup Complete${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

if [ "$DELETE_CLUSTER" = true ]; then
    echo -e "Kind cluster deleted. To recreate:"
    echo -e "  ${YELLOW}kind create cluster --name ${KIND_CLUSTER_NAME}${NC}"
else
    echo -e "Kind cluster preserved. To delete:"
    echo -e "  ${YELLOW}./cleanup.sh --delete-cluster${NC}"
fi

echo ""
