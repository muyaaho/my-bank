# MyBank Kubernetes Resources

이 디렉토리는 MyBank 마이크로서비스 플랫폼의 Kubernetes 배포 리소스를 포함합니다.

## 📁 디렉토리 구조

```
k8s/
├── helm/                          # Helm Charts
│   └── mybank/                    # Main Helm chart
│       ├── Chart.yaml             # Chart metadata
│       ├── values.yaml            # Default values
│       ├── templates/             # Kubernetes templates
│       └── charts/                # Subcharts
│           ├── infrastructure/    # DB, Kafka, Redis
│           ├── services/          # Backend microservices
│           └── frontend/          # Frontend application
│
├── kustomize/                     # Kustomize overlays
│   ├── base/                      # Base configurations
│   ├── overlays/
│   │   ├── development/           # Dev environment
│   │   ├── staging/               # Staging environment
│   │   └── production/            # Production environment
│   └── kustomization.yaml
│
├── config/                        # ConfigMaps and Secrets
│   ├── common-configmap.yaml
│   ├── common-secret.yaml
│   └── *-configmap.yaml
│
├── services/                      # Service deployments (legacy)
│   ├── api-gateway.yaml
│   ├── auth-service.yaml
│   └── ...
│
├── infrastructure/                # Infrastructure resources (legacy)
│   ├── postgres.yaml
│   ├── mongodb.yaml
│   ├── redis.yaml
│   └── kafka.yaml
│
├── istio/                         # Istio service mesh
│   ├── gateway.yaml
│   └── virtual-service.yaml
│
├── argocd/                        # ArgoCD GitOps
│   ├── install-argocd.yaml
│   └── applications/
│
└── scripts/                       # Deployment scripts
    ├── deploy-helm.sh
    └── deploy-kustomize.sh
```

## 🚀 배포 방법

### 1. Helm을 사용한 배포 (권장)

#### 전체 스택 배포
```bash
# Namespace 생성
kubectl create namespace mybank
kubectl label namespace mybank istio-injection=enabled

# Helm으로 배포
helm install mybank ./helm/mybank \
  --namespace mybank \
  --create-namespace

# 또는 values 파일 오버라이드
helm install mybank ./helm/mybank \
  --namespace mybank \
  --values ./helm/mybank/values-production.yaml
```

#### 인프라만 배포
```bash
helm install mybank-infra ./helm/mybank \
  --namespace mybank \
  --set services.enabled=false \
  --set frontend.enabled=false
```

#### 서비스만 배포
```bash
helm install mybank-services ./helm/mybank \
  --namespace mybank \
  --set infrastructure.enabled=false \
  --set frontend.enabled=false
```

#### 업그레이드
```bash
helm upgrade mybank ./helm/mybank \
  --namespace mybank

# 또는 특정 값만 변경
helm upgrade mybank ./helm/mybank \
  --namespace mybank \
  --set services.apiGateway.replicas=3
```

#### 롤백
```bash
# 이전 버전으로 롤백
helm rollback mybank 1 --namespace mybank

# 릴리스 히스토리 확인
helm history mybank --namespace mybank
```

#### 삭제
```bash
helm uninstall mybank --namespace mybank
```

### 2. Kustomize를 사용한 배포

#### Development 환경
```bash
kubectl apply -k kustomize/overlays/development
```

#### Staging 환경
```bash
kubectl apply -k kustomize/overlays/staging
```

#### Production 환경
```bash
kubectl apply -k kustomize/overlays/production
```

### 3. 기존 YAML 파일 직접 배포 (Legacy)

```bash
# Namespace
kubectl apply -f namespace.yaml

# ConfigMaps and Secrets
kubectl apply -f config/

# Infrastructure
kubectl apply -f infrastructure/

# Services
kubectl apply -f services/

# Frontend
kubectl apply -f frontend-deployment.yaml

# Istio (if enabled)
kubectl apply -f istio/
```

## 🔧 환경별 설정

### Development (개발 환경)
- Replica: 1
- Resources: 최소 (개발용)
- Persistence: hostPath (로컬)
- Logging: DEBUG
- Image Pull Policy: Always

### Staging (스테이징 환경)
- Replica: 2
- Resources: 중간
- Persistence: NFS or Cloud PV
- Logging: INFO
- Image Pull Policy: IfNotPresent

