#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

log_header "Phase 1: Cleanup"

# Cleanup Helm releases
log_step "Cleaning up Helm releases..."
if namespace_exists "$NAMESPACE"; then
    helm uninstall mybank-frontend -n "$NAMESPACE" 2>/dev/null || log_warning "Frontend release not found"
    helm uninstall mybank-services -n "$NAMESPACE" 2>/dev/null || log_warning "Services release not found"
    helm uninstall mybank-infrastructure -n "$NAMESPACE" 2>/dev/null || log_warning "Infrastructure release not found"
    log_success "Helm releases cleaned up"
else
    log_info "Namespace $NAMESPACE does not exist, skipping Helm cleanup"
fi

# Delete Kind cluster
log_step "Deleting Kind cluster..."
if cluster_exists; then
    kind delete cluster --name "$CLUSTER_NAME"
    # Wait for cluster to be fully deleted
    sleep 2
    # Force remove any remaining containers
    docker ps -a | grep "$CLUSTER_NAME" | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true
    # Clean up kubectl context
    kubectl config delete-context "kind-$CLUSTER_NAME" 2>/dev/null || true
    kubectl config delete-cluster "kind-$CLUSTER_NAME" 2>/dev/null || true
    log_success "Cluster deleted"
else
    log_info "Cluster does not exist"
fi

# Stop conflicting containers
log_step "Stopping conflicting containers..."
CONFLICTING_IDS=$(docker ps -q --filter "publish=80" --filter "publish=443" 2>/dev/null || true)
if [ -n "$CONFLICTING_IDS" ]; then
    echo "$CONFLICTING_IDS" | xargs docker stop
    echo "$CONFLICTING_IDS" | xargs docker rm
    log_success "Conflicting containers stopped"
else
    log_info "No conflicting containers found"
fi

# Remove Istio directory
log_step "Cleaning up Istio directory..."
if [ -d "istio-1.27.3" ]; then
    rm -rf istio-1.27.3
    log_success "Istio directory removed"
else
    log_info "Istio directory not found"
fi

# Optional: Clean Docker system (commented out for safety)
# log_step "Cleaning Docker system..."
# docker system prune -f
# log_success "Docker system cleaned"

log_success "Cleanup complete! 🧹"
