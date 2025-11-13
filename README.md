# MyBank - 현대적인 핀테크 플랫폼

**Spring Boot 3**, **Spring Cloud**, **Kafka**, **MongoDB**, **Next.js 14**로 구축된 클라우드 네이티브 마이크로서비스 기반 핀테크 플랫폼입니다. 높은 확장성과 성능을 위해 **MSA (Microservices Architecture)**, **EDA (Event-Driven Architecture)**, **DDD (Domain-Driven Design)** 패턴을 구현했습니다.

> 💡 **빠른 시작**: 한 번의 명령으로 전체 시스템을 배포하려면 [`./deploy-mybank.sh`](#kubernetes-kind---통합-배포-권장)를 실행하세요.

## 아키텍처 개요

```
┌──────────────┐
│   Browser    │
│  (Frontend)  │
└──────┬───────┘
       │ https://app.mybank.com
       │
┌──────▼───────────────────────────────────────────────────────┐
│                    Istio Service Mesh                         │
│                  (Service Discovery, mTLS)                    │
└────────────┬─────────────────────────────────────────────────┘
             │
┌────────────▼─────────────────────────────────────────────────┐
│                   API Gateway (8080)                          │
│         JWT Authentication & Request Routing                  │
└────────────┬─────────────────────────────────────────────────┘
             │
    ┌────────┴────────┬──────────┬──────────┬──────────┐
    │                 │          │          │          │
┌───▼────┐  ┌────▼─────┐  ┌──▼───┐  ┌──▼───┐  ┌──▼───┐
│  Auth  │  │   PFM    │  │Payment│  │Invest│  │ User │
│Service │  │ Service  │  │Service│  │Service│  │Service│
│        │  │          │  │       │  │      │  │      │
│:8081   │  │  :8082   │  │ :8083 │  │:8084 │  │:8085 │
└────────┘  └────┬─────┘  └───┬───┘  └──┬───┘  └──────┘
                 │            │         │
                 └────────┬───┴─────────┘
                          │
                    ┌─────▼─────┐
                    │   Kafka   │
                    │  Events   │
                    └───────────┘
```

## 핵심 기능

### 1. 개인 자산 관리 (PFM)
- **통합 자산 조회**: 은행, 카드, 증권, 보험 계좌 통합
- **AI 기반 지출 분석**: 자동 거래 분류 및 이상 탐지
- **Redis 캐싱**: Cache-Aside 패턴을 통한 실시간 잔액 조회

### 2. 투자 서비스
- **소액 투자**: 1,000원부터 주식/ETF 투자 가능
- **자동 거스름돈 투자**: 결제 시 남은 거스름돈 자동 투자
- **이벤트 기반**: Kafka를 통한 결제 이벤트 수신으로 자동 투자 실행

### 3. 송금 및 결제
- **간편 송금**: FIDO2 생체 인증
- **구독 관리**: 통신비, OTT, 공과금 자동 납부
- **이벤트 발행**: Kafka로 결제 이벤트 발행

### 4. 인증 및 보안
- **OAuth 2.0 & JWT**: Stateless 인증 (Token Blacklist 패턴)
  - Netflix, Uber, Spotify 등에서 사용하는 프로덕션 표준 패턴
  - JWT는 사용자 정보를 포함하여 완전히 stateless
  - Redis Blacklist는 로그아웃된 토큰만 저장 (최소 메모리)
  - 요청당 1회 Redis 조회 (기존 세션 방식 대비 10배 성능 향상)
- **계정 잠금**: 무차별 대입 공격 방지
- **토큰 해싱**: SHA-256 해시로 토큰 노출 방지

## 기술 스택

| 분류 | 기술 | 용도 |
|------|------|------|
| **아키텍처** | MSA, EDA | 서비스 독립성, 비동기 통신 |
| **서비스 메시** | Istio 1.27.3 | 서비스 디스커버리, mTLS, 트래픽 관리 |
| **프론트엔드** | Next.js 14, React, TypeScript | 현대적인 웹 애플리케이션 |
| **백엔드** | Spring Boot 3.2, Spring Cloud 2023 | 마이크로서비스 프레임워크 |
| **서비스 디스커버리** | Eureka (로컬), Istio (K8s) | 서비스 레지스트리 |
| **API Gateway** | Spring Cloud Gateway | 요청 라우팅, JWT 검증 |
| **주요 DB** | MongoDB, PostgreSQL | 유연한 스키마(PFM), ACID 트랜잭션(Auth) |
| **캐시** | Redis | Token Blacklist, 실시간 데이터 |
| **메시징** | Apache Kafka (KRaft) | 이벤트 스트리밍, EDA 구현 |
| **모니터링** | Prometheus, Grafana | 메트릭 수집, 시각화 |
| **컨테이너** | Docker, Docker Compose | 로컬 개발 환경 |
| **오케스트레이션** | Kubernetes (Kind) | 프로덕션 배포 |

## 프로젝트 구조

```
mybank/
├── app/                       # 프론트엔드 (Next.js)
│   ├── app/                  # Next.js App Router
│   ├── components/           # React 컴포넌트
│   ├── lib/                  # API 클라이언트, 유틸리티
│   ├── stores/               # 상태 관리 (Zustand)
│   └── types/                # TypeScript 타입
├── api-gateway/              # API Gateway (Port 8080)
├── config-server/            # Config Server (Port 8888)
├── service-discovery/        # Eureka Server (Port 8761)
├── auth-service/             # 인증 서비스 (Port 8081)
├── user-service/             # 사용자 프로필 서비스 (Port 8085)
├── pfm-core-service/         # 자산 관리 서비스 (Port 8082)
├── payment-service/          # 송금 서비스 (Port 8083)
├── investment-service/       # 투자 서비스 (Port 8084)
├── common-lib/               # 공통 라이브러리, DTOs, Events
├── k8s/                      # Kubernetes 매니페스트
│   ├── services/            # Service deployments
│   ├── config/              # ConfigMaps
│   └── istio/               # Istio Gateway & VirtualServices
├── scripts/                  # 배포 스크립트
│   ├── deploy-complete-system.sh  # 전체 시스템 배포
│   ├── generate-certs.sh          # TLS 인증서 생성
│   └── setup-hosts.sh             # /etc/hosts 설정
└── docker-compose.yml        # 로컬 개발 환경
```

## 📚 개발자 문서

상세한 개발 가이드, 아키텍처 패턴, 자주 사용하는 명령어, 트러블슈팅 가이드는 **[CLAUDE.md](./CLAUDE.md)**를 참고하세요.

**주요 내용:**
- Event-Driven Architecture 패턴 및 Kafka 사용법
- JWT 인증 플로우 및 Token Blacklist 패턴
- Redis 캐싱 및 분산 락 패턴
- 프론트엔드 API 통합 및 상태 관리
- 개발 워크플로우 및 배포 전략
- 자주 발생하는 문제 해결 방법

## 🚀 시작하기

### 사전 요구사항

- **Java 17+** (권장: Java 21)
- **Docker** 및 **Docker Compose**
- **Gradle 8.x**
- **Node.js 20+** (프론트엔드 개발용)
- **Kind** (Kubernetes 배포용, 선택사항)
- **kubectl** (Kubernetes CLI, Kind 사용 시)

### 개발 환경 선택

MyBank는 3가지 개발 환경을 지원합니다:

1. **Docker Compose** (가장 간단) - 로컬 개발 및 빠른 테스트
2. **Kubernetes (Kind)** (권장) - 프로덕션 환경과 유사한 테스트
3. **로컬 실행** - 개별 서비스 개발 및 디버깅

> ⚠️ **중요**: 코드 변경 후에는 **반드시 Kind 클러스터에 배포**하여 Kubernetes 환경에서 정상 작동을 확인하세요.

### 1. 인프라 서비스 시작

```bash
# 모든 인프라 서비스 시작
docker-compose up -d

# 서비스 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f kafka
```

**시작된 서비스:**
- PostgreSQL: `localhost:5432`
- MongoDB: `localhost:27017`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8090`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001` (admin/admin)

### 2. 프로젝트 빌드

```bash
# 모든 모듈 빌드
./gradlew clean build

# 테스트 스킵하여 빠른 빌드
./gradlew clean build -x test
```

### 3. 마이크로서비스 시작

**서비스를 순서대로 시작:**

```bash
# 1. Service Discovery (시작 대기)
./gradlew :service-discovery:bootRun

# 2. Config Server (Eureka 등록 대기)
./gradlew :config-server:bootRun

# 3. API Gateway (Eureka 등록 대기)
./gradlew :api-gateway:bootRun

# 4. 비즈니스 서비스들 (병렬 시작 가능)
./gradlew :auth-service:bootRun
./gradlew :pfm-core-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :investment-service:bootRun
```

### 4. 서비스 확인

```bash
# Eureka 대시보드 확인
open http://localhost:8761

# API Gateway 상태 확인
curl http://localhost:8080/actuator/health

# 각 서비스 상태 확인
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

## API 사용 예제

### 1. 회원가입

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@mybank.com",
    "password": "MyBank123!",
    "name": "홍길동",
    "phoneNumber": "010-1234-5678"
  }'
```

### 2. 로그인

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@mybank.com",
    "password": "MyBank123!"
  }'
