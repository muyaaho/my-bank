#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

log_header "Phase 3: Deploy Applications"

# Deploy infrastructure
log_step "Deploying infrastructure..."
helm upgrade --install mybank-infrastructure helm/infrastructure \
    --namespace "$NAMESPACE" --create-namespace --wait --timeout=10m
log_success "Infrastructure deployed"

# Wait for infrastructure to be ready
log_step "Waiting for infrastructure pods..."
wait_for_pods "$NAMESPACE" "tier=infrastructure" 300 || log_warning "Some infrastructure pods may not be ready"

# Deploy services
log_step "Deploying services..."
helm upgrade --install mybank-services helm/services \
    --namespace "$NAMESPACE" --wait --timeout=10m
log_success "Services deployed"

# Deploy frontend
log_step "Deploying frontend..."
helm upgrade --install mybank-frontend helm/frontend \
    --namespace "$NAMESPACE" --wait --timeout=10m
log_success "Frontend deployed"

log_success "Deploy complete! 🎉"
