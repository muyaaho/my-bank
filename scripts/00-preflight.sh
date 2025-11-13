#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

log_header "Phase 0: Pre-flight Checks"

FAILED_CHECKS=0

# Check required tools
log_step "Checking required tools..."
TOOLS=("docker" "kind" "kubectl" "helm" "make")
for tool in "${TOOLS[@]}"; do
    if command_exists "$tool"; then
        log_success "$tool is installed"
    else
        log_error "$tool is NOT installed"
        ((FAILED_CHECKS++))
    fi
done

# Check Docker daemon
log_step "Checking Docker daemon..."
if docker_running; then
    log_success "Docker is running"
    docker version --format 'Docker version: {{.Server.Version}}'
else
    log_error "Docker is NOT running"
    ((FAILED_CHECKS++))
fi

# Check required ports
log_step "Checking required ports..."
PORTS=(80 443 30000 30001 30002)
for port in "${PORTS[@]}"; do
    # Check for LISTEN state only (not established connections)
    # Filter to only show TCP, exclude UDP
    if lsof -i ":$port" -sTCP:LISTEN 2>/dev/null | grep -q "TCP"; then
        log_error "Port $port is IN USE (LISTEN)"
        echo "Processes listening on port $port:"
        lsof -i ":$port" -sTCP:LISTEN | grep "TCP" | head -3
        ((FAILED_CHECKS++))
    else
        log_success "Port $port is available"
    fi
done

# Check disk space
log_step "Checking disk space..."
AVAILABLE_GB=$(df -h . | awk 'NR==2 {print $4}' | sed 's/Gi//g' | sed 's/G//g')
if [ "${AVAILABLE_GB%.*}" -lt 10 ]; then
    log_warning "Low disk space: ${AVAILABLE_GB}GB available (recommended: 10GB+)"
else
    log_success "Disk space OK: ${AVAILABLE_GB}GB available"
fi

# Check memory
log_step "Checking memory..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    TOTAL_MEM_GB=$(sysctl -n hw.memsize | awk '{print int($1/1024/1024/1024)}')
else
    TOTAL_MEM_GB=$(free -g | awk 'NR==2 {print $2}')
fi
if [ "$TOTAL_MEM_GB" -lt 8 ]; then
    log_warning "Low memory: ${TOTAL_MEM_GB}GB (recommended: 8GB+)"
else
    log_success "Memory OK: ${TOTAL_MEM_GB}GB"
fi

# Check for conflicting resources
log_step "Checking for conflicting resources..."

# Check for running containers on required ports
CONFLICTING_CONTAINERS=$(docker ps -q --filter "publish=80" --filter "publish=443" 2>/dev/null | wc -l | tr -d ' ')
if [ "$CONFLICTING_CONTAINERS" -gt 0 ]; then
    log_error "Found $CONFLICTING_CONTAINERS container(s) using ports 80/443"
    docker ps --filter "publish=80" --filter "publish=443"
    ((FAILED_CHECKS++))
else
    log_success "No conflicting containers found"
fi

# Check if cluster already exists
if cluster_exists; then
    log_warning "Cluster '${CLUSTER_NAME}' already exists"
    echo "  Use 'task cleanup:cluster' to remove it"
else
    log_success "No existing cluster found"
fi

# Summary
echo ""
log_header "Pre-flight Summary"
if [ $FAILED_CHECKS -eq 0 ]; then
    log_success "All pre-flight checks passed! ✈️"
    echo ""
    echo "You can proceed with:"
    echo "  task provision   # Provision infrastructure"
    echo "  task up          # Complete setup"
    exit 0
else
    log_error "$FAILED_CHECKS check(s) failed"
    echo ""
    echo "Please fix the issues above before proceeding"
    exit 1
fi
