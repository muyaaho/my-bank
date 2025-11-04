#!/bin/bash

# Complete deployment script for MyBank on Kind cluster with HTTPS

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}🚀 Deploying MyBank to Kind cluster with HTTPS${NC}"
echo ""

# Step 1: Build Docker images
echo -e "${YELLOW}1️⃣ Building Docker images...${NC}"
./build-images.sh

# Step 2: Load images into Kind
echo -e "${YELLOW}2️⃣ Loading images into Kind cluster...${NC}"
kind load docker-image mybank/api-gateway:latest --name mybank-cluster
kind load docker-image mybank/auth-service:latest --name mybank-cluster
kind load docker-image mybank/user-service:latest --name mybank-cluster
kind load docker-image mybank/pfm-core-service:latest --name mybank-cluster
kind load docker-image mybank/payment-service:latest --name mybank-cluster
kind load docker-image mybank/investment-service:latest --name mybank-cluster
kind load docker-image mybank/frontend:latest --name mybank-cluster

echo ""
echo -e "${YELLOW}3️⃣ Deploying infrastructure...${NC}"

# PostgreSQL for auth-service
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-auth-init
  namespace: mybank
data:
  init.sql: |
    CREATE DATABASE mybank_auth;
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-auth
  namespace: mybank
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres-auth
  template:
    metadata:
      labels:
        app: postgres-auth
    spec:
      containers:
        - name: postgres
          image: postgres:16
          env:
            - name: POSTGRES_USER
              value: "mybank"
            - name: POSTGRES_PASSWORD
              value: "mybank123"
            - name: POSTGRES_DB
              value: "mybank_auth"
          ports:
            - containerPort: 5432
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
      volumes:
        - name: data
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-auth
  namespace: mybank
spec:
  selector:
    app: postgres-auth
  ports:
    - port: 5432
      targetPort: 5432
  type: ClusterIP
EOF

# PostgreSQL for user-service
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-user
  namespace: mybank
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres-user
  template:
    metadata:
      labels:
        app: postgres-user
    spec:
      containers:
        - name: postgres
          image: postgres:16
          env:
            - name: POSTGRES_USER
              value: "mybank"
            - name: POSTGRES_PASSWORD
              value: "mybank123"
            - name: POSTGRES_DB
              value: "mybank_user"
          ports:
            - containerPort: 5432
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
      volumes:
        - name: data
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-user
  namespace: mybank
spec:
  selector:
    app: postgres-user
  ports:
    - port: 5432
      targetPort: 5432
  type: ClusterIP
EOF

# MongoDB
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mongodb
  namespace: mybank
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mongodb
  template:
    metadata:
      labels:
        app: mongodb
    spec:
      containers:
        - name: mongodb
          image: mongo:7
          env:
            - name: MONGO_INITDB_ROOT_USERNAME
              value: "root"
            - name: MONGO_INITDB_ROOT_PASSWORD
              value: "rootpassword"
          ports:
            - containerPort: 27017
          volumeMounts:
            - name: data
              mountPath: /data/db
      volumes:
        - name: data
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: mongodb
  namespace: mybank
spec:
  selector:
    app: mongodb
  ports:
    - port: 27017
      targetPort: 27017
  type: ClusterIP
EOF

# Redis
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: mybank
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          command: ["redis-server", "--appendonly", "yes"]
          ports:
            - containerPort: 6379
          volumeMounts:
            - name: data
              mountPath: /data
      volumes:
        - name: data
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: mybank
spec:
  selector:
    app: redis
  ports:
    - port: 6379
      targetPort: 6379
  type: ClusterIP
EOF

echo -e "${GREEN}✅ Infrastructure deployed${NC}"
echo ""
echo -e "${YELLOW}4️⃣ Waiting for infrastructure to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app=postgres-auth -n mybank --timeout=120s
kubectl wait --for=condition=ready pod -l app=postgres-user -n mybank --timeout=120s
kubectl wait --for=condition=ready pod -l app=mongodb -n mybank --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis -n mybank --timeout=120s

echo ""
echo -e "${YELLOW}5️⃣ Deploying microservices...${NC}"
kubectl apply -f k8s/services/auth-service.yaml
kubectl apply -f k8s/services/user-service.yaml
kubectl apply -f k8s/services/pfm-core-service.yaml
kubectl apply -f k8s/services/payment-service.yaml
kubectl apply -f k8s/services/investment-service.yaml
kubectl apply -f k8s/services/api-gateway.yaml

echo ""
echo -e "${YELLOW}6️⃣ Deploying Istio Gateway and VirtualServices...${NC}"
kubectl apply -f k8s/istio/gateway.yaml
kubectl apply -f k8s/istio/virtual-service.yaml

echo ""
echo -e "${YELLOW}7️⃣ Waiting for services to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app=auth-service -n mybank --timeout=180s || true
kubectl wait --for=condition=ready pod -l app=user-service -n mybank --timeout=180s || true

echo ""
echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""
echo -e "${YELLOW}📝 Access URLs:${NC}"
echo "  Frontend: https://app.mybank.local"
echo "  API: https://api.mybank.local"
echo ""
echo -e "${YELLOW}🔧 Setup instructions:${NC}"
echo "  1. Trust CA certificate:"
echo "     sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain certs/ca.crt"
echo ""
echo "  2. Add to /etc/hosts:"
echo "     echo '127.0.0.1 mybank.local api.mybank.local app.mybank.local' | sudo tee -a /etc/hosts"
echo ""
echo "  3. Port forward (in separate terminals):"
echo "     kubectl port-forward -n istio-system svc/istio-ingressgateway 443:443"
echo "     kubectl port-forward -n istio-system svc/istio-ingressgateway 80:80"
echo ""
echo -e "${YELLOW}📊 Check status:${NC}"
echo "  kubectl get pods -n mybank"
echo "  kubectl logs -f deployment/auth-service -n mybank"
echo ""
