# MyBank 360 - Production Deployment Guide

## 🎯 Overview

This guide provides complete instructions for deploying MyBank 360 to a production-ready Kubernetes environment with:

- ✅ HTTPS/TLS encryption
- ✅ Domain-based routing
- ✅ ConfigMap/Secret based configuration
- ✅ ArgoCD GitOps
- ✅ Complete CI/CD pipeline
- ✅ Full observability stack
- ✅ Comprehensive test coverage

## 📋 Prerequisites

### Required Tools

| Tool | Version | Installation |
|------|---------|--------------|
| Docker | 20.10+ | https://docs.docker.com/get-docker/ |
| Kind | 0.20+ | `brew install kind` or https://kind.sigs.k8s.io/ |
| kubectl | 1.28+ | `brew install kubectl` |
| Java | 21+ | https://adoptium.net/ |
| Node.js | 20+ | https://nodejs.org/ |
| OpenSSL | 1.1+ | Usually pre-installed |

### System Requirements

- **CPU**: 4 cores minimum (8 cores recommended)
- **RAM**: 8GB minimum (16GB recommended)
- **Disk**: 20GB free space
- **OS**: macOS, Linux, or Windows with WSL2

## 🚀 Quick Start

### One-Command Deployment

```bash
./scripts/deploy-complete-system.sh
```

This script will:
1. ✅ Check all prerequisites
2. ✅ Create Kind cluster
3. ✅ Build all Docker images
4. ✅ Generate TLS certificates
5. ✅ Setup /etc/hosts
6. ✅ Install NGINX Ingress
7. ✅ Deploy infrastructure (PostgreSQL, MongoDB, Redis, Kafka)
8. ✅ Deploy all microservices
9. ✅ Deploy frontend
10. ✅ Install ArgoCD
11. ✅ Configure domain-based routing

## 📖 Detailed Deployment Steps

### Step 1: Clone Repository

```bash
git clone https://github.com/your-org/my-bank.git
cd my-bank
```

### Step 2: Build Services

```bash
# Build backend services
./gradlew clean build

# Build Docker images
./build-images.sh
```

### Step 3: Create Kubernetes Cluster

```bash
# Using Kind
kind create cluster --config kind-config.yaml

# Or use existing cluster
export KUBECONFIG=/path/to/your/kubeconfig
```

### Step 4: Generate TLS Certificates

```bash
./scripts/generate-certs.sh
```

This creates:
- CA certificate
- Service certificates for all domains
- Kubernetes TLS secrets

### Step 5: Setup Local DNS

```bash
./scripts/setup-hosts.sh
```

Adds these entries to `/etc/hosts`:
```
127.0.0.1 app.mybank.com
127.0.0.1 api.mybank.com
127.0.0.1 eureka.mybank.com
127.0.0.1 grafana.mybank.com
127.0.0.1 kafka-ui.mybank.com
127.0.0.1 prometheus.mybank.com
127.0.0.1 argocd.mybank.com
```

### Step 6: Install NGINX Ingress

```bash
kubectl apply -f k8s/ingress/ingress-nginx-setup.yaml

# Wait for readiness
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s
```

### Step 7: Deploy ConfigMaps and Secrets

```bash
# Create namespace
kubectl create namespace mybank

# Apply configurations
kubectl apply -f k8s/config/
```

### Step 8: Deploy Infrastructure

```bash
kubectl apply -f k8s/infrastructure/

# Wait for readiness
kubectl wait --for=condition=ready pod -l app=postgres -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=mongodb -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=redis -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=kafka -n mybank --timeout=180s
```

### Step 9: Deploy Microservices

```bash
# Load images to Kind
kind load docker-image mybank/service-discovery:latest --name mybank-cluster
kind load docker-image mybank/api-gateway:latest --name mybank-cluster
kind load docker-image mybank/auth-service:latest --name mybank-cluster
kind load docker-image mybank/pfm-core-service:latest --name mybank-cluster
kind load docker-image mybank/payment-service:latest --name mybank-cluster
kind load docker-image mybank/investment-service:latest --name mybank-cluster
kind load docker-image mybank/frontend:latest --name mybank-cluster

# Deploy services
kubectl apply -f k8s/services/
kubectl apply -f k8s/frontend-deployment.yaml
```

### Step 10: Deploy Ingress Rules

```bash
kubectl apply -f k8s/ingress/mybank-ingress.yaml
```

### Step 11: Install ArgoCD

```bash
./scripts/install-argocd.sh

# Get admin password
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d
```

### Step 12: Deploy ArgoCD Applications

```bash
kubectl apply -f k8s/argocd/applications/
```

## 🌐 Access Applications

### URLs

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | https://app.mybank.com | Main web application |
| **API Gateway** | https://api.mybank.com | Backend API |
| **Service Discovery** | https://eureka.mybank.com | Eureka dashboard |
| **Grafana** | https://grafana.mybank.com | Metrics visualization |
| **Kafka UI** | https://kafka-ui.mybank.com | Kafka monitoring |
| **Prometheus** | https://prometheus.mybank.com | Metrics collection |
| **ArgoCD** | https://argocd.mybank.com | GitOps management |

