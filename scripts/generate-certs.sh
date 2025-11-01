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
  -subj "/C=KR/ST=Seoul/L=Seoul/O=MyBank/OU=IT/CN=MyBank Root CA"

echo "✅ CA certificate generated"
echo ""

# Function to generate certificate for a domain
generate_cert() {
  local domain=$1
  local common_name=$2

  echo "Generating certificate for $domain..."

  # Generate private key
  openssl genrsa -out "$CERT_DIR/$domain.key" 2048

  # Create certificate signing request (CSR)
  openssl req -new -key "$CERT_DIR/$domain.key" \
    -out "$CERT_DIR/$domain.csr" \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=MyBank/OU=IT/CN=$common_name"

  # Create SAN config
  cat > "$CERT_DIR/$domain.ext" <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = $common_name
DNS.2 = *.$common_name
DNS.3 = localhost
IP.1 = 127.0.0.1
EOF

  # Sign certificate with CA
  openssl x509 -req -in "$CERT_DIR/$domain.csr" \
    -CA "$CERT_DIR/ca.crt" -CAkey "$CERT_DIR/ca.key" \
    -CAcreateserial -out "$CERT_DIR/$domain.crt" \
    -days 825 -sha256 -extfile "$CERT_DIR/$domain.ext"

  # Clean up CSR and extension file
  rm "$CERT_DIR/$domain.csr" "$CERT_DIR/$domain.ext"

  echo "✅ Certificate for $domain generated"
}

# Generate certificates for all domains
echo "2. Generating service certificates..."
echo ""

generate_cert "app" "app.mybank.com"
generate_cert "api" "api.mybank.com"
generate_cert "eureka" "eureka.mybank.com"
generate_cert "grafana" "grafana.mybank.com"
generate_cert "kafka-ui" "kafka-ui.mybank.com"
generate_cert "prometheus" "prometheus.mybank.com"
generate_cert "argocd" "argocd.mybank.com"

echo ""
echo "3. Creating Kubernetes TLS secrets..."
echo ""

# Create namespace if not exists
kubectl create namespace mybank --dry-run=client -o yaml | kubectl apply -f -

# Create TLS secrets for each service
kubectl create secret tls mybank-tls-app \
  --cert="$CERT_DIR/app.crt" \
  --key="$CERT_DIR/app.key" \
  --namespace=mybank \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret tls mybank-tls-api \
  --cert="$CERT_DIR/api.crt" \
  --key="$CERT_DIR/api.key" \
  --namespace=mybank \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret tls mybank-tls-eureka \
  --cert="$CERT_DIR/eureka.crt" \
  --key="$CERT_DIR/eureka.key" \
  --namespace=mybank \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret tls mybank-tls-grafana \
  --cert="$CERT_DIR/grafana.crt" \
  --key="$CERT_DIR/grafana.key" \
  --namespace=mybank \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret tls mybank-tls-kafka-ui \
  --cert="$CERT_DIR/kafka-ui.crt" \
  --key="$CERT_DIR/kafka-ui.key" \
  --namespace=mybank \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret tls mybank-tls-prometheus \
  --cert="$CERT_DIR/prometheus.crt" \
  --key="$CERT_DIR/prometheus.key" \
  --namespace=mybank \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret tls mybank-tls-argocd \
  --cert="$CERT_DIR/argocd.crt" \
  --key="$CERT_DIR/argocd.key" \
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
echo ""
echo "📝 To trust these certificates on your local machine:"
echo "   macOS: sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain $CERT_DIR/ca.crt"
echo "   Linux: sudo cp $CERT_DIR/ca.crt /usr/local/share/ca-certificates/ && sudo update-ca-certificates"
echo ""
echo "⚠️  Don't forget to add these entries to /etc/hosts:"
echo "   127.0.0.1 app.mybank.com"
echo "   127.0.0.1 api.mybank.com"
echo "   127.0.0.1 eureka.mybank.com"
echo "   127.0.0.1 grafana.mybank.com"
echo "   127.0.0.1 kafka-ui.mybank.com"
echo "   127.0.0.1 prometheus.mybank.com"
echo "   127.0.0.1 argocd.mybank.com"
