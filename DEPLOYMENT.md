# MyBank - Deployment Guide (Kind)

## 프로젝트 구조 (Flat Multi-Module MSA)

```
my-bank/
├── api-gateway/              # API Gateway (Port 8080)
├── config-server/            # Config Server (Port 8888)
├── service-discovery/        # Eureka Server (Port 8761)
├── auth-service/             # Authentication Service (Port 8081)
├── pfm-core-service/         # PFM Service (Port 8082)
├── payment-service/          # Payment Service (Port 8083)
├── investment-service/       # Investment Service (Port 8084)
├── common-lib/               # Shared Library
├── k8s/                      # Kubernetes Manifests
│   ├── namespace.yaml
│   ├── infrastructure/       # PostgreSQL, MongoDB, Redis, Kafka
│   └── services/             # Microservices Deployments
├── docker-compose.yml        # Local Development
├── kind-config.yaml          # Kind Cluster Configuration
├── build-images.sh           # Build Docker Images
├── deploy-kind.sh            # Deploy to Kind
└── kind-deploy-all.sh        # Complete Deployment
```

## Prerequisites

1. **Java 17**: OpenJDK 17 or higher
2. **Docker Desktop**: Running with sufficient resources
3. **Kind**: Kubernetes in Docker
4. **kubectl**: Kubernetes CLI

### Install Kind

```bash
# macOS
brew install kind

# Or download from: https://kind.sigs.k8s.io/docs/user/quick-start/#installation
```

### Verify Installation

```bash
java -version        # Should show 17+
docker --version
kind --version
kubectl version --client
```

## Complete Deployment Process

### Option 1: One-Command Deployment

```bash
# 1. Start Docker Desktop first!

# 2. Run complete deployment
./kind-deploy-all.sh
```

This script will:
1. Build all Spring Boot services
2. Create Docker images
3. Create Kind cluster
4. Load images to Kind
5. Deploy all services

### Option 2: Step-by-Step Deployment

#### Step 1: Build Gradle Project

```bash
./gradlew clean build -x test
```

#### Step 2: Build Docker Images

```bash
./build-images.sh
```

Built images:
- `mybank/service-discovery:latest`
- `mybank/config-server:latest`
- `mybank/api-gateway:latest`
- `mybank/auth-service:latest`
- `mybank/pfm-core-service:latest`
- `mybank/payment-service:latest`
- `mybank/investment-service:latest`

#### Step 3: Deploy to Kind

```bash
./deploy-kind.sh
```

This will:
1. Create Kind cluster with 1 control-plane + 2 workers
2. Load Docker images to Kind
3. Deploy infrastructure (PostgreSQL, MongoDB, Redis, Kafka)
4. Deploy Spring Boot microservices

## Access Services

After deployment, services are accessible via NodePort:

| Service | URL | Port |
|---------|-----|------|
| API Gateway | http://localhost:8080 | 30080 |
| Eureka Dashboard | http://localhost:8761 | 30761 |
| Kafka UI | http://localhost:8090 | 30090 |
| Prometheus | http://localhost:9090 | 30900 |
| Grafana | http://localhost:3000 | 30300 |

## Verify Deployment

```bash
# Check all pods
kubectl get pods -n mybank

# Check services
kubectl get svc -n mybank

# Check logs
kubectl logs -f deployment/api-gateway -n mybank

# Port forward (alternative access)
kubectl port-forward -n mybank svc/api-gateway 8080:8080
```

## Test the API

### 1. Register User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.com",
    "password": "Test123!@#",
    "name": "Test User",
    "phoneNumber": "010-1234-5678"
  }'
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.com",
    "password": "Test123!@#"
  }'
```

Save the `accessToken` from the response.

### 3. Get Assets

```bash
curl http://localhost:8080/api/v1/pfm/assets \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 4. Transfer Money

```bash
curl -X POST http://localhost:8080/api/v1/payment/transfer \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "acc-123",
    "toAccountId": "acc-456",
    "recipientName": "Jane Doe",
    "amount": 3450,
    "description": "Test transfer"
  }'
```

