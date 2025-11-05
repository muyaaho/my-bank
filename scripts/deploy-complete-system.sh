#!/bin/bash

#######################################################################################
# MyBank - Complete Production Deployment Script
# This script deploys the entire MyBank system to Kubernetes (Kind)
#
# Features:
# - HTTPS with self-signed certificates
# - Domain-based routing (*.mybank.com)
# - ConfigMap/Secret based configuration
# - ArgoCD GitOps
# - Full observability stack
#######################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
CLUSTER_NAME="mybank-cluster"
NAMESPACE="mybank"

# Helper functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  $1"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"

    local missing_tools=()

    if ! command -v kind &> /dev/null; then
        missing_tools+=("kind")
    fi

    if ! command -v kubectl &> /dev/null; then
        missing_tools+=("kubectl")
    fi

    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi

    if ! command -v openssl &> /dev/null; then
        missing_tools+=("openssl")
    fi

    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        echo ""
        echo "Installation instructions:"
        echo "  kind: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
        echo "  kubectl: https://kubernetes.io/docs/tasks/tools/"
        echo "  docker: https://docs.docker.com/get-docker/"
        exit 1
    fi

    log_success "All prerequisites met"
}

# Create Kind cluster
create_cluster() {
    print_header "Creating Kind Cluster"

    if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        log_warning "Cluster '${CLUSTER_NAME}' already exists"
        read -p "Do you want to delete and recreate it? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            log_info "Deleting existing cluster..."
            kind delete cluster --name "${CLUSTER_NAME}"
        else
            log_info "Using existing cluster"
            return
        fi
    fi

    log_info "Creating Kind cluster with custom configuration..."
    kind create cluster --config kind-config.yaml

    log_success "Kind cluster created"
}

# Build Docker images
build_images() {
    print_header "Building Docker Images"

    log_info "Building backend services..."
    ./gradlew clean build -x test --no-daemon --parallel

    log_info "Building Docker images..."
    ./build-images.sh

    log_success "All images built"
}

# Load images to Kind
load_images_to_kind() {
    print_header "Loading Images to Kind"

    local images=(
        "mybank/service-discovery:latest"
        "mybank/api-gateway:latest"
        "mybank/auth-service:latest"
        "mybank/pfm-core-service:latest"
        "mybank/payment-service:latest"
        "mybank/investment-service:latest"
        "mybank/frontend:latest"
    )

    for image in "${images[@]}"; do
        log_info "Loading $image..."
        kind load docker-image "$image" --name "${CLUSTER_NAME}"
    done

    log_success "All images loaded to Kind"
}

# Setup /etc/hosts
setup_hosts() {
    print_header "Setting up /etc/hosts"

    if ! grep -q "mybank.com" /etc/hosts; then
        log_info "Adding MyBank domains to /etc/hosts..."
        ./scripts/setup-hosts.sh
    else
        log_warning "MyBank domains already in /etc/hosts"
    fi

    log_success "/etc/hosts configured"
}

# Generate TLS certificates
generate_certificates() {
    print_header "Generating TLS Certificates"

    if [ -d "./certs" ] && [ -f "./certs/ca.crt" ]; then
        log_warning "Certificates already exist"
        read -p "Do you want to regenerate them? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Using existing certificates"
            return
        fi
    fi

    log_info "Generating self-signed certificates..."
    ./scripts/generate-certs.sh

    log_success "Certificates generated"
}

# Install Istio
install_istio() {
    print_header "Installing Istio Service Mesh"

    if ! command -v istioctl &> /dev/null; then
        log_warning "istioctl not found. Installing Istio..."
        curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.27.3 sh -
        export PATH=$PWD/istio-1.27.3/bin:$PATH
    fi

    log_info "Installing Istio with default profile..."
    istioctl install --set profile=default -y

    log_info "Waiting for Istio components to be ready..."
    kubectl wait --namespace istio-system \
        --for=condition=ready pod \
        --selector=app=istiod \
        --timeout=300s

    log_success "Istio installed"
}

# Deploy infrastructure
deploy_infrastructure() {
    print_header "Deploying Infrastructure Services"

    # Create namespace
    log_info "Creating namespace..."
    kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

    # Apply ConfigMaps and Secrets
    log_info "Applying ConfigMaps and Secrets..."
    kubectl apply -f k8s/config/

    # Deploy infrastructure services
    log_info "Deploying databases and message queue..."
    kubectl apply -f k8s/infrastructure/

    # Wait for infrastructure to be ready
    log_info "Waiting for infrastructure pods..."
    sleep 20
    kubectl wait --for=condition=ready pod -l app=postgres -n "${NAMESPACE}" --timeout=180s
    kubectl wait --for=condition=ready pod -l app=mongodb -n "${NAMESPACE}" --timeout=180s
    kubectl wait --for=condition=ready pod -l app=redis -n "${NAMESPACE}" --timeout=180s
    kubectl wait --for=condition=ready pod -l app=kafka -n "${NAMESPACE}" --timeout=180s

    log_success "Infrastructure deployed"
}

