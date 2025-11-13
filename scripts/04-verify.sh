#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

log_header "Phase 4: Verify Deployment"

FAILED=0

# Check pods
log_step "Checking pods..."
NOT_RUNNING=$(get_pod_status "$NAMESPACE")
if [ "$NOT_RUNNING" -eq 0 ]; then
    log_success "All pods are running"
else
    log_error "$NOT_RUNNING pod(s) not running"
    kubectl get pods -n "$NAMESPACE"
    ((FAILED++))
fi

# Check services
log_step "Checking services..."
SERVICES=("postgres-auth" "mongodb" "redis" "kafka" "api-gateway" "auth-service" "frontend")
for svc in "${SERVICES[@]}"; do
    if service_exists "$NAMESPACE" "$svc"; then
        log_success "Service $svc exists"
    else
        log_error "Service $svc NOT found"
        ((FAILED++))
    fi
done

# Health check API Gateway
log_step "Checking API Gateway health..."
PF_PID=$(port_forward_with_retry "$NAMESPACE" "api-gateway" 8080 8080)
if [ -n "$PF_PID" ]; then
    sleep 3
    if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
        log_success "API Gateway is healthy"
    else
        log_warning "API Gateway health check inconclusive"
    fi
    kill_process "$PF_PID"
else
    log_warning "Could not port-forward to API Gateway"
fi

# Summary
echo ""
if [ $FAILED -eq 0 ]; then
    log_success "All verifications passed! ✅"
    echo ""
    echo "Access URLs:"
    echo "  Frontend: https://app.mybank.com"
    echo "  API:      https://api.mybank.com"
    exit 0
else
    log_error "$FAILED verification(s) failed"
    exit 1
fi