```

### 3. 자산 조회 (JWT 포함)

```bash
curl http://localhost:8080/api/v1/pfm/assets \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. 송금

```bash
curl -X POST http://localhost:8080/api/v1/payment/transfer \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "account-123",
    "toAccountId": "account-456",
    "recipientName": "김철수",
    "amount": 50000,
    "description": "저녁 식사비"
  }'
```

### 5. 투자 요약 조회

```bash
curl http://localhost:8080/api/v1/invest/summary \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 이벤트 플로우: 거스름돈 자동 투자

```
1. 사용자가 3,450원 결제
   ↓
2. Payment Service에서 거래 완료 처리
   ↓
3. Kafka 토픽 "payment-completed"에 "PaymentCompletedEvent" 발행
   ↓
4. Investment Service에서 이벤트 수신
   ↓
5. 거스름돈 계산: 4,000 - 3,450 = 550원
   ↓
6. 550원 자동 투자 처리
   ↓
7. 투자 잔액 및 통계 업데이트
```

## 모니터링

### Prometheus 메트릭
`http://localhost:9090` 방문

### Grafana 대시보드
1. `http://localhost:3001` 접속 (admin/admin)
2. Prometheus 데이터 소스 추가: `http://prometheus:9090`
3. 대시보드 ID 가져오기: 4701 (JVM Micrometer)