### Production (프로덕션 환경)
- Replica: 3+
- Resources: 최대 (고가용성)
- Persistence: Cloud PV with backup
- Logging: WARN
- Image Pull Policy: IfNotPresent
- Monitoring: Enabled
- Auto-scaling: Enabled

## 📊 Helm Values 주요 설정

### 글로벌 설정
```yaml
global:
  namespace: mybank
  environment: production
  imageRegistry: mybank
  imagePullPolicy: IfNotPresent
```

### 인프라 설정
```yaml
infrastructure:
  enabled: true
  postgres:
    replicas: 1
    persistence:
      size: 1Gi
  mongodb:
    replicas: 1
    persistence:
      size: 2Gi
  redis:
    replicas: 1
  kafka:
    replicas: 1
    persistence:
      size: 5Gi
```

### 서비스 설정
```yaml
services:
  enabled: true
  common:
    replicas: 2
    resources:
      requests:
        memory: "512Mi"
        cpu: "500m"
      limits:
        memory: "1Gi"
        cpu: "1000m"
```

## 🔐 Secrets 관리

### Secrets 생성
```bash
kubectl create secret generic common-secret \
  --from-literal=JWT_SECRET=your-jwt-secret \
  --from-literal=POSTGRES_PASSWORD=your-postgres-password \
  --from-literal=MONGODB_PASSWORD=your-mongodb-password \
  --namespace mybank
```

### Sealed Secrets (권장 - GitOps용)
```bash
# Sealed Secrets 설치
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Secret을 Sealed Secret으로 변환
kubectl create secret generic common-secret \
  --from-literal=JWT_SECRET=your-jwt-secret \
  --dry-run=client -o yaml | \
  kubeseal -o yaml > config/sealed-secret.yaml

# Sealed Secret 배포
kubectl apply -f config/sealed-secret.yaml
```

## 📈 모니터링 및 로깅

### Prometheus & Grafana (Helm)
```bash
# Prometheus 설치
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace

# Grafana 대시보드 접속
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80
# http://localhost:3000 (admin/prom-operator)
```

### Jaeger (Distributed Tracing)
```bash
kubectl apply -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/main/deploy/crds/jaegertracing.io_jaegers_crd.yaml
kubectl apply -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/main/deploy/operator.yaml
```

## 🔄 GitOps (ArgoCD)

### ArgoCD 설치
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# ArgoCD UI 접속
kubectl port-forward svc/argocd-server -n argocd 8080:443

# 초기 비밀번호 확인
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

### Application 등록
```bash
kubectl apply -f argocd/applications/mybank-app.yaml
```

## 🧪 테스트

### Helm Chart 검증
```bash
# Dry-run
helm install mybank ./helm/mybank --dry-run --debug --namespace mybank

# Template 렌더링
helm template mybank ./helm/mybank --namespace mybank

# Lint
helm lint ./helm/mybank
```

### Kustomize 검증
```bash
# Dry-run
kubectl apply -k kustomize/overlays/development --dry-run=client

# Build
kubectl kustomize kustomize/overlays/development
```

## 🐛 트러블슈팅

### Pod 상태 확인
```bash
kubectl get pods -n mybank
kubectl describe pod <pod-name> -n mybank
kubectl logs <pod-name> -n mybank -c <container-name>
```

### Service 연결 테스트
```bash
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://api-gateway.mybank.svc.cluster.local:8080/actuator/health
```

### DNS 문제 해결
```bash
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup api-gateway.mybank.svc.cluster.local
```

### Istio 문제
```bash
# Istio 주입 확인
kubectl get namespace -L istio-injection

# Sidecar 로그
kubectl logs <pod-name> -n mybank -c istio-proxy
```

## 📚 참고 자료

- [Helm 공식 문서](https://helm.sh/docs/)
- [Kustomize 가이드](https://kubectl.docs.kubernetes.io/references/kustomize/)
- [Istio 서비스 메시](https://istio.io/latest/docs/)
- [ArgoCD GitOps](https://argo-cd.readthedocs.io/)
- [Kubernetes 모범 사례](https://kubernetes.io/docs/concepts/configuration/overview/)

## 🤝 기여

배포 관련 이슈나 개선 사항은 이슈 트래커에 등록해주세요.
