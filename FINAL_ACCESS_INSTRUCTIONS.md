# 🎉 MyBank 배포 완료 - 최종 접속 방법

## ✅ 배포 완료!

모든 서비스가 Kind 클러스터에 HTTPS로 배포되었습니다.

## 🚀 빠른 시작 (3단계)

### 1단계: CA 인증서 신뢰 (최초 1회)

```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  /Users/kimhyeonwoo/Documents/GitHub/mybank/certs/ca.crt
```

### 2단계: Hosts 파일 설정 (최초 1회)

```bash
echo "127.0.0.1 mybank.com api.mybank.com app.mybank.com" | sudo tee -a /etc/hosts
```

### 3단계: 포트 포워딩 시작

```bash
cd /Users/kimhyeonwoo/Documents/GitHub/mybank
kubectl port-forward -n istio-system svc/istio-ingressgateway 8443:443 8080:80
```

## 🌐 접속 URL

- **프론트엔드**: https://app.mybank.com:8443
- **API**: https://api.mybank.com:8443
- **Health Check**: https://api.mybank.com:8443/actuator/health

## 🧪 API 테스트

### 1. 회원가입

```bash
curl -X POST https://api.mybank.com:8443/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.com",
    "password": "password123",
    "name": "Test User",
    "phoneNumber": "010-1234-5678"
  }' \
  --cacert certs/ca.crt
```

### 2. 로그인

```bash
curl -X POST https://api.mybank.com:8443/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.com",
    "password": "password123"
  }' \
  --cacert certs/ca.crt
```

### 3. 프로필 조회 (인증 필요)

```bash
# 로그인에서 받은 토큰을 사용
export TOKEN="<access_token_from_login>"

curl https://api.mybank.com:8443/api/v1/user/profile \
  -H "Authorization: Bearer $TOKEN" \
  --cacert certs/ca.crt
```

## 📊 서비스 상태 확인

```bash
# 모든 Pod 상태 확인
kubectl get pods -n mybank

# 특정 서비스 로그 확인
kubectl logs -f deployment/user-service -n mybank -c user-service
kubectl logs -f deployment/auth-service -n mybank -c auth-service

# Redis 세션 확인
kubectl exec -it deployment/redis -n mybank -- redis-cli KEYS "mybank:*"
```

## 🎯 주요 변경사항

### ✅ Auth/User 서비스 분리
- **auth-service** (8081): 인증 전용
  - 로그인/로그아웃
  - JWT 발급
  - OAuth 2.0
  - 데이터베이스: `mybank_auth` (postgres-auth)

- **user-service** (8085): 사용자 관리 전용 ⭐ **NEW**
  - 사용자 프로필 CRUD
  - 권한/역할 관리
  - 사용자 검색
  - 데이터베이스: `mybank_user` (postgres-user)

### ✅ 강화된 보안
1. **API Gateway 이중 검증**
   - JWT 서명/만료 검증
   - Redis 세션 유효성 검증
   - 토큰 블랙리스트 확인
   - 슬라이딩 윈도우 세션 (30분)

2. **마이크로서비스 인증**
   - Header 기반 인증 (X-User-Id, X-User-Email, X-User-Roles)
   - Spring Security 통합
   - Method-level authorization (@PreAuthorize)

3. **HTTPS 전용**
   - 모든 트래픽 TLS 암호화
   - 자체 서명 인증서 (10년 유효)
   - HTTP → HTTPS 리다이렉트

### ✅ 인프라
- **PostgreSQL** 2개: auth DB + user DB (분리)
- **MongoDB**: 비즈니스 데이터
- **Redis**: 세션 + 캐시
- **Kafka**: 이벤트 스트리밍
- **Istio**: 서비스 메시

## 🔍 트러블슈팅

### 포트가 이미 사용 중인 경우

```bash
# 실행 중인 port-forward 확인
ps aux | grep "kubectl port-forward"

# 종료
pkill -f "kubectl port-forward"

# 다시 시작
kubectl port-forward -n istio-system svc/istio-ingressgateway 8443:443 8080:80
```

### 인증서 경고가 나타나는 경우

1. CA 인증서 신뢰 확인:
```bash
security find-certificate -c "MyBank Root CA" -a
```

2. 브라우저 완전 재시작
3. SSL 상태 초기화 (Chrome: chrome://settings/clearBrowserData)

### DNS가 해결되지 않는 경우

```bash
# hosts 파일 확인
cat /etc/hosts | grep mybank

# 없으면 추가
echo "127.0.0.1 mybank.com api.mybank.com app.mybank.com" | sudo tee -a /etc/hosts
```

## 📈 아키텍처 하이라이트

```
Browser (HTTPS:8443)
    ↓
Istio Ingress Gateway
    ↓
API Gateway (Spring Security WebFlux)
    ├─ JwtAuthenticationWebFilter (JWT 검증)
    └─ RedisSessionWebFilter (세션 검증 + 슬라이딩 윈도우)
    ↓
Headers: X-User-Id, X-User-Email, X-User-Roles
    ↓
Microservices (Header-based Auth)
    ├─ auth-service (인증)
    ├─ user-service (사용자) ⭐ NEW
    ├─ pfm-core-service (PFM)
    ├─ payment-service (결제)
    └─ investment-service (투자)
```

## 📚 참고 문서

- [아키텍처 리팩토링 요약](./ARCHITECTURE_REFACTORING_SUMMARY.md)
- [배포 완료 가이드](./DEPLOYMENT_COMPLETE.md)
- [테스트 가이드](./TEST_DEPLOYMENT.md)
- [인증서 README](./certs/README.md)

## 🎊 완료!

이제 브라우저에서 **https://app.mybank.com:8443** 에 접속하여 MyBank를 사용할 수 있습니다!

---

**배포 일시**: 2025-11-03
**클러스터**: Kind (mybank-cluster)
**네임스페이스**: mybank
**포트**: 8443 (HTTPS), 8080 (HTTP)
**인증서 만료**: 2035-11-01 (10년)
