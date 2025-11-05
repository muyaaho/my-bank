#!/bin/bash

# Script to generate self-signed TLS certificates for MyBank services

set -e

CERT_DIR="./certs"
mkdir -p "$CERT_DIR"

echo "🔐 Generating TLS certificates for MyBank services..."
echo ""

# Generate CA private key and certificate
echo "1. Generating Certificate Authority (CA)..."
openssl genrsa -out "$CERT_DIR/ca.key" 4096

openssl req -x509 -new -nodes -key "$CERT_DIR/ca.key" \
  -sha256 -days 3650 -out "$CERT_DIR/ca.crt" \
  -subj "/C=KR/ST=Seoul/L=Seoul/O=MyBank CA/CN=MyBank Root CA"

echo "✅ CA certificate generated"
echo ""

# Generate wildcard certificate for *.mybank.com
echo "2. Generating wildcard certificate for *.mybank.com..."

# Generate private key
openssl genrsa -out "$CERT_DIR/tls-mybank.key" 4096

# Create certificate signing request (CSR)
openssl req -new -key "$CERT_DIR/tls-mybank.key" \
  -out "$CERT_DIR/tls-mybank.csr" \
  -subj "/C=KR/ST=Seoul/L=Seoul/O=MyBank/OU=Engineering/CN=*.mybank.com/emailAddress=admin@mybank.com"

# Create SAN config with all domains
cat > "$CERT_DIR/tls-mybank.ext" <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = mybank.com
DNS.2 = *.mybank.com
DNS.3 = api.mybank.com
DNS.4 = app.mybank.com
DNS.5 = auth.mybank.com
DNS.6 = user.mybank.com
DNS.7 = pfm.mybank.com
DNS.8 = payment.mybank.com
DNS.9 = investment.mybank.com
DNS.10 = eureka.mybank.com
DNS.11 = grafana.mybank.com
DNS.12 = kafka-ui.mybank.com
DNS.13 = prometheus.mybank.com
DNS.14 = argocd.mybank.com
DNS.15 = localhost
IP.1 = 127.0.0.1
EOF

# Sign certificate with CA
openssl x509 -req -in "$CERT_DIR/tls-mybank.csr" \
  -CA "$CERT_DIR/ca.crt" -CAkey "$CERT_DIR/ca.key" \
  -CAcreateserial -out "$CERT_DIR/tls-mybank.crt" \
  -days 825 -sha256 -extfile "$CERT_DIR/tls-mybank.ext"

# Clean up CSR and extension file
rm "$CERT_DIR/tls-mybank.csr" "$CERT_DIR/tls-mybank.ext"

echo "✅ Wildcard certificate generated"
echo ""

# Verify certificate
echo "3. Verifying certificate..."
openssl x509 -in "$CERT_DIR/tls-mybank.crt" -text -noout | grep -A 1 "Subject Alternative Name"
echo ""

echo "4. Creating Kubernetes TLS secrets..."
echo ""

# Create namespace if not exists
kubectl create namespace mybank --dry-run=client -o yaml | kubectl apply -f -

# Create single TLS secret for Istio Gateway
kubectl create secret tls mybank-tls-cert \
  --cert="$CERT_DIR/tls-mybank.crt" \
  --key="$CERT_DIR/tls-mybank.key" \
  --namespace=istio-system \
  --dry-run=client -o yaml | kubectl apply -f -

# Also create in mybank namespace for internal services
kubectl create secret tls mybank-tls-cert \
  --cert="$CERT_DIR/tls-mybank.crt" \
  --key="$CERT_DIR/tls-mybank.key" \
  --namespace=mybank \
  --dry-run=client -o yaml | kubectl apply -f -

# Create CA certificate as ConfigMap for services to trust
kubectl create configmap mybank-ca-cert \
  --from-file=ca.crt="$CERT_DIR/ca.crt" \
  --namespace=mybank \
  --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "✅ All certificates generated and stored in Kubernetes!"
echo ""
echo "📁 Certificate files location: $CERT_DIR/"
echo "   - ca.crt / ca.key: Certificate Authority"
echo "   - tls-mybank.crt / tls-mybank.key: Wildcard certificate for *.mybank.com"
echo ""
echo "📝 To trust these certificates on your local machine:"
echo "   macOS: sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain $CERT_DIR/ca.crt"
echo "   Linux: sudo cp $CERT_DIR/ca.crt /usr/local/share/ca-certificates/ && sudo update-ca-certificates"
echo ""
echo "⚠️  Don't forget to add these entries to /etc/hosts:"
echo "   127.0.0.1 app.mybank.com api.mybank.com eureka.mybank.com"
echo "   127.0.0.1 grafana.mybank.com kafka-ui.mybank.com prometheus.mybank.com argocd.mybank.com"