## Monitoring

### Eureka Dashboard

Visit http://localhost:8761 to see all registered services.

### Kafka UI

Visit http://localhost:8090 to monitor Kafka topics:
- `transaction-events`
- `payment-completed`

### Prometheus & Grafana

1. Prometheus: http://localhost:9090
2. Grafana: http://localhost:3000 (admin/admin)
   - Add Prometheus data source: `http://prometheus:9090`
   - Import dashboard ID: 4701 (JVM Micrometer)

## Troubleshooting

### Port Already in Use

```bash
# Kill processes using ports
lsof -ti:8080 | xargs kill -9
lsof -ti:8761 | xargs kill -9
lsof -ti:8090 | xargs kill -9
lsof -ti:9090 | xargs kill -9
lsof -ti:3000 | xargs kill -9

# Or stop Docker Compose if running
docker-compose down
```

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n mybank

# Check logs
kubectl logs <pod-name> -n mybank

# Restart deployment
kubectl rollout restart deployment/<deployment-name> -n mybank
```

### Images Not Found

```bash
# Rebuild and reload images
./build-images.sh
kind load docker-image mybank/api-gateway:latest --name mybank-cluster
```

### Database Connection Issues

```bash
# Check infrastructure pods
kubectl get pods -n mybank | grep -E 'postgres|mongodb|redis|kafka'

# Restart infrastructure
kubectl delete pod <pod-name> -n mybank
```

## Clean Up

### Delete Deployments Only

```bash
./undeploy-kind.sh
```

### Delete Everything Including Cluster

```bash
./undeploy-kind.sh --delete-cluster

# Or manually
kind delete cluster --name mybank-cluster
```

## Architecture Highlights

### MSA Pattern

- **Flat Multi-Module Structure**: Each service is an independent root-level module
- **Service Discovery**: Eureka for service registration and discovery
- **API Gateway**: Centralized routing and JWT authentication
- **Config Server**: Centralized configuration management

### EDA Pattern

- **Kafka Event Streaming**: Asynchronous communication between services
- **Event-Driven Round-Up Investing**: Payment events trigger automatic investment
- **Transaction Analysis**: Real-time consumption analysis via event consumption

### Infrastructure

- **PostgreSQL**: Relational data (Auth)
- **MongoDB**: Flexible schema (PFM, Payment, Investment)
- **Redis**: Caching and session management
- **Kafka**: Event streaming and messaging

## Scaling

### Scale Microservices

```bash
# Scale API Gateway to 3 replicas
kubectl scale deployment api-gateway -n mybank --replicas=3

# Scale all business services
kubectl scale deployment auth-service -n mybank --replicas=3
kubectl scale deployment pfm-core-service -n mybank --replicas=3
kubectl scale deployment payment-service -n mybank --replicas=3
kubectl scale deployment investment-service -n mybank --replicas=3
```

### Add More Worker Nodes

Edit `kind-config.yaml` and add more worker nodes, then recreate cluster.

## Production Considerations

1. **Secrets Management**: Use Kubernetes Secrets or external secret managers
2. **Persistent Volumes**: Configure PVC with proper storage classes
3. **Resource Limits**: Adjust CPU/Memory limits based on load
4. **Health Checks**: Configure proper liveness/readiness probes
5. **Logging**: Integrate with ELK or Loki stack
6. **Monitoring**: Set up comprehensive Prometheus alerts
7. **Security**: Enable network policies, pod security policies
8. **Backup**: Implement database backup strategies

## Next Steps

1. Implement AI Coach Service
2. Implement Content & Community Service
3. Add Saga pattern for distributed transactions
4. Implement circuit breaker with Resilience4j
5. Add API rate limiting
6. Implement comprehensive testing
7. Set up CI/CD pipeline

## Support

For issues or questions:
- Check logs: `kubectl logs -f <pod-name> -n mybank`
- Describe resources: `kubectl describe <resource> <name> -n mybank`
- Check events: `kubectl get events -n mybank --sort-by='.lastTimestamp'`