### Kafka UI
`http://localhost:8090`에서 토픽 모니터링

## 프론트엔드 애플리케이션

### 기능
- 🔐 **인증**: JWT 기반 로그인/회원가입
- 📊 **대시보드**: 자산 요약 및 차트
- 💰 **지출 분석**: 카테고리별 분류 및 이상 탐지
- 📈 **투자**: 거스름돈 투자 및 포트폴리오 추적
- 💳 **송금**: 계좌 이체 및 거래 내역

### 빠른 시작

#### 옵션 1: Docker Compose (권장)

```bash
# 프론트엔드 포함 모든 서비스 시작
docker-compose up -d

# 프론트엔드 접속
open http://localhost:3000
```

#### 옵션 2: 로컬 개발

```bash
# 필요시 npm 캐시 권한 수정
sudo chown -R $(whoami) ~/.npm

# 의존성 설치
cd app
npm install

# 개발 서버 실행
npm run dev

# 프론트엔드 접속
open http://localhost:3000
```

#### 옵션 3: Kubernetes (Kind)

```bash
# 전체 시스템 자동 배포 (Istio 포함)
./scripts/deploy-complete-system.sh

# HTTPS를 통한 프론트엔드 접속 (Istio Gateway)
open https://app.mybank.com

# 또는 NodePort를 통한 접속
open http://localhost:30000
```

### 프론트엔드 기술 스택

- **Next.js 14**: App Router를 사용하는 React 프레임워크
- **TypeScript**: 타입 안전 개발
- **Tailwind CSS**: 유틸리티 우선 스타일링
- **React Query**: 서버 상태 관리
- **Zustand**: 클라이언트 상태 관리
- **Recharts**: 데이터 시각화

상세한 배포 가이드는 [FRONTEND_DEPLOYMENT.md](./FRONTEND_DEPLOYMENT.md)를 참고하세요.

## 🚢 배포

### Kubernetes (Kind) - 통합 배포 (✅ 권장)

**한 번의 명령으로 전체 시스템을 배포하세요:**

```bash
# 🚀 모든 것을 자동으로 설치하고 배포
./deploy-mybank.sh
```

**이 스크립트가 자동으로 수행하는 작업:**

