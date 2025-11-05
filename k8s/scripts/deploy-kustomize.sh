#!/bin/bash

# MyBank Kustomize Deployment Script
# Usage: ./deploy-kustomize.sh [environment] [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT=${1:-development}
NAMESPACE="mybank"
KUSTOMIZE_PATH="../kustomize"
DRY_RUN=false
DELETE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --delete)
            DELETE=true
            shift
            ;;
        --namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        *)
            ENVIRONMENT="$1"
            shift
            ;;
    esac
done

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}MyBank Kustomize Deployment${NC}"
echo -e "${GREEN}================================${NC}"
echo -e "Environment: ${YELLOW}${ENVIRONMENT}${NC}"
echo -e "Namespace: ${YELLOW}${NAMESPACE}${NC}"
echo ""

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    exit 1
fi

# Check if kind cluster exists
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
    exit 1
fi

# Determine overlay path
OVERLAY_PATH=""
case ${ENVIRONMENT} in
    development|dev)
        OVERLAY_PATH="${KUSTOMIZE_PATH}/overlays/development"
        ;;
    staging)
        OVERLAY_PATH="${KUSTOMIZE_PATH}/overlays/staging"
        ;;
    production|prod)
        OVERLAY_PATH="${KUSTOMIZE_PATH}/overlays/production"
        ;;
    *)
        echo -e "${RED}Error: Unknown environment '${ENVIRONMENT}'${NC}"
        echo -e "Valid environments: development, staging, production"
        exit 1
        ;;
esac

if [ ! -d "$OVERLAY_PATH" ]; then
    echo -e "${RED}Error: Overlay path not found: ${OVERLAY_PATH}${NC}"
    exit 1
fi

echo -e "${YELLOW}Using overlay: ${OVERLAY_PATH}${NC}"

# Delete resources if requested
if [ "$DELETE" = true ]; then
    echo -e "${YELLOW}Deleting resources...${NC}"
    kubectl delete -k ${OVERLAY_PATH} --ignore-not-found=true
    echo -e "${GREEN}Resources deleted${NC}"
    exit 0
fi

# Dry run
if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}Running dry-run...${NC}"
    kubectl apply -k ${OVERLAY_PATH} --dry-run=client
    echo ""
    echo -e "${YELLOW}Generated manifests:${NC}"
    kubectl kustomize ${OVERLAY_PATH}
    exit 0
fi

# Apply kustomization
echo -e "${YELLOW}Applying Kustomize overlay...${NC}"
kubectl apply -k ${OVERLAY_PATH}

echo -e "${GREEN}Deployment completed successfully${NC}"

# Wait for pods to be ready
echo ""
echo -e "${YELLOW}Waiting for pods to be ready...${NC}"
kubectl wait --for=condition=ready pod \
    --selector=app.kubernetes.io/part-of=mybank \
    --namespace=${NAMESPACE} \
    --timeout=300s || true

# Display deployment status
echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Deployment Status${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

echo -e "${YELLOW}Pods:${NC}"
kubectl get pods -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Services:${NC}"
kubectl get svc -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Deployments:${NC}"
kubectl get deployments -n ${NAMESPACE}
echo ""

# Get access URLs
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Access Information${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

echo -e "API Gateway: ${YELLOW}http://localhost:8080${NC}"
echo -e "  Port forward: ${YELLOW}kubectl port-forward -n ${NAMESPACE} svc/api-gateway 8080:8080${NC}"
echo ""

echo -e "Frontend: ${YELLOW}http://localhost:3000${NC}"
echo -e "  Port forward: ${YELLOW}kubectl port-forward -n ${NAMESPACE} svc/frontend 3000:3000${NC}"
echo ""

echo -e "${GREEN}Deployment completed successfully!${NC}"
