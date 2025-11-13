.PHONY: help init deploy destroy status logs dev build test clean argocd-* helm-* kind-*

# ============================================================================
# Configuration
# ============================================================================

CLUSTER_NAME := mybank-cluster
NAMESPACE := mybank
HELM_TIMEOUT := 10m
ARGOCD_VERSION := stable

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
NC := \033[0m # No Color

# ============================================================================
# Help
# ============================================================================

help: ## Show this help message
	@echo ''
	@echo '$(BLUE)MyBank - Infrastructure as Code$(NC)'
	@echo ''
	@echo '$(YELLOW)Usage:$(NC)'
	@echo '  make <target>'
	@echo ''
	@echo '$(YELLOW)Setup:$(NC)'
	@grep -E '^init:.*?## .*$$|^prereq:.*?## .*$$|^kind-create:.*?## .*$$|^kind-delete:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ''
	@echo '$(YELLOW)Deployment:$(NC)'
	@grep -E '^deploy.*:.*?## .*$$|^destroy:.*?## .*$$|^helm-.*:.*?## .*$$|^helmfile-.*:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ''
	@echo '$(YELLOW)Development:$(NC)'
	@grep -E '^dev:.*?## .*$$|^build:.*?## .*$$|^test:.*?## .*$$|^clean:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ''
	@echo '$(YELLOW)GitOps (ArgoCD):$(NC)'
	@grep -E '^argocd-.*:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ''
	@echo '$(YELLOW)Operations:$(NC)'
	@grep -E '^status:.*?## .*$$|^logs:.*?## .*$$|^port-forward:.*?## .*$$|^shell:.*?## .*$$|^restart:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ''

# ============================================================================
# Prerequisites Check
# ============================================================================

prereq: ## Check if all required tools are installed
	@echo "$(BLUE)Checking prerequisites...$(NC)"
	@command -v docker >/dev/null 2>&1 || (echo "$(RED)✗ Docker not found$(NC)" && exit 1)
	@echo "$(GREEN)✓ Docker$(NC)"
	@command -v kind >/dev/null 2>&1 || (echo "$(RED)✗ Kind not found$(NC)" && exit 1)
	@echo "$(GREEN)✓ Kind$(NC)"
	@command -v kubectl >/dev/null 2>&1 || (echo "$(RED)✗ kubectl not found$(NC)" && exit 1)
	@echo "$(GREEN)✓ kubectl$(NC)"
	@command -v helm >/dev/null 2>&1 || (echo "$(RED)✗ Helm not found$(NC)" && exit 1)
	@echo "$(GREEN)✓ Helm$(NC)"
	@command -v helmfile >/dev/null 2>&1 || (echo "$(YELLOW)⚠ Helmfile not found (optional)$(NC)")
	@command -v tilt >/dev/null 2>&1 || (echo "$(YELLOW)⚠ Tilt not found (optional for dev)$(NC)")
	@command -v istioctl >/dev/null 2>&1 || (echo "$(YELLOW)⚠ istioctl not found (optional)$(NC)")
	@echo "$(GREEN)✓ All required tools are installed!$(NC)"

# ============================================================================
# Kind Cluster Management
# ============================================================================

kind-create: ## Create Kind cluster with port mappings
	@echo "$(BLUE)Creating Kind cluster...$(NC)"
	@if kind get clusters 2>/dev/null | grep -q "^$(CLUSTER_NAME)$$"; then \
		echo "$(YELLOW)Cluster already exists. Use 'make kind-delete' first.$(NC)"; \
		exit 1; \
	fi
	@kind create cluster --config kind-config.yaml --name $(CLUSTER_NAME)
	@kubectl cluster-info --context kind-$(CLUSTER_NAME)
	@echo "$(GREEN)✓ Kind cluster created!$(NC)"

kind-delete: ## Delete Kind cluster
	@echo "$(RED)Deleting Kind cluster...$(NC)"
	@kind delete cluster --name $(CLUSTER_NAME)
	@echo "$(GREEN)✓ Kind cluster deleted!$(NC)"

kind-load-images: ## Load Docker images into Kind cluster
	@echo "$(BLUE)Loading images into Kind cluster...$(NC)"
	@kind load docker-image mybank/api-gateway:latest --name $(CLUSTER_NAME)
	@kind load docker-image mybank/auth-service:latest --name $(CLUSTER_NAME)
	@kind load docker-image mybank/user-service:latest --name $(CLUSTER_NAME)
	@kind load docker-image mybank/analytics-service:latest --name $(CLUSTER_NAME)
	@kind load docker-image mybank/asset-service:latest --name $(CLUSTER_NAME)
	@kind load docker-image mybank/payment-service:latest --name $(CLUSTER_NAME)
	@kind load docker-image mybank/investment-service:latest --name $(CLUSTER_NAME)
	@kind load docker-image mybank/frontend:latest --name $(CLUSTER_NAME)
	@echo "$(GREEN)✓ Images loaded!$(NC)"

# ============================================================================
# Initial Setup
# ============================================================================

init: prereq kind-create istio-install setup-namespace setup-hosts ## Complete initial setup (cluster + Istio + namespace)
	@echo "$(GREEN)✓ Initial setup complete!$(NC)"
	@echo ""
	@echo "$(YELLOW)Next steps:$(NC)"
	@echo "  1. Build images: make build"
	@echo "  2. Deploy apps:  make deploy"
	@echo "  3. Or use Tilt:  make dev"

setup-namespace: ## Create namespace and label for Istio
	@echo "$(BLUE)Creating namespace...$(NC)"
	@kubectl create namespace $(NAMESPACE) --dry-run=client -o yaml | kubectl apply -f -
	@kubectl label namespace $(NAMESPACE) istio-injection=enabled --overwrite
	@echo "$(GREEN)✓ Namespace ready!$(NC)"

setup-hosts: ## Add mybank.com domains to /etc/hosts
	@echo "$(BLUE)Setting up /etc/hosts...$(NC)"
	@if grep -q "mybank.com" /etc/hosts 2>/dev/null; then \
		echo "$(YELLOW)Domains already in /etc/hosts$(NC)"; \
	else \
		echo "$(YELLOW)Adding domains (requires sudo)...$(NC)"; \
		echo "127.0.0.1 app.mybank.com api.mybank.com eureka.mybank.com grafana.mybank.com prometheus.mybank.com argocd.mybank.com" | sudo tee -a /etc/hosts > /dev/null; \
		echo "$(GREEN)✓ Domains added to /etc/hosts!$(NC)"; \
	fi

# ============================================================================
# Istio Service Mesh
# ============================================================================

istio-install: ## Install Istio Service Mesh
	@echo "$(BLUE)Installing Istio...$(NC)"
	@if ! command -v istioctl >/dev/null 2>&1; then \
		echo "$(YELLOW)Downloading Istio...$(NC)"; \
		curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.27.3 sh -; \
	fi
	@if command -v istioctl >/dev/null 2>&1; then \
		istioctl install --set profile=default -y; \
	else \
		$$PWD/istio-1.27.3/bin/istioctl install --set profile=default -y; \
	fi
	@kubectl wait --namespace istio-system --for=condition=ready pod --selector=app=istiod --timeout=300s
	@echo "$(GREEN)✓ Istio installed!$(NC)"

istio-uninstall: ## Uninstall Istio Service Mesh
	@echo "$(RED)Uninstalling Istio...$(NC)"
	@istioctl uninstall --purge -y
	@kubectl delete namespace istio-system
	@echo "$(GREEN)✓ Istio uninstalled!$(NC)"

# ============================================================================
# Build
# ============================================================================

build: ## Build all Docker images
	@echo "$(BLUE)Building all services...$(NC)"
	@./gradlew clean build -x test --no-daemon --parallel
	@echo "$(BLUE)Building Docker images...$(NC)"
	@COMPOSE_PROFILES=build docker-compose build
	@echo "$(GREEN)✓ Build complete!$(NC)"

build-service: ## Build specific service (usage: make build-service SERVICE=auth-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)Error: SERVICE not specified$(NC)"; \
		echo "Usage: make build-service SERVICE=auth-service"; \
		exit 1; \
	fi
	@echo "$(BLUE)Building $(SERVICE)...$(NC)"
	@./gradlew :$(SERVICE):build -x test
	@docker build -t mybank/$(SERVICE):latest -f $(SERVICE)/Dockerfile .
	@echo "$(GREEN)✓ $(SERVICE) built!$(NC)"

# ============================================================================
# Deployment (Helmfile)
# ============================================================================

deploy: build kind-load-images helmfile-sync ## Complete deployment (build + load + deploy via Helmfile)
	@echo "$(GREEN)✓ Deployment complete!$(NC)"
	@make status

deploy-fast: helmfile-sync ## Fast deploy (skip build, use existing images)
	@echo "$(GREEN)✓ Fast deployment complete!$(NC)"

helmfile-sync: ## Deploy all charts using Helmfile
	@echo "$(BLUE)Deploying with Helmfile...$(NC)"
	@helmfile sync
	@echo "$(GREEN)✓ Helmfile sync complete!$(NC)"

helmfile-diff: ## Show diff between current and desired state
	@helmfile diff

helmfile-destroy: ## Remove all Helmfile releases
	@echo "$(RED)Destroying all releases...$(NC)"
	@helmfile destroy
	@echo "$(GREEN)✓ All releases destroyed!$(NC)"

# ============================================================================
# Deployment (Helm directly)
# ============================================================================

helm-install: ## Install all Helm charts individually
	@echo "$(BLUE)Installing Helm charts...$(NC)"
	@helm upgrade --install mybank-infrastructure helm/infrastructure \
		--namespace $(NAMESPACE) --create-namespace --wait --timeout=$(HELM_TIMEOUT)
	@helm upgrade --install mybank-services helm/services \
		--namespace $(NAMESPACE) --wait --timeout=$(HELM_TIMEOUT)
	@helm upgrade --install mybank-frontend helm/frontend \
		--namespace $(NAMESPACE) --wait --timeout=$(HELM_TIMEOUT)
	@echo "$(GREEN)✓ Helm charts installed!$(NC)"

helm-uninstall: ## Uninstall all Helm charts
	@echo "$(RED)Uninstalling Helm charts...$(NC)"
	@helm uninstall mybank-frontend -n $(NAMESPACE) || true
	@helm uninstall mybank-services -n $(NAMESPACE) || true
	@helm uninstall mybank-infrastructure -n $(NAMESPACE) || true
	@echo "$(GREEN)✓ Helm charts uninstalled!$(NC)"

helm-upgrade-service: ## Upgrade specific service (usage: make helm-upgrade-service SERVICE=auth-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)Error: SERVICE not specified$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)Upgrading $(SERVICE)...$(NC)"
	@helm upgrade mybank-services helm/services \
		--namespace $(NAMESPACE) --wait --timeout=$(HELM_TIMEOUT) \
		--set services.$(SERVICE).image.tag=latest
	@kubectl rollout restart deployment/$(SERVICE) -n $(NAMESPACE)
	@echo "$(GREEN)✓ $(SERVICE) upgraded!$(NC)"

# ============================================================================
# ArgoCD GitOps
# ============================================================================

argocd-install: ## Install ArgoCD
	@echo "$(BLUE)Installing ArgoCD...$(NC)"
	@kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
	@kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/$(ARGOCD_VERSION)/manifests/install.yaml
	@kubectl wait --namespace argocd --for=condition=ready pod --selector=app.kubernetes.io/name=argocd-server --timeout=300s
	@echo "$(GREEN)✓ ArgoCD installed!$(NC)"
	@echo ""
	@echo "$(YELLOW)ArgoCD Password:$(NC)"
	@kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath="{.data.password}" | base64 -d && echo ""
	@echo ""
	@echo "$(YELLOW)Access ArgoCD:$(NC)"
	@echo "  kubectl port-forward svc/argocd-server -n argocd 8080:443"
	@echo "  Then open: https://localhost:8080"
	@echo "  Username: admin"

argocd-deploy: ## Deploy applications via ArgoCD (App of Apps pattern)
	@echo "$(BLUE)Deploying applications via ArgoCD...$(NC)"
	@kubectl apply -f argocd/applications/app-of-apps.yaml
	@echo "$(GREEN)✓ ArgoCD applications deployed!$(NC)"
	@echo "Monitor at: https://localhost:8080 (after port-forward)"

argocd-password: ## Get ArgoCD admin password
	@kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath="{.data.password}" | base64 -d && echo ""

argocd-ui: ## Open ArgoCD UI (requires port-forward)
	@echo "$(YELLOW)Opening ArgoCD UI...$(NC)"
	@echo "Starting port-forward (Ctrl+C to stop)..."
	@kubectl port-forward svc/argocd-server -n argocd 8080:443

argocd-uninstall: ## Uninstall ArgoCD
	@echo "$(RED)Uninstalling ArgoCD...$(NC)"
	@kubectl delete -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/$(ARGOCD_VERSION)/manifests/install.yaml
	@kubectl delete namespace argocd
	@echo "$(GREEN)✓ ArgoCD uninstalled!$(NC)"

# ============================================================================
# Development (Tilt)
# ============================================================================

dev: ## Start Tilt for local development with hot-reload
	@echo "$(BLUE)Starting Tilt...$(NC)"
	@echo "Web UI will open at: http://localhost:10350"
	@tilt up

dev-down: ## Stop Tilt
	@tilt down

# ============================================================================
# Operations
# ============================================================================

status: ## Show status of all resources
	@echo "$(BLUE)Cluster Status:$(NC)"
	@kubectl get nodes
	@echo ""
	@echo "$(BLUE)Pods:$(NC)"
	@kubectl get pods -n $(NAMESPACE)
	@echo ""
	@echo "$(BLUE)Services:$(NC)"
	@kubectl get svc -n $(NAMESPACE)
	@echo ""
	@echo "$(BLUE)Deployments:$(NC)"
	@kubectl get deployments -n $(NAMESPACE)

logs: ## Stream logs from all backend services
	@kubectl logs -f -l tier=backend -n $(NAMESPACE) --all-containers --max-log-requests=10

logs-service: ## Stream logs from specific service (usage: make logs-service SERVICE=auth-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)Error: SERVICE not specified$(NC)"; \
		exit 1; \
	fi
	@kubectl logs -f deployment/$(SERVICE) -n $(NAMESPACE)

port-forward: ## Port forward to API Gateway
	@echo "$(YELLOW)Port forwarding to API Gateway...$(NC)"
	@kubectl port-forward svc/api-gateway -n $(NAMESPACE) 8080:8080

shell: ## Open shell in specific service (usage: make shell SERVICE=auth-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)Error: SERVICE not specified$(NC)"; \
		exit 1; \
	fi
	@kubectl exec -it deployment/$(SERVICE) -n $(NAMESPACE) -- /bin/sh

restart: ## Restart all deployments
	@echo "$(BLUE)Restarting all deployments...$(NC)"
	@kubectl rollout restart deployment -n $(NAMESPACE)
	@echo "$(GREEN)✓ All deployments restarted!$(NC)"

restart-service: ## Restart specific service (usage: make restart-service SERVICE=auth-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)Error: SERVICE not specified$(NC)"; \
		exit 1; \
	fi
	@kubectl rollout restart deployment/$(SERVICE) -n $(NAMESPACE)
	@kubectl rollout status deployment/$(SERVICE) -n $(NAMESPACE)

# ============================================================================
# Testing
# ============================================================================

test: ## Run all tests
	@./gradlew test

test-service: ## Run tests for specific service (usage: make test-service SERVICE=auth-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)Error: SERVICE not specified$(NC)"; \
		exit 1; \
	fi
	@./gradlew :$(SERVICE):test

test-e2e: ## Run complete end-to-end test suite (15-20 minutes)
	@echo "$(BLUE)Running end-to-end test suite...$(NC)"
	@./test-e2e.sh

test-quick: ## Quick validation test (no deployment)
	@echo "$(BLUE)Running quick validation tests...$(NC)"
	@echo ""
	@echo "$(YELLOW)1. Prerequisites:$(NC)"
	@make prereq
	@echo ""
	@echo "$(YELLOW)2. Helm Charts:$(NC)"
	@helm lint helm/infrastructure && echo "$(GREEN)✓ Infrastructure chart valid$(NC)"
	@helm lint helm/services && echo "$(GREEN)✓ Services chart valid$(NC)"
	@helm lint helm/frontend && echo "$(GREEN)✓ Frontend chart valid$(NC)"
	@echo ""
	@echo "$(YELLOW)3. Helmfile:$(NC)"
	@python3 -c "import yaml; yaml.safe_load(open('helmfile.yaml')); print('✓ Helmfile YAML valid')"
	@echo ""
	@echo "$(YELLOW)4. ArgoCD:$(NC)"
	@find argocd/applications -name "*.yaml" -exec python3 -c "import yaml, sys; yaml.safe_load(open(sys.argv[1]))" {} \; && echo "$(GREEN)✓ ArgoCD manifests valid$(NC)"
	@echo ""
	@echo "$(GREEN)✅ All quick tests passed!$(NC)"

test-deployment: ## Test deployment without cleanup
	@echo "$(BLUE)Testing deployment workflow...$(NC)"
	@make init
	@make build
	@make kind-load-images
	@make helm-install
	@echo ""
	@echo "$(YELLOW)Waiting for pods to be ready...$(NC)"
	@sleep 60
	@make status
	@echo ""
	@echo "$(GREEN)✅ Deployment test complete!$(NC)"
	@echo "$(YELLOW)To cleanup: make destroy$(NC)"

verify: ## Verify current deployment
	@echo "$(BLUE)Verifying deployment...$(NC)"
	@echo ""
	@echo "$(YELLOW)Pods:$(NC)"
	@kubectl get pods -n $(NAMESPACE) 2>/dev/null || echo "$(RED)No pods found$(NC)"
	@echo ""
	@echo "$(YELLOW)Services:$(NC)"
	@kubectl get svc -n $(NAMESPACE) 2>/dev/null || echo "$(RED)No services found$(NC)"
	@echo ""
	@echo "$(YELLOW)Pod Status Summary:$(NC)"
	@running=$$(kubectl get pods -n $(NAMESPACE) --field-selector=status.phase=Running --no-headers 2>/dev/null | wc -l | xargs); \
	total=$$(kubectl get pods -n $(NAMESPACE) --no-headers 2>/dev/null | wc -l | xargs); \
	if [ $$total -eq 0 ]; then \
		echo "$(RED)No deployment found$(NC)"; \
	elif [ $$running -eq $$total ]; then \
		echo "$(GREEN)✅ All $$total pods running$(NC)"; \
	else \
		echo "$(YELLOW)⚠️  $$running/$$total pods running$(NC)"; \
	fi

# ============================================================================
# Cleanup
# ============================================================================

clean: ## Clean build artifacts
	@./gradlew clean
	@rm -rf build/
	@echo "$(GREEN)✓ Clean complete!$(NC)"

stop: ## Stop all services but keep cluster running
	@echo "$(YELLOW)Stopping all services...$(NC)"
	@kubectl scale deployment --all --replicas=0 -n $(NAMESPACE) 2>/dev/null || true
	@kubectl scale statefulset --all --replicas=0 -n $(NAMESPACE) 2>/dev/null || true
	@echo "$(GREEN)✓ All services stopped (cluster still running)$(NC)"
	@echo "$(BLUE)To restart: make restart$(NC)"

shutdown: helm-uninstall ## Shutdown: Remove all apps but keep cluster
	@echo "$(YELLOW)Shutting down applications...$(NC)"
	@echo "$(GREEN)✓ Applications removed (cluster still running)$(NC)"
	@echo "$(BLUE)Cluster is still active. To remove: make kind-delete$(NC)"

destroy: helm-uninstall kind-delete ## Complete cleanup (remove everything)
	@echo "$(RED)Destroying everything...$(NC)"
	@echo "$(GREEN)✓ Everything destroyed!$(NC)"

nuke: ## Nuclear option - force delete everything including Docker containers
	@echo "$(RED)⚠️  NUCLEAR CLEANUP - This will force delete EVERYTHING!$(NC)"
	@echo "$(YELLOW)Stopping Tilt...$(NC)"
	@tilt down 2>/dev/null || true
	@echo "$(YELLOW)Deleting Helm releases...$(NC)"
	@helm uninstall mybank-frontend -n $(NAMESPACE) 2>/dev/null || true
	@helm uninstall mybank-services -n $(NAMESPACE) 2>/dev/null || true
	@helm uninstall mybank-infrastructure -n $(NAMESPACE) 2>/dev/null || true
	@echo "$(YELLOW)Deleting ArgoCD...$(NC)"
	@kubectl delete namespace argocd 2>/dev/null || true
	@echo "$(YELLOW)Deleting namespace...$(NC)"
	@kubectl delete namespace $(NAMESPACE) 2>/dev/null || true
	@echo "$(YELLOW)Deleting Kind cluster...$(NC)"
	@kind delete cluster --name $(CLUSTER_NAME) 2>/dev/null || true
	@echo "$(YELLOW)Removing Docker images...$(NC)"
	@docker images | grep mybank | awk '{print $$1":"$$2}' | xargs docker rmi -f 2>/dev/null || true
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	@./gradlew clean 2>/dev/null || true
	@rm -rf build/ 2>/dev/null || true
	@echo "$(GREEN)✓ Nuclear cleanup complete!$(NC)"

# ============================================================================
# Info
# ============================================================================

info: ## Show access information
	@echo ""
	@echo "$(YELLOW)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)                       MyBank Access Information$(NC)"
	@echo "$(YELLOW)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo ""
	@echo "$(BLUE)🌐 Application URLs:$(NC)"
	@echo "   Frontend:          https://app.mybank.com"
	@echo "   API Gateway:       https://api.mybank.com"
	@echo "   Frontend NodePort: http://localhost:30000"
	@echo ""
	@echo "$(BLUE)🔧 Development Tools:$(NC)"
	@echo "   Tilt UI:          http://localhost:10350"
	@echo "   ArgoCD UI:        https://localhost:8080 (after port-forward)"
	@echo ""
	@echo "$(BLUE)📊 Monitoring:$(NC)"
	@echo "   Prometheus:       http://localhost:9090"
	@echo "   Grafana:          http://localhost:3001"
	@echo ""
	@echo "$(BLUE)🔍 Useful Commands:$(NC)"
	@echo "   Check pods:       kubectl get pods -n $(NAMESPACE)"
	@echo "   Stream logs:      make logs"
	@echo "   Port forward:     make port-forward"
	@echo "   Restart all:      make restart"
	@echo ""
	@echo "$(YELLOW)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo ""