1. ✅ Kind 클러스터 생성 (포트 매핑: 80, 443, 30000-30002)
2. ✅ Gradle 빌드 및 Docker 이미지 빌드
3. ✅ Kind로 이미지 로드
4. ✅ /etc/hosts 도메인 자동 설정 (`*.mybank.com`)
5. ✅ 자체 서명 TLS 인증서 생성 (CA + 와일드카드 인증서)
6. ✅ Istio Service Mesh 설치 (버전 1.27.3)
7. ✅ Kubernetes Namespace 생성 및 TLS 시크릿 적용
8. ✅ 인프라 서비스 배포 (PostgreSQL, MongoDB, Redis, Kafka)
9. ✅ 마이크로서비스 순차 배포 (Service Discovery → Gateway → 비즈니스 서비스)
10. ✅ Istio Gateway 및 VirtualService 설정

**배포 후 서비스 접속:**

```
프론트엔드:    https://app.mybank.com (또는 http://localhost:30000)
API Gateway:  https://api.mybank.com
Eureka:       https://eureka.mybank.com
```

**개별 서비스 업데이트:**

```bash
# 1. 서비스 빌드
./gradlew :auth-service:build -x test

# 2. Docker 이미지 빌드
docker build -t mybank/auth-service:latest -f auth-service/Dockerfile .

# 3. Kind로 이미지 로드
kind load docker-image mybank/auth-service:latest --name mybank-cluster

# 4. 배포 재시작
kubectl rollout restart deployment/auth-service -n mybank

# 5. 롤아웃 상태 확인
kubectl rollout status deployment/auth-service -n mybank
```

**유용한 Kubernetes 명령어:**

```bash
# Pod 상태 확인
kubectl get pods -n mybank

# 실시간 로그 확인
kubectl logs -f deployment/auth-service -n mybank

# Istio Gateway 확인
kubectl get gateway -n mybank
kubectl get virtualservice -n mybank

# 서비스 엔드포인트 확인
kubectl get svc -n mybank

# 클러스터 삭제
kind delete cluster --name mybank-cluster
```

### Docker Compose (간단한 로컬 개발용)

```bash
# 모든 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f [service-name]

# 서비스 중지
docker-compose down

# 볼륨까지 삭제
docker-compose down -v
```

### Kubernetes (Kind) - 단계별 배포

통합 스크립트 대신 단계별로 배포하려면:

```bash
# 1. 인증서 생성
./scripts/generate-certs.sh

# 2. 도메인 설정
./scripts/setup-hosts.sh

# 3. 전체 시스템 배포
./scripts/deploy-complete-system.sh
```

## 🧪 플랫폼 테스트

### 1. 계정 생성

배포 환경에 따라 접속:
- **Docker Compose**: http://localhost:3000
- **Kind (NodePort)**: http://localhost:30000
- **Kind (Istio)**: https://app.mybank.com

회원가입 정보 예시:
```
Email: test@mybank.com
Password: MyBank123!
Name: 홍길동
Phone: 010-1234-5678
```

### 2. 기능 탐색

로그인 후 다음 기능 확인:
- **Dashboard**: 자산 요약 및 카테고리별 지출 분류
- **지출 분석**: 월별/카테고리별 지출 패턴 분석
- **투자**: 투자 포트폴리오 및 거스름돈 자동 투자 추적
- **송금**: 계좌 간 송금 및 거래 내역

### 3. 거스름돈 투자 테스트 (Event-Driven Architecture 검증)

```
1. 송금 페이지로 이동
2. 송금 실행 (예: 3,450원)
3. Payment Service가 "payment-completed" 이벤트 발행 (Kafka)
4. Investment Service가 이벤트 수신
5. 거스름돈 계산: 4,000 - 3,450 = 550원
6. 550원 자동 투자 처리
7. 투자 페이지에서 거스름돈 투자 내역 확인
```

**Kafka 이벤트 모니터링:**
```bash
# Kafka UI 접속
open http://localhost:8090

# "payment-completed" 토픽에서 이벤트 확인
# Investment Service 로그 확인
kubectl logs -f deployment/investment-service -n mybank | grep "round-up"
```

## 📖 API 문서

### 인증 엔드포인트 (`auth-service`)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|----------|
| POST | `/api/v1/auth/register` | 회원가입 | ❌ |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) | ❌ |
| POST | `/api/v1/auth/logout` | 로그아웃 (토큰 블랙리스트 추가) | ✅ |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 | ✅ |

