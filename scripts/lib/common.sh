#!/bin/bash

# Common library for MyBank IaC scripts

# Colors
export RED='\033[0;31m'
export GREEN='\033[0;32m'
export YELLOW='\033[1;33m'
export BLUE='\033[0;34m'
export PURPLE='\033[0;35m'
export CYAN='\033[0;36m'
export NC='\033[0m' # No Color

# Configuration
export CLUSTER_NAME="${CLUSTER_NAME:-mybank-cluster}"
export NAMESPACE="${NAMESPACE:-mybank}"

# Logging functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_step() {
    echo -e "${PURPLE}▶️  $1${NC}"
}

log_header() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check if port is in use
port_in_use() {
    local port=$1
    lsof -i ":$port" >/dev/null 2>&1
}

# Check if Docker is running
docker_running() {
    docker info >/dev/null 2>&1
}

# Check if Kind cluster exists
cluster_exists() {
    kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"
}

# Wait for pods to be ready
wait_for_pods() {
    local namespace=$1
    local label=$2
    local timeout=${3:-300}

    log_step "Waiting for pods with label $label in namespace $namespace..."
    kubectl wait --namespace "$namespace" \
        --for=condition=ready pod \
        --selector="$label" \
        --timeout="${timeout}s" 2>&1
}

# Get pod status
get_pod_status() {
    local namespace=$1
    kubectl get pods -n "$namespace" --no-headers 2>/dev/null | \
        awk '{print $3}' | \
        grep -v -E "Running|Completed|Succeeded" | \
        wc -l | \
        tr -d ' '
}

# Check if service exists
service_exists() {
    local namespace=$1
    local service=$2
    kubectl get svc "$service" -n "$namespace" >/dev/null 2>&1
}

# Port forward with retries
port_forward_with_retry() {
    local namespace=$1
    local service=$2
    local local_port=$3
    local remote_port=$4
    local max_retries=3
    local retry=0

    while [ $retry -lt $max_retries ]; do
        kubectl port-forward -n "$namespace" "svc/$service" "$local_port:$remote_port" &
        local pf_pid=$!
        sleep 3

        if ps -p $pf_pid > /dev/null; then
            echo $pf_pid
            return 0
        fi

        retry=$((retry + 1))
        sleep 2
    done

    return 1
}

# Kill process by PID
kill_process() {
    local pid=$1
    if ps -p "$pid" > /dev/null 2>&1; then
        kill "$pid" 2>/dev/null || true
        sleep 1
    fi
}

# Check if namespace exists
namespace_exists() {
    local namespace=$1
    kubectl get namespace "$namespace" >/dev/null 2>&1
}

# Get resource count
get_resource_count() {
    local namespace=$1
    local resource=$2
    kubectl get "$resource" -n "$namespace" --no-headers 2>/dev/null | wc -l | tr -d ' '
}

# Pretty print key-value
print_kv() {
    local key=$1
    local value=$2
    printf "  %-20s %s\n" "$key:" "$value"
}

# Exit with error
die() {
    log_error "$1"
    exit 1
}

# Require root/sudo for certain operations
require_sudo() {
    if [ "$EUID" -ne 0 ] && [ -z "$SUDO_USER" ]; then
        log_warning "This operation may require sudo privileges"
    fi
}
