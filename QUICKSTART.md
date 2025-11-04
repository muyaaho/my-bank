# MyBank - Quick Start Guide

## 🚀 한번에 전체 시스템 배포하기

### Prerequisites 확인
```bash
# Docker 확인
docker --version

# Kind 확인
kind --version

# kubectl 확인
kubectl version --client

# Java 확인
java --version

# Node.js 확인
node --version
```

### 1. 전체 시스템 배포 (자동)

```bash
# 프로젝트 루트에서 실행
cd /Users/kimhyeonwoo/Documents/GitHub/my-bank

# 전체 배포 스크립트 실행
./scripts/deploy-complete-system.sh
```

이 스크립트는 자동으로:
1. Kind 클러스터 생성
2. 모든 Docker 이미지 빌드 (백엔드 + 프론트엔드)
3. TLS 인증서 생성
4. /etc/hosts 설정
5. NGINX Ingress 설치
6. 인프라 배포 (PostgreSQL, MongoDB, Redis, Kafka)
7. 마이크로서비스 배포
8. 프론트엔드 배포
9. ArgoCD 설치

### 2. 수동 배포 (단계별)

#### Step 1: 클러스터 생성
```bash
kind create cluster --config kind-config.yaml
```

#### Step 2: 백엔드 빌드
```bash
./gradlew clean build -x test --no-daemon
```

#### Step 3: Docker 이미지 빌드
```bash
./build-images.sh
```

#### Step 4: 이미지를 Kind에 로드
```bash
kind load docker-image mybank/service-discovery:latest --name mybank-cluster
kind load docker-image mybank/api-gateway:latest --name mybank-cluster
kind load docker-image mybank/auth-service:latest --name mybank-cluster
kind load docker-image mybank/pfm-core-service:latest --name mybank-cluster
kind load docker-image mybank/payment-service:latest --name mybank-cluster
kind load docker-image mybank/investment-service:latest --name mybank-cluster
kind load docker-image mybank/frontend:latest --name mybank-cluster
```

#### Step 5: TLS 인증서 생성
```bash
./scripts/generate-certs.sh
```

#### Step 6: /etc/hosts 설정
```bash
./scripts/setup-hosts.sh
```

#### Step 7: NGINX Ingress 설치
```bash
kubectl apply -f k8s/ingress/ingress-nginx-setup.yaml

# Ingress 준비 대기
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s
```

#### Step 8: Namespace 생성
```bash
kubectl create namespace mybank
```

#### Step 9: ConfigMaps & Secrets 적용
```bash
kubectl apply -f k8s/config/
```

#### Step 10: 인프라 배포
```bash
kubectl apply -f k8s/infrastructure/

# 인프라 준비 대기
sleep 20
kubectl wait --for=condition=ready pod -l app=postgres -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=mongodb -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=redis -n mybank --timeout=180s
kubectl wait --for=condition=ready pod -l app=kafka -n mybank --timeout=180s
```

#### Step 11: 서비스 배포
```bash
# Service Discovery 먼저
kubectl apply -f k8s/services/service-discovery.yaml
sleep 15
kubectl wait --for=condition=ready pod -l app=service-discovery -n mybank --timeout=180s

# API Gateway
kubectl apply -f k8s/services/api-gateway.yaml
sleep 10

# 비즈니스 서비스들
kubectl apply -f k8s/services/auth-service.yaml
kubectl apply -f k8s/services/pfm-core-service.yaml
kubectl apply -f k8s/services/payment-service.yaml
kubectl apply -f k8s/services/investment-service.yaml
```

#### Step 12: 프론트엔드 배포
```bash
kubectl apply -f k8s/frontend-deployment.yaml
```

#### Step 13: Ingress 규칙 적용
```bash
kubectl apply -f k8s/ingress/mybank-ingress.yaml
```

### 3. 배포 확인

```bash
# 모든 Pod 확인
kubectl get pods -n mybank

# 서비스 확인
kubectl get svc -n mybank

# Ingress 확인
kubectl get ingress -n mybank

# 특정 서비스 로그 확인
kubectl logs -f deployment/frontend -n mybank
```

## 🌐 접속하기

### 프론트엔드
**URL**: https://app.mybank.com

### API Gateway
**URL**: https://api.mybank.com

### 기타 대시보드
- **Eureka**: https://eureka.mybank.com
- **Grafana**: https://grafana.mybank.com (admin/admin)
- **Kafka UI**: https://kafka-ui.mybank.com
- **Prometheus**: https://prometheus.mybank.com
- **ArgoCD**: https://argocd.mybank.com

## 🔐 인증서 신뢰 설정

브라우저에서 자체 서명 인증서 경고가 표시되면:

### macOS
```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain certs/ca.crt
```

### Linux
```bash
sudo cp certs/ca.crt /usr/local/share/ca-certificates/mybank-ca.crt
sudo update-ca-certificates
```

### 브라우저
1. https://app.mybank.com 접속
2. "고급" 클릭
3. "안전하지 않음(계속)" 클릭

## 📝 테스트 계정

### 회원가입
1. https://app.mybank.com/register 접속
2. 정보 입력:
   - Name: Test User
   - Email: test@mybank.com
   - Phone: 01012345678
   - Password: test1234

### 로그인
1. https://app.mybank.com/login 접속
2. 위에서 생성한 계정으로 로그인

## 🔍 트러블슈팅

### Pod가 시작하지 않는 경우
```bash
# Pod 상태 확인
kubectl get pods -n mybank

# 특정 Pod 로그 확인
kubectl logs <pod-name> -n mybank

# Pod 상세 정보
kubectl describe pod <pod-name> -n mybank
```

### Ingress가 동작하지 않는 경우
```bash
# Ingress Controller 확인
kubectl get pods -n ingress-nginx

# Ingress Controller 로그
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller

# /etc/hosts 확인
cat /etc/hosts | grep mybank
```

### 프론트엔드가 API와 통신하지 못하는 경우
```bash
# API Gateway Pod 확인
kubectl get pods -n mybank -l app=api-gateway

# API Gateway 로그
kubectl logs -f deployment/api-gateway -n mybank

# 프론트엔드 환경변수 확인
kubectl get deployment frontend -n mybank -o yaml | grep NEXT_PUBLIC
```

### 이미지를 찾을 수 없는 경우
```bash
# Kind 클러스터에 이미지 다시 로드
kind load docker-image mybank/frontend:latest --name mybank-cluster

# Pod 재시작
kubectl rollout restart deployment/frontend -n mybank
```

## 🧹 클린업

### 전체 삭제
```bash
# Kind 클러스터 삭제
kind delete cluster --name mybank-cluster

# Docker 이미지 삭제
docker rmi $(docker images 'mybank/*' -q)

# 인증서 삭제
rm -rf certs/
```

### /etc/hosts 엔트리 제거 (수동)
```bash
sudo vi /etc/hosts
# mybank.com 관련 라인 삭제
```

## 📚 추가 문서

- [PRODUCTION_DEPLOYMENT.md](./PRODUCTION_DEPLOYMENT.md) - 상세 배포 가이드
- [FRONTEND_DEPLOYMENT.md](./FRONTEND_DEPLOYMENT.md) - 프론트엔드 배포 가이드
- [app/README.md](./app/README.md) - 프론트엔드 개발 가이드
- [DEPLOYMENT.md](./DEPLOYMENT.md) - 인프라 배포 가이드