### PFM 엔드포인트 (`pfm-core-service`)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|----------|
| GET | `/api/v1/pfm/assets` | 자산 요약 조회 (Redis 캐싱) | ✅ |
| GET | `/api/v1/pfm/spending/analysis?daysBack=30` | 지출 분석 조회 | ✅ |
| POST | `/api/v1/pfm/sync` | 자산 동기화 (캐시 갱신) | ✅ |

### 송금 엔드포인트 (`payment-service`)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|----------|
| POST | `/api/v1/payment/transfer` | 송금 실행 (Kafka 이벤트 발행) | ✅ |
| GET | `/api/v1/payment/{paymentId}` | 송금 상세 조회 | ✅ |
| GET | `/api/v1/payment/history` | 송금 내역 조회 | ✅ |

### 투자 엔드포인트 (`investment-service`)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|----------|
| GET | `/api/v1/invest/summary` | 투자 요약 조회 | ✅ |
| GET | `/api/v1/invest/roundup/history` | 거스름돈 투자 내역 | ✅ |
| POST | `/api/v1/invest/roundup/enable` | 거스름돈 투자 활성화 | ✅ |

### 사용자 엔드포인트 (`user-service`)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|----------|
| GET | `/api/v1/user/profile` | 프로필 조회 | ✅ |
| PUT | `/api/v1/user/profile` | 프로필 수정 | ✅ |

## 🌐 Istio Service Mesh

Kubernetes 배포 시 Istio Service Mesh가 자동으로 설치되어 다음 기능을 제공합니다:

### 주요 기능

| 기능 | 설명 | 장점 |
|------|------|------|
| **서비스 디스커버리** | Eureka 대신 Istio의 자동 서비스 등록 | 설정 간소화, 자동 헬스체크 |
| **트래픽 관리** | 로드 밸런싱, 재시도, 타임아웃, Circuit Breaker | 장애 격리, 높은 가용성 |
| **보안** | 서비스 간 mTLS 암호화 | 네트워크 계층 보안 |
| **관찰성** | Jaeger를 통한 분산 추적 | 요청 추적, 성능 분석 |

### Istio 리소스 확인

```bash
# Istio Gateway 확인
kubectl get gateway -n mybank

# Virtual Services 확인 (라우팅 규칙)
kubectl get virtualservice -n mybank

# TLS 인증서 확인
kubectl get secret mybank-tls-cert -n mybank

# Istio Proxy 상태 확인
istioctl proxy-status
```

### 도메인 및 TLS 설정

```bash
# /etc/hosts 자동 설정 (deploy-mybank.sh에서 자동 실행)
./scripts/setup-hosts.sh

# 설정되는 도메인:
# 127.0.0.1 app.mybank.com      (프론트엔드)
# 127.0.0.1 api.mybank.com      (API Gateway)
# 127.0.0.1 eureka.mybank.com   (Service Discovery)
```

### Gateway 및 VirtualService 구조

```yaml
# Istio Gateway - TLS 종료 지점
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: mybank-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: mybank-tls-cert
    hosts:
    - "*.mybank.com"

# VirtualService - 라우팅 규칙
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: frontend
spec:
  hosts:
  - "app.mybank.com"
  gateways:
  - mybank-gateway
  http:
  - route:
    - destination:
        host: frontend
        port:
          number: 3000
```

## 📊 모니터링 및 관찰성

MyBank는 다양한 모니터링 도구를 통해 시스템 상태를 실시간으로 확인할 수 있습니다:

| 도구 | URL | 용도 | 인증 정보 |
|------|-----|------|----------|
| **Prometheus** | http://localhost:9090 | 메트릭 수집 및 쿼리 | - |
| **Grafana** | http://localhost:3001 | 대시보드 시각화 | admin/admin |
| **Kafka UI** | http://localhost:8090 | Kafka 토픽 모니터링 | - |
| **Eureka Dashboard** | http://localhost:8761 | 서비스 레지스트리 | - |
| **Jaeger** | (Istio 설치 시) | 분산 추적 | - |

### Grafana 대시보드 설정

```bash
# 1. Grafana 접속
open http://localhost:3001

# 2. Prometheus 데이터 소스 추가
# Configuration → Data Sources → Add data source
# URL: http://prometheus:9090

# 3. 대시보드 Import
# Dashboard ID: 4701 (JVM Micrometer)
# Dashboard ID: 11378 (Spring Boot 2.1 Statistics)
```

