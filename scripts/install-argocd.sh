#!/bin/bash

# Install ArgoCD on Kubernetes cluster

set -e

echo "🚀 Installing ArgoCD..."
echo ""

# Create argocd namespace
echo "1. Creating argocd namespace..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

# Install ArgoCD
echo "2. Installing ArgoCD components..."
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
echo ""
echo "3. Waiting for ArgoCD pods to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=300s

# Apply custom configurations
echo ""
echo "4. Applying custom ArgoCD configurations..."
kubectl apply -f k8s/argocd/install-argocd.yaml

# Get initial admin password
echo ""
echo "5. Retrieving ArgoCD admin password..."
ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)

echo ""
echo "✅ ArgoCD installed successfully!"
echo ""
echo "📝 Access Information:"
echo "   URL: https://argocd.mybank.com"
echo "   Username: admin"
echo "   Password: $ARGOCD_PASSWORD"
echo ""
echo "💡 To change the password:"
echo "   argocd account update-password"
echo ""
echo "📦 Installing ArgoCD CLI (optional):"
echo "   macOS: brew install argocd"
echo "   Linux: curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64"
echo ""
echo "🔐 Login via CLI:"
echo "   argocd login argocd.mybank.com --username admin --password $ARGOCD_PASSWORD --insecure"
echo ""

# Save password to file for reference
echo "$ARGOCD_PASSWORD" > .argocd-password
echo "⚠️  Password saved to .argocd-password (add to .gitignore)"
