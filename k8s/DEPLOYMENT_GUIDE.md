# MyBank Kubernetes 배포 가이드

## 📋 목차

1. [배포 구조 개요](#배포-구조-개요)
2. [빠른 시작](#빠른-시작)
3. [Helm 배포](#helm-배포)
4. [Kustomize 배포](#kustomize-배포)
5. [환경별 설정](#환경별-설정)
6. [트러블슈팅](#트러블슈팅)

## 배포 구조 개요

MyBank는 **Helm**과 **Kustomize** 두 가지 방법으로 배포할 수 있습니다.

### Helm Charts 구조
```
helm/mybank/
├── Chart.yaml                 # 메인 차트
├── values.yaml               # 기본 값
├── values-development.yaml   # 개발 환경
├── values-production.yaml    # 프로덕션 환경
└── charts/                   # 서브차트
    ├── infrastructure/       # DB, Kafka, Redis
    ├── services/            # 백엔드 서비스
    └── frontend/            # 프론트엔드
```

### Kustomize 구조
```
kustomize/
├── base/                    # 기본 설정
│   ├── kustomization.yaml
│   └── namespace.yaml
└── overlays/               # 환경별 오버레이
    ├── development/
    ├── staging/
    └── production/
```

## 빠른 시작

### 1. 사전 요구사항

```bash
# 필수 도구 설치 확인
kubectl version
helm version
kind version

# (선택) Istio
istioctl version
```

### 2. Kind 클러스터 생성

```bash
# 클러스터 생성
kind create cluster --name mybank-cluster --config ../kind-config.yaml

# 클러스터 확인
kubectl cluster-info --context kind-mybank-cluster
```

### 3. Docker 이미지 빌드 및 로드

```bash
cd k8s/scripts

# 모든 서비스 빌드
./build-and-load-images.sh --all

# 또는 특정 서비스만
./build-and-load-images.sh api-gateway
./build-and-load-images.sh auth-service
```

### 4. 배포

#### Helm 사용 (권장)
```bash
cd k8s/scripts

# 개발 환경 배포
./deploy-helm.sh development

# Istio 포함 배포
./deploy-helm.sh development --install-istio
```

#### Kustomize 사용
```bash
cd k8s/scripts

# 개발 환경 배포
./deploy-kustomize.sh development
```

### 5. 서비스 접속

```bash
# API Gateway
kubectl port-forward -n mybank svc/api-gateway 8080:8080

# Frontend
kubectl port-forward -n mybank svc/frontend 3000:3000

# 브라우저에서 접속
# API: http://localhost:8080
# Frontend: http://localhost:3000
```

## Helm 배포

### 기본 배포

```bash
helm install mybank ./helm/mybank \
  --namespace mybank \
  --create-namespace
```

### 환경별 배포

#### Development
```bash
helm install mybank ./helm/mybank \
  --namespace mybank \
  --values ./helm/mybank/values-development.yaml
```

#### Production
```bash
helm install mybank ./helm/mybank \
  --namespace mybank \
  --values ./helm/mybank/values-production.yaml
```

### 부분 배포

#### 인프라만
```bash
helm install mybank-infra ./helm/mybank \
  --namespace mybank \
  --set services.enabled=false \
  --set frontend.enabled=false
```

#### 서비스만 (인프라 제외)
```bash
helm install mybank-services ./helm/mybank \
  --namespace mybank \
  --set infrastructure.enabled=false
```

#### 특정 서비스만
```bash
helm install mybank ./helm/mybank \
  --namespace mybank \
  --set services.authService.enabled=true \
  --set services.pfmCoreService.enabled=false \
  --set services.paymentService.enabled=false
```

### 업그레이드

```bash
# 값 변경 후 업그레이드
helm upgrade mybank ./helm/mybank \
  --namespace mybank \
  --values ./helm/mybank/values.yaml

# 이미지 태그만 변경
helm upgrade mybank ./helm/mybank \
  --namespace mybank \
  --set services.apiGateway.image.tag=v1.1.0

# Replica 수 변경
helm upgrade mybank ./helm/mybank \
  --namespace mybank \
  --set services.common.replicas=3
```

### 롤백

```bash
# 이전 버전으로 롤백
helm rollback mybank --namespace mybank

# 특정 리비전으로 롤백
helm rollback mybank 2 --namespace mybank

# 히스토리 확인
helm history mybank --namespace mybank
```

### Dry Run 및 검증

```bash
# Dry run (실제 배포 없이 테스트)
helm install mybank ./helm/mybank \
  --namespace mybank \
  --dry-run --debug

# Template 렌더링
helm template mybank ./helm/mybank --namespace mybank

# Lint (문법 검사)
helm lint ./helm/mybank
```

## Kustomize 배포

### 기본 배포

```bash
# Development
kubectl apply -k kustomize/overlays/development

# Staging
kubectl apply -k kustomize/overlays/staging

# Production
kubectl apply -k kustomize/overlays/production
```

### Dry Run

```bash
# 변경사항 미리보기
kubectl apply -k kustomize/overlays/development --dry-run=client

# 생성될 매니페스트 확인
kubectl kustomize kustomize/overlays/development
```

### 업데이트

```bash
# 재배포
kubectl apply -k kustomize/overlays/development

# 특정 리소스만 재시작
kubectl rollout restart deployment api-gateway -n mybank
```

### 삭제

```bash
kubectl delete -k kustomize/overlays/development
```

## 환경별 설정

### Development (개발)

**특징:**
- Replica: 1
- 리소스 제한: 낮음
- 로깅: DEBUG
- 퍼시스턴스: hostPath
- 이미지 태그: dev

**배포:**
```bash
# Helm
./scripts/deploy-helm.sh development

# Kustomize
./scripts/deploy-kustomize.sh development
```

**Values 오버라이드:**
```yaml
services:
  common:
    replicas: 1
    resources:
      limits:
        memory: "512Mi"
        cpu: "500m"
```

### Staging (스테이징)

**특징:**
- Replica: 2
- 리소스 제한: 중간
- 로깅: INFO
- 퍼시스턴스: NFS/Cloud PV
- 이미지 태그: staging

**배포:**
```bash
# Helm
./scripts/deploy-helm.sh staging

# Kustomize
./scripts/deploy-kustomize.sh staging
```

### Production (프로덕션)

**특징:**
- Replica: 3+
- 리소스 제한: 높음
- 로깅: WARN
- 퍼시스턴스: Cloud PV (백업 포함)
- 이미지 태그: v1.0.0 (Semantic versioning)
- HPA: 활성화
- PDB: 활성화
- 모니터링: 활성화

**배포:**
```bash
# Helm
./scripts/deploy-helm.sh production

# Kustomize
./scripts/deploy-kustomize.sh production
```

**Production Values:**
```yaml
services:
  common:
    replicas: 3
    resources:
      requests:
        memory: "512Mi"
        cpu: "500m"
      limits:
        memory: "2Gi"
        cpu: "2000m"

  # HPA 설정
  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 10
    targetCPUUtilization: 70

  # PDB 설정
  podDisruptionBudget:
    enabled: true
    minAvailable: 1
```

## TLS 인증서 관리

### 인증서 구조

MyBank는 **단일 와일드카드 인증서**(`*.mybank.com`)를 사용하여 모든 서비스를 커버합니다.

```
certs/
├── ca.crt                  # 루트 CA 인증서 (시스템에 신뢰 추가 필요)
├── ca.key                  # 루트 CA 개인키
├── tls-mybank.crt          # 와일드카드 인증서 (*.mybank.com)
└── tls-mybank.key          # 와일드카드 개인키
```

### 인증서 생성

```bash
./scripts/generate-certs.sh
```

이 스크립트는:
1. CA 인증서 생성 (10년 유효)
2. 와일드카드 인증서 생성 (825일 유효)
3. Kubernetes Secrets 자동 생성:
   - `mybank-tls-cert` (istio-system namespace): Istio Gateway용
   - `mybank-tls-cert` (mybank namespace): 내부 서비스용
   - `mybank-ca-cert` (ConfigMap): CA 인증서

### 와일드카드 인증서 SAN (Subject Alternative Names)

`tls-mybank.crt`가 커버하는 도메인:
- `mybank.com`
- `*.mybank.com`
- `api.mybank.com`
- `app.mybank.com`
- `auth.mybank.com`, `user.mybank.com`, `pfm.mybank.com`
- `payment.mybank.com`, `investment.mybank.com`
- `eureka.mybank.com`, `grafana.mybank.com`
- `kafka-ui.mybank.com`, `prometheus.mybank.com`, `argocd.mybank.com`
- `localhost`, `127.0.0.1`

### 로컬 머신에서 인증서 신뢰

브라우저 "안전하지 않음" 경고를 제거하려면:

**macOS:**
```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  certs/ca.crt
```

**Linux:**
```bash
sudo cp certs/ca.crt /usr/local/share/ca-certificates/mybank-ca.crt
sudo update-ca-certificates
```

### /etc/hosts 설정

```bash
sudo ./scripts/setup-hosts.sh
```

또는 수동으로 `/etc/hosts`에 추가:
```
127.0.0.1 app.mybank.com api.mybank.com eureka.mybank.com
127.0.0.1 grafana.mybank.com kafka-ui.mybank.com prometheus.mybank.com argocd.mybank.com
```

### 인증서 확인

```bash
# 인증서 정보 보기
openssl x509 -in certs/tls-mybank.crt -text -noout

# SAN 확인
openssl x509 -in certs/tls-mybank.crt -text -noout | grep -A 10 "Subject Alternative Name"

# 유효기간 확인
openssl x509 -in certs/tls-mybank.crt -noout -dates

# Kubernetes Secret 확인
kubectl get secret mybank-tls-cert -n istio-system
kubectl get secret mybank-tls-cert -n mybank
```

### 인증서 재생성

```bash
# 기존 인증서 삭제
rm -rf certs/

# 새 인증서 생성 (Kubernetes Secrets 자동 업데이트)
./scripts/generate-certs.sh

# Istio Gateway 재시작 (인증서 변경 적용)
kubectl rollout restart deployment/istio-ingressgateway -n istio-system
```

## 트러블슈팅

### 0. 인증서 오류

**증상**: 브라우저에서 "연결이 비공개로 설정되지 않음" 또는 `NET::ERR_CERT_AUTHORITY_INVALID`

**원인**:
1. CA 인증서가 시스템에 신뢰되지 않음
2. `/etc/hosts`에 도메인이 설정되지 않음
3. 인증서 만료
4. Istio Gateway가 올바른 Secret을 찾지 못함

**해결방법**:

```bash
# 1. CA 인증서 신뢰 추가 (위 섹션 참고)

# 2. /etc/hosts 확인
grep mybank.com /etc/hosts

# 3. 인증서 유효기간 확인
openssl x509 -in certs/tls-mybank.crt -noout -dates

# 4. Istio Gateway Secret 확인
kubectl get secret mybank-tls-cert -n istio-system

# Secret이 없다면 재생성
./scripts/generate-certs.sh

# 5. Gateway 로그 확인
kubectl logs -n istio-system deployment/istio-ingressgateway | grep -i tls

# 6. Gateway 재시작
kubectl rollout restart deployment/istio-ingressgateway -n istio-system

# 7. 브라우저 재시작 후 재시도
```

### 1. Pod가 시작되지 않음

```bash
# Pod 상태 확인
kubectl get pods -n mybank

# Pod 상세 정보
kubectl describe pod <pod-name> -n mybank

# 로그 확인
kubectl logs <pod-name> -n mybank

# 이전 컨테이너 로그
kubectl logs <pod-name> -n mybank --previous
```

### 2. 이미지 Pull 실패

```bash
# ImagePullPolicy 확인
kubectl get deployment <deployment-name> -n mybank -o yaml | grep imagePullPolicy

# Kind에 이미지 로드
kind load docker-image mybank/api-gateway:latest --name mybank-cluster
```

### 3. 서비스 연결 실패

```bash
# Service 확인
kubectl get svc -n mybank

# Endpoints 확인
kubectl get endpoints <service-name> -n mybank

# DNS 테스트
kubectl run -it --rm debug --image=busybox --restart=Never -- \
  nslookup api-gateway.mybank.svc.cluster.local

# 연결 테스트
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://api-gateway.mybank.svc.cluster.local:8080/actuator/health
```

### 4. 데이터베이스 연결 실패

```bash
# PostgreSQL 로그
kubectl logs deployment/postgres-auth -n mybank

# MongoDB 로그
kubectl logs deployment/mongodb -n mybank

# 연결 테스트
kubectl exec -it deployment/postgres-auth -n mybank -- \
  psql -U mybank -d mybank_auth -c "\dt"
```

### 5. Helm 문제

```bash
# 릴리스 상태 확인
helm status mybank -n mybank

# 값 확인
helm get values mybank -n mybank

# 매니페스트 확인
helm get manifest mybank -n mybank

# 릴리스 삭제 후 재설치
helm uninstall mybank -n mybank
helm install mybank ./helm/mybank -n mybank
```

### 6. Istio 문제

```bash
# Istio 주입 확인
kubectl get namespace mybank -o yaml | grep istio-injection

# Sidecar 로그
kubectl logs <pod-name> -n mybank -c istio-proxy

# Istio 재주입
kubectl label namespace mybank istio-injection=enabled --overwrite
kubectl rollout restart deployment -n mybank
```

## 유용한 명령어

### 모니터링

```bash
# 모든 Pod 상태 실시간 모니터링
kubectl get pods -n mybank -w

# 리소스 사용량
kubectl top pods -n mybank
kubectl top nodes

# 이벤트 확인
kubectl get events -n mybank --sort-by='.lastTimestamp'
```

### 스케일링

```bash
# Manual scaling
kubectl scale deployment api-gateway --replicas=5 -n mybank

# HPA 확인
kubectl get hpa -n mybank

# HPA 상세
kubectl describe hpa api-gateway-hpa -n mybank
```

### 로그

```bash
# 여러 Pod 로그 동시 확인
kubectl logs -f -l app=api-gateway -n mybank --all-containers

# 특정 시간 이후 로그
kubectl logs <pod-name> -n mybank --since=1h

# 마지막 N줄만
kubectl logs <pod-name> -n mybank --tail=100
```

### 정리

```bash
# 스크립트 사용
./scripts/cleanup.sh

# Namespace만 삭제
./scripts/cleanup.sh --keep-namespace

# 클러스터까지 삭제
./scripts/cleanup.sh --delete-cluster
```

## 참고 자료

- [Helm 공식 문서](https://helm.sh/docs/)
- [Kustomize 가이드](https://kubectl.docs.kubernetes.io/references/kustomize/)
- [Kind 문서](https://kind.sigs.k8s.io/)
- [Istio 서비스 메시](https://istio.io/latest/docs/)