### Actuator 엔드포인트

각 서비스는 Spring Boot Actuator를 통해 헬스체크 및 메트릭을 제공합니다:

```bash
# Health check
curl http://localhost:8081/actuator/health

# Metrics (Prometheus 형식)
curl http://localhost:8081/actuator/prometheus

# 전체 Actuator 엔드포인트 목록
curl http://localhost:8081/actuator
```

## 🔧 개발 워크플로우

### 표준 개발 워크플로우

1. **로컬 개발** (선택사항, 빠른 테스트용)
   ```bash
   ./gradlew :auth-service:bootRun
   ```

2. **빌드 및 테스트**
   ```bash
   ./gradlew clean build
   ```

3. **Kind에 배포** (필수)
   ```bash
   ./gradlew :auth-service:build -x test
   docker build -t mybank/auth-service:latest -f auth-service/Dockerfile .
   kind load docker-image mybank/auth-service:latest --name mybank-cluster
   kubectl rollout restart deployment/auth-service -n mybank
   ```

4. **Kind에서 검증**
   ```bash
   kubectl get pods -n mybank
   kubectl logs -f deployment/auth-service -n mybank
   ```

> ⚠️ **중요**: 개발 작업 완료 후 반드시 Kind 클러스터에 배포하여 Kubernetes 환경에서 정상 작동을 확인하세요.

## 🐛 자주 발생하는 문제 해결

### 서비스가 시작되지 않음

```bash
# 1. 필수 서비스 확인 (Eureka, 데이터베이스)
docker-compose ps

# 2. 포트 충돌 확인
lsof -i :8080

# 3. application.yml 연결 문자열 확인
```

### Kafka 연결 실패

```bash
# 1. Kafka 상태 확인
docker-compose ps kafka

# 2. bootstrap-servers 설정 확인
# 로컬: localhost:9092
# Docker 네트워크: kafka:9093

# 3. Kafka UI에서 토픽 확인
open http://localhost:8090
```

### 프론트엔드가 백엔드에 연결할 수 없음

```bash
# 1. API Gateway 상태 확인
curl http://localhost:8080/actuator/health

# 2. NEXT_PUBLIC_API_URL 환경 변수 확인
echo $NEXT_PUBLIC_API_URL

# 3. JWT 토큰 확인 (브라우저 DevTools → Application → Local Storage)
```

### 데이터베이스 연결 문제

```bash
# PostgreSQL (auth)
docker exec -it mybank-postgres psql -U mybank -d mybank

# PostgreSQL (user)
docker exec -it mybank-postgres-user psql -U mybank_user -d mybank_user

# MongoDB
docker exec -it mybank-mongodb mongosh -u root -p rootpassword

# Redis
docker exec -it mybank-redis redis-cli
```

## 📚 추가 문서

- **[CLAUDE.md](./CLAUDE.md)** - 완전한 개발자 가이드
  - 아키텍처 패턴 상세 설명
  - Kafka 이벤트 구조 및 사용법
  - JWT 인증 플로우 상세
  - 테스트 전략
  - 트러블슈팅 가이드
- **[app/README.md](./app/README.md)** - 프론트엔드 개발 가이드

## 🏗️ 기술적 하이라이트

- ✅ **Microservices Architecture (MSA)**: 독립적인 서비스 배포 및 확장
- ✅ **Event-Driven Architecture (EDA)**: Kafka를 통한 비동기 이벤트 처리
- ✅ **Domain-Driven Design (DDD)**: 투자 서비스에 적용된 도메인 모델링
- ✅ **JWT Token Blacklist Pattern**: 프로덕션 표준 인증 패턴 (10배 성능 향상)
- ✅ **Redis Cache-Aside Pattern**: 자산 조회 성능 최적화
- ✅ **Distributed Locking**: 송금 중복 방지를 위한 Redis 분산 락
- ✅ **Istio Service Mesh**: mTLS, Circuit Breaker, 분산 추적
- ✅ **KRaft Kafka**: Zookeeper 없는 경량 Kafka 클러스터
- ✅ **Idempotent Event Processing**: eventId 기반 중복 이벤트 처리 방지
- ✅ **React Query + Zustand**: 서버 상태와 클라이언트 상태 분리 관리

## 📄 라이선스

MIT License