### Default Credentials

| Service | Username | Password |
|---------|----------|----------|
| Grafana | admin | admin |
| ArgoCD | admin | (check `.argocd-password` file) |

## 🔐 TLS/SSL Configuration

### Trust Self-Signed CA

**macOS:**
```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain certs/ca.crt
```

**Linux:**
```bash
sudo cp certs/ca.crt /usr/local/share/ca-certificates/mybank-ca.crt
sudo update-ca-certificates
```

**Browser:**
Accept the security warning when first accessing the site.

## ⚙️ Configuration

### ConfigMaps

All non-sensitive configuration is stored in ConfigMaps:

- `common-config`: Shared configuration (Eureka, Kafka, Redis)
- `auth-service-config`: Auth service specific config
- `pfm-core-service-config`: PFM service specific config
- `payment-service-config`: Payment service specific config
- `investment-service-config`: Investment service specific config
- `api-gateway-config`: API Gateway specific config
- `frontend-config`: Frontend environment variables

### Secrets

Sensitive data is stored in Kubernetes Secrets:

- `common-secret`: Database passwords, JWT secret, etc.

**To update a secret:**
```bash
kubectl edit secret common-secret -n mybank
```

## 🔄 CI/CD Pipeline

### GitHub Actions Workflows

1. **Backend Services CI/CD** (`.github/workflows/backend-services.yml`)
   - Builds and tests all Java services
   - Creates Docker images
   - Pushes to container registry
   - Updates Kubernetes manifests

2. **Frontend CI/CD** (`.github/workflows/frontend.yml`)
   - Lints and type-checks code
   - Runs unit tests
   - Builds Next.js app
   - Creates Docker image
   - Runs E2E tests

### ArgoCD GitOps

ArgoCD automatically syncs Kubernetes resources from Git:

```bash
# View applications
kubectl get applications -n argocd

# Sync manually
argocd app sync mybank-services
```

## 🧪 Testing

### Run All Tests

```bash
# Backend tests
./gradlew test

# Frontend tests
cd app
npm test

# E2E tests
npm run test:e2e
```

### Test Coverage Reports

- **Backend**: `build/reports/jacoco/test/html/index.html`
- **Frontend**: `app/coverage/lcov-report/index.html`

## 📊 Monitoring

### Prometheus

```bash
# Port forward
kubectl port-forward -n mybank svc/prometheus 9090:9090

# Access: http://localhost:9090
```

### Grafana

```bash
# Access: https://grafana.mybank.com
# Login: admin / admin

# Add Prometheus data source
URL: http://prometheus:9090
```

### Kafka UI

```bash
# Access: https://kafka-ui.mybank.com
```

## 🔧 Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n mybank

# View logs
kubectl logs -f <pod-name> -n mybank

# Describe pod
kubectl describe pod <pod-name> -n mybank
```

### Service Connectivity Issues

```bash
# Test from within cluster
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://api-gateway:8080/actuator/health

# Check services
kubectl get svc -n mybank

# Check endpoints
kubectl get endpoints -n mybank
```

### Certificate Issues

```bash
# Check certificate
openssl x509 -in certs/app.crt -text -noout

# Verify TLS secret
kubectl get secret mybank-tls-app -n mybank -o yaml

# Regenerate certificates
./scripts/generate-certs.sh
```

### Ingress Not Working

```bash
# Check Ingress status
kubectl get ingress -n mybank

# Check Ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller

# Verify /etc/hosts
cat /etc/hosts | grep mybank
```

## 🔄 Updates and Rollbacks

### Update Service

```bash
# Update image
kubectl set image deployment/auth-service \
  auth-service=mybank/auth-service:v2.0 -n mybank

# Or edit deployment
kubectl edit deployment auth-service -n mybank

# Check rollout status
kubectl rollout status deployment/auth-service -n mybank
```

### Rollback

```bash
# View rollout history
kubectl rollout history deployment/auth-service -n mybank

# Rollback to previous version
kubectl rollout undo deployment/auth-service -n mybank

# Rollback to specific revision
kubectl rollout undo deployment/auth-service --to-revision=2 -n mybank
```

## 🧹 Cleanup

### Remove Everything

```bash
# Delete Kind cluster
kind delete cluster --name mybank-cluster

# Remove Docker images
docker rmi $(docker images 'mybank/*' -q)

# Remove certificates
rm -rf certs/

# Remove /etc/hosts entries (manual)
sudo vi /etc/hosts
# Remove lines containing mybank.com
```

## 📚 Additional Resources

- [Architecture Documentation](./README.md)
- [Frontend Guide](./FRONTEND_DEPLOYMENT.md)
- [API Documentation](./API.md)
- [Contributing Guide](./CONTRIBUTING.md)

## 🆘 Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review application logs
3. Check GitHub Issues
4. Contact the development team

## 📝 License

MIT License - See LICENSE file for details
