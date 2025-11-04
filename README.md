# MyBank - 현대적인 핀테크 플랫폼

**Spring Boot 3**, **Spring Cloud**, **Kafka**, **MongoDB**, **Next.js**로 구축된 클라우드 네이티브 마이크로서비스 기반 핀테크 플랫폼입니다. 높은 확장성과 성능을 위해 **MSA (Microservices Architecture)** 및 **EDA (Event-Driven Architecture)** 패턴을 구현했습니다.

## 아키텍처 개요

```
┌──────────────┐
│   Browser    │
│  (Frontend)  │
└──────┬───────┘
       │ http://localhost:3000
       │
┌──────▼───────────────────────────────────────────────────────┐
│                       API Gateway (8080)                      │
│              JWT Authentication & Routing                      │
└────────────┬─────────────────────────────────────────────────┘
             │
    ┌────────┴────────┬──────────┬──────────┬──────────┐
    │                 │          │          │          │
┌───▼────┐  ┌────▼─────┐  ┌──▼───┐  ┌──▼───┐  ┌──▼───┐
│  Auth  │  │   PFM    │  │Payment│  │Invest│  │ ...  │
│Service │  │ Service  │  │Service│  │Service│  │      │
│        │  │          │  │       │  │      │  │      │
│:8081   │  │  :8082   │  │ :8083 │  │:8084 │  │      │
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
- **OAuth 2.0 & JWT**: 중앙화된 인증 처리
- **계정 잠금**: 무차별 대입 공격 방지
- **Redis 세션**: 분산 세션 관리

## 기술 스택

| 분류 | 기술 | 용도 |
|------|------|------|
| **아키텍처** | MSA, EDA | 서비스 독립성, 비동기 통신 |
| **프론트엔드** | Next.js 14, React, TypeScript | 현대적인 웹 애플리케이션 |
| **백엔드** | Spring Boot 3.2, Spring Cloud 2023 | 마이크로서비스 프레임워크 |
| **서비스 디스커버리** | Eureka | 서비스 레지스트리 |
| **API Gateway** | Spring Cloud Gateway | 요청 라우팅, JWT 검증 |
| **주요 DB** | MongoDB, PostgreSQL | 유연한 스키마(PFM), ACID 트랜잭션(Auth) |
| **캐시** | Redis Cluster | 세션, 랭킹, 실시간 데이터 |
| **메시징** | Apache Kafka | 이벤트 스트리밍, EDA 구현 |
| **모니터링** | Prometheus, Grafana | 메트릭 수집, 시각화 |
| **컨테이너** | Docker, Docker Compose | 로컬 개발 환경 |
| **오케스트레이션** | Kubernetes (Kind) | 프로덕션 배포 |

## 프로젝트 구조

```
my-bank/
├── app/                       # 프론트엔드 (Next.js)
│   ├── app/                  # Next.js App Router
│   ├── components/           # React 컴포넌트
│   ├── lib/                  # API 클라이언트, 유틸리티
│   ├── stores/               # 상태 관리
│   └── types/                # TypeScript 타입
├── api-gateway/              # API Gateway (Port 8080)
├── config-server/            # Config Server (Port 8888)
├── service-discovery/        # Eureka Server (Port 8761)
├── auth-service/             # 인증 서비스 (Port 8081)
├── pfm-core-service/         # 자산 관리 서비스 (Port 8082)
├── payment-service/          # 송금 서비스 (Port 8083)
├── investment-service/       # 투자 서비스 (Port 8084)
├── common-lib/               # 공통 라이브러리, DTOs, 설정
├── k8s/                      # Kubernetes 매니페스트
├── docker/                   # Docker 설정
└── docker-compose.yml        # 로컬 개발 환경
```

## 개발자 문서

상세한 개발 가이드, 아키텍처 패턴, 자주 사용하는 명령어는 **[CLAUDE.md](./CLAUDE.md)**를 참고하세요.

## 시작하기

### 사전 요구사항

- **Java 21** 이상
- **Docker** 및 **Docker Compose**
- **Gradle 8.x**
- **Node.js 20+** (프론트엔드 개발용)

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
curl http://localhost:8081/auth/health
curl http://localhost:8082/pfm/health
curl http://localhost:8083/payment/health
curl http://localhost:8084/invest/health
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
# 모든 이미지 빌드 및 배포
./kind-deploy-all.sh

# NodePort를 통한 프론트엔드 접속
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

## 배포

### Docker Compose

```bash
# 모든 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 서비스 중지
docker-compose down
```

### Kubernetes (Kind)

```bash
# 전체 빌드 및 배포
./kind-deploy-all.sh

# 서비스 접속
프론트엔드:         http://localhost:30000
API Gateway:      http://localhost:8080
Eureka 대시보드:   http://localhost:8761
Kafka UI:         http://localhost:8090

# Pod 확인
kubectl get pods -n mybank

# 로그 확인
kubectl logs -f deployment/frontend -n mybank

# 정리
./undeploy-kind.sh
```

## 플랫폼 테스트

### 1. 계정 생성

http://localhost:3000 (Kind 사용 시 :30000) 접속하여 회원가입:
- Email: test@mybank.com
- Password: MyBank123!
- Name: 홍길동
- Phone: 010-1234-5678

### 2. 기능 탐색

로그인 후 다음 기능 확인:
- **Dashboard**: 자산 및 카테고리별 분류 확인
- **지출 분석**: 지출 패턴 분석
- **투자**: 투자 포트폴리오 및 거스름돈 투자 추적
- **송금**: 계좌 간 송금

### 3. 거스름돈 투자 테스트

1. 송금 페이지로 이동
2. 송금 실행 (예: 15,300원)
3. 투자 페이지로 이동
4. 자동 거스름돈 투자 확인 (200원이 투자되어 15,500원으로 올림)

## API 문서

### 인증 엔드포인트

- `POST /api/v1/auth/register` - 회원가입
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/logout` - 로그아웃
- `POST /api/v1/auth/refresh` - 토큰 갱신

### PFM 엔드포인트

- `GET /api/v1/pfm/assets` - 자산 요약 조회
- `GET /api/v1/pfm/spending/analysis?daysBack=30` - 지출 분석 조회

### 송금 엔드포인트

- `POST /api/v1/payment/transfer` - 송금 실행
- `GET /api/v1/payment/{paymentId}` - 송금 상세 조회

### 투자 엔드포인트

- `GET /api/v1/invest/summary` - 투자 요약 조회

## 추가 문서

- **[CLAUDE.md](./CLAUDE.md)** - 아키텍처 패턴, 테스트 전략, 트러블슈팅을 포함한 완전한 개발자 가이드
- **[QUICKSTART.md](./QUICKSTART.md)** - 전체 시스템 빠른 배포 가이드
- **[app/README.md](./app/README.md)** - 프론트엔드 개발 가이드

## 라이선스

MIT License
