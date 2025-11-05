#!/bin/bash

# MyBank Helm Deployment Script
# Usage: ./deploy-helm.sh [environment] [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT=${1:-development}
NAMESPACE="mybank"
CHART_PATH="../helm/mybank"
RELEASE_NAME="mybank"
DRY_RUN=false
UPGRADE=false
INSTALL_ISTIO=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --upgrade)
            UPGRADE=true
            shift
            ;;
        --install-istio)
            INSTALL_ISTIO=true
            shift
            ;;
        --namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        --release-name)
            RELEASE_NAME="$2"
            shift 2
            ;;
        *)
            ENVIRONMENT="$1"
            shift
            ;;
    esac
done

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}MyBank Helm Deployment${NC}"
echo -e "${GREEN}================================${NC}"
echo -e "Environment: ${YELLOW}${ENVIRONMENT}${NC}"
echo -e "Namespace: ${YELLOW}${NAMESPACE}${NC}"
echo -e "Release: ${YELLOW}${RELEASE_NAME}${NC}"
echo ""

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    exit 1
fi

# Check if helm is available
if ! command -v helm &> /dev/null; then
    echo -e "${RED}Error: helm is not installed${NC}"
    exit 1
fi

# Check if kind cluster exists
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
    exit 1
fi

# Install Istio if requested
if [ "$INSTALL_ISTIO" = true ]; then
    echo -e "${YELLOW}Installing Istio...${NC}"

    if ! command -v istioctl &> /dev/null; then
        echo -e "${YELLOW}Installing istioctl...${NC}"
        curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.20.0 sh -
        export PATH=$PWD/istio-1.20.0/bin:$PATH
    fi

    istioctl install --set profile=demo -y
    kubectl label namespace ${NAMESPACE} istio-injection=enabled --overwrite
    echo -e "${GREEN}Istio installed successfully${NC}"
fi

# Create namespace if it doesn't exist
if ! kubectl get namespace ${NAMESPACE} &> /dev/null; then
    echo -e "${YELLOW}Creating namespace ${NAMESPACE}...${NC}"
    kubectl create namespace ${NAMESPACE}
    kubectl label namespace ${NAMESPACE} istio-injection=enabled
    echo -e "${GREEN}Namespace created${NC}"
fi

# Set values file based on environment
VALUES_FILE=""
case ${ENVIRONMENT} in
    development|dev)
        VALUES_FILE="${CHART_PATH}/values-development.yaml"
        if [ ! -f "$VALUES_FILE" ]; then
            VALUES_FILE="${CHART_PATH}/values.yaml"
        fi
        ;;
    staging)
        VALUES_FILE="${CHART_PATH}/values-staging.yaml"
        if [ ! -f "$VALUES_FILE" ]; then
            VALUES_FILE="${CHART_PATH}/values.yaml"
        fi
        ;;
    production|prod)
        VALUES_FILE="${CHART_PATH}/values-production.yaml"
        if [ ! -f "$VALUES_FILE" ]; then
            VALUES_FILE="${CHART_PATH}/values.yaml"
        fi
        ;;
    *)
        VALUES_FILE="${CHART_PATH}/values.yaml"
        ;;
esac

echo -e "${YELLOW}Using values file: ${VALUES_FILE}${NC}"

# Helm command options
HELM_OPTS="--namespace ${NAMESPACE}"
if [ -f "$VALUES_FILE" ]; then
    HELM_OPTS="${HELM_OPTS} --values ${VALUES_FILE}"
fi

# Check if release exists
RELEASE_EXISTS=$(helm list -n ${NAMESPACE} | grep ${RELEASE_NAME} || true)

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}Running dry-run...${NC}"
    helm install ${RELEASE_NAME} ${CHART_PATH} ${HELM_OPTS} --dry-run --debug
    exit 0
fi

# Deploy or upgrade
if [ -n "$RELEASE_EXISTS" ] || [ "$UPGRADE" = true ]; then
    echo -e "${YELLOW}Upgrading existing release...${NC}"
    helm upgrade ${RELEASE_NAME} ${CHART_PATH} ${HELM_OPTS} \
        --install \
        --create-namespace \
        --wait \
        --timeout 10m

    echo -e "${GREEN}Upgrade completed successfully${NC}"
else
    echo -e "${YELLOW}Installing new release...${NC}"
    helm install ${RELEASE_NAME} ${CHART_PATH} ${HELM_OPTS} \
        --create-namespace \
        --wait \
        --timeout 10m

    echo -e "${GREEN}Installation completed successfully${NC}"
fi

# Display deployment status
echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Deployment Status${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

echo -e "${YELLOW}Helm Release:${NC}"
helm list -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Pods:${NC}"
kubectl get pods -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Services:${NC}"
kubectl get svc -n ${NAMESPACE}
echo ""

# Get access URLs
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Access Information${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

if kubectl get svc api-gateway -n ${NAMESPACE} &> /dev/null; then
    echo -e "API Gateway: ${YELLOW}http://localhost:8080${NC}"
    echo -e "  Port forward: ${YELLOW}kubectl port-forward -n ${NAMESPACE} svc/api-gateway 8080:8080${NC}"
fi

if kubectl get svc frontend -n ${NAMESPACE} &> /dev/null; then
    echo -e "Frontend: ${YELLOW}http://localhost:3000${NC}"
    echo -e "  Port forward: ${YELLOW}kubectl port-forward -n ${NAMESPACE} svc/frontend 3000:3000${NC}"
fi

echo ""
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo ""
echo -e "Next steps:"
echo -e "  1. Port forward to access services"
echo -e "  2. Check logs: ${YELLOW}kubectl logs -n ${NAMESPACE} <pod-name>${NC}"
echo -e "  3. Monitor: ${YELLOW}kubectl get pods -n ${NAMESPACE} -w${NC}"
