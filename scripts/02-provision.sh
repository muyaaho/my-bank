#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

log_header "Phase 2: Provision Infrastructure"

# Create cluster if doesn't exist
if cluster_exists; then
    log_warning "Cluster already exists, skipping creation"
else
    log_step "Creating Kind cluster..."
    kind create cluster --config kind-config.yaml --name "$CLUSTER_NAME"
    kubectl wait --for=condition=Ready nodes --all --timeout=120s
    log_success "Cluster created"
fi

# Install Istio
log_step "Installing Istio..."
if ! command_exists istioctl; then
    curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.27.3 sh -
    ./istio-1.27.3/bin/istioctl install --set profile=default -y
else
    istioctl install --set profile=default -y
fi
kubectl wait --namespace istio-system --for=condition=ready pod --selector=app=istiod --timeout=300s
log_success "Istio installed"

# Create namespace
log_step "Creating namespace..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
kubectl label namespace "$NAMESPACE" istio-injection=enabled --overwrite
log_success "Namespace ready"

# Build images
log_step "Building images..."
./gradlew clean build -x test --no-daemon --parallel
COMPOSE_PROFILES=build docker-compose build
log_success "Images built"

# Load images to Kind
log_step "Loading images to Kind..."
for img in api-gateway auth-service user-service analytics-service asset-service payment-service investment-service frontend; do
    kind load docker-image mybank/$img:latest --name "$CLUSTER_NAME"
done
log_success "Images loaded"

log_success "Provision complete! 🚀"