# Deploy microservices
deploy_services() {
    print_header "Deploying Microservices"

    # Deploy Service Discovery first
    log_info "Deploying Service Discovery..."
    kubectl apply -f k8s/services/service-discovery.yaml
    sleep 15
    kubectl wait --for=condition=ready pod -l app=service-discovery -n "${NAMESPACE}" --timeout=180s

    # Deploy API Gateway
    log_info "Deploying API Gateway..."
    kubectl apply -f k8s/services/api-gateway.yaml
    sleep 10

    # Deploy business services
    log_info "Deploying business services..."
    kubectl apply -f k8s/services/auth-service.yaml
    kubectl apply -f k8s/services/pfm-core-service.yaml
    kubectl apply -f k8s/services/payment-service.yaml
    kubectl apply -f k8s/services/investment-service.yaml

    # Deploy frontend
    log_info "Deploying frontend..."
    kubectl apply -f k8s/services/frontend.yaml

    log_success "Microservices deployed"
}

# Deploy Istio Gateway and VirtualServices
deploy_istio_routing() {
    print_header "Deploying Istio Gateway and VirtualServices"

    log_info "Applying Istio Gateway..."
    kubectl apply -f k8s/istio/gateway.yaml

    log_info "Applying VirtualServices..."
    kubectl apply -f k8s/istio/virtual-service.yaml

    log_success "Istio routing configured"
}

# Install ArgoCD
install_argocd() {
    print_header "Installing ArgoCD"

    log_info "Installing ArgoCD..."
    ./scripts/install-argocd.sh

    log_success "ArgoCD installed"
}

# Verify deployment
verify_deployment() {
    print_header "Verifying Deployment"

    log_info "Checking pod status..."
    kubectl get pods -n "${NAMESPACE}"

    echo ""
    log_info "Checking services..."
    kubectl get svc -n "${NAMESPACE}"

    echo ""
    log_info "Checking Istio Gateway..."
    kubectl get gateway -n "${NAMESPACE}"

    echo ""
    log_info "Checking VirtualServices..."
    kubectl get virtualservice -n "${NAMESPACE}"

    log_success "Deployment verification complete"
}

# Print access information
print_access_info() {
    print_header "Access Information"

    echo "🌐 Application URLs:"
    echo ""
    echo "  Frontend:              https://app.mybank.com"
    echo "  API Gateway:           https://api.mybank.com"
    echo "  Service Discovery:     https://eureka.mybank.com"
    echo "  Grafana:               https://grafana.mybank.com"
    echo "  Kafka UI:              https://kafka-ui.mybank.com"
    echo "  Prometheus:            https://prometheus.mybank.com"
    echo "  ArgoCD:                https://argocd.mybank.com"
    echo ""
    echo "📝 Default Credentials:"
    echo ""
    echo "  Grafana:    admin / admin"
    echo "  ArgoCD:     Check .argocd-password file"
    echo ""
    echo "🔐 Certificate Trust:"
    echo ""
    echo "  The system uses self-signed certificates."
    echo "  To trust them on your system:"
    echo ""
    echo "  macOS:"
    echo "    sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain certs/ca.crt"
    echo ""
    echo "  Linux:"
    echo "    sudo cp certs/ca.crt /usr/local/share/ca-certificates/mybank-ca.crt"
    echo "    sudo update-ca-certificates"
    echo ""
    echo "  Or accept the security warning in your browser"
    echo ""
    echo "🔍 Useful Commands:"
    echo ""
    echo "  View all pods:          kubectl get pods -n mybank"
    echo "  View logs:              kubectl logs -f <pod-name> -n mybank"
    echo "  Port forward API:       kubectl port-forward -n mybank svc/api-gateway 8080:8080"
    echo "  Restart deployment:     kubectl rollout restart deployment/<name> -n mybank"
    echo ""
    echo "📚 Documentation:"
    echo ""
    echo "  README.md                  - Main documentation"
    echo "  FRONTEND_DEPLOYMENT.md     - Frontend deployment guide"
    echo "  DEPLOYMENT.md              - Infrastructure deployment guide"
    echo ""
}

# Main execution
main() {
    clear

    cat << "EOF"
    ╔══════════════════════════════════════════════════════════╗
    ║                                                          ║
    ║              MyBank - Deployment Script              ║
    ║                                                          ║
    ║         Complete Production-Ready Deployment             ║
    ║                                                          ║
    ╚══════════════════════════════════════════════════════════╝
EOF

    echo ""
    log_warning "This script will deploy the complete MyBank system."
    log_warning "Make sure you have reviewed the prerequisites."
    echo ""
    read -p "Do you want to continue? (y/N) " -n 1 -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Deployment cancelled"
        exit 0
    fi

    # Execute deployment steps
    check_prerequisites
    create_cluster
    build_images
    load_images_to_kind
    setup_hosts
    install_istio
    generate_certificates
    deploy_infrastructure
    deploy_services
    deploy_istio_routing
    install_argocd
    verify_deployment
    print_access_info

    echo ""
    log_success "🎉 MyBank deployment completed successfully!"
    echo ""
    log_info "The system is now running. Please wait a few minutes for all services to start."
    log_info "You can monitor the progress with: kubectl get pods -n mybank -w"
    echo ""
}

# Run main function
main "$@"
