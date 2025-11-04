# MyBank 접근 가이드 (https://app.mybank.com)

## ✅ 배포 완료!

모든 서비스가 Kind 클러스터에 배포되었으며 `https://app.mybank.com` 으로 접근 가능합니다.

## 🚀 빠른 시작 (3단계)

### 1단계: CA 인증서 신뢰 (최초 1회)

기존에 생성된 CA 인증서를 그대로 사용합니다:

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

## 📊 인증서 정보

- **루트 CA**: `certs/ca.crt` (기존 인증서 재사용)
- **서버 인증서**: `certs/tls-mybank.crt` (새로 생성)
- **서버 키**: `certs/tls-mybank.key` (새로 생성)
- **인증서 체인**: `certs/fullchain-mybank.pem` (서버 + CA)

### 인증서 도메인 (SAN):
- mybank.com
- *.mybank.com
- api.mybank.com
- app.mybank.com
- auth.mybank.com
- user.mybank.com
- pfm.mybank.com
- payment.mybank.com
- investment.mybank.com
- localhost
- 127.0.0.1

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

## 📈 배포 아키텍처

```
Browser (HTTPS:8443)
    ↓
Port Forward (localhost:8443 → cluster:443)
    ↓
Istio Ingress Gateway (TLS termination)
    ├─ Certificate: *.mybank.com
    └─ CA: MyBank Root CA
    ↓
VirtualService Routing
    ├─ app.mybank.com → frontend:3000
    ├─ api.mybank.com/api/v1/auth/** → auth-service:8081
    ├─ api.mybank.com/api/v1/user/** → user-service:8085
    └─ api.mybank.com/** → api-gateway:8080
```

## ✅ 작동 중인 서비스

- ✅ auth-service (2/2 Running)
- ✅ user-service (2/2 Running)
- ✅ api-gateway (2/2 Running)
- ✅ frontend (2/2 Running)
- ✅ postgres-auth (2/2 Running)
- ✅ postgres-user (2/2 Running)
- ✅ mongodb (2/2 Running)
- ✅ redis (2/2 Running)

## 🎊 완료!

이제 브라우저에서 **https://app.mybank.com:8443** 에 접속하여 MyBank를 사용할 수 있습니다!

---

**배포 일시**: 2025-11-03
**클러스터**: Kind (mybank-cluster)
**네임스페이스**: mybank
**도메인**: mybank.com
**포트**: 8443 (HTTPS), 8080 (HTTP)
**인증서**: 루트 CA 재사용, 서버 인증서 신규 생성
