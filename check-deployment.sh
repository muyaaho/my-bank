#!/bin/bash

echo "======================================"
echo "  MyBank Deployment Status"
echo "======================================"
echo ""

echo "📦 Pods Status:"
kubectl get pods -n mybank
echo ""

echo "🌐 Services:"
kubectl get svc -n mybank
echo ""

echo "🔀 Ingress Rules:"
kubectl get ingress -n mybank
echo ""

echo "🔍 /etc/hosts entries:"
grep mybank.com /etc/hosts || echo "❌ mybank.com entries not found in /etc/hosts"
echo ""

echo "🔐 TLS Certificates:"
ls -la certs/ 2>/dev/null || echo "❌ certs/ directory not found"
echo ""

echo "======================================"
echo "  Access URLs"
echo "======================================"
echo "Frontend:  https://app.mybank.com"
echo "API:       https://api.mybank.com"
echo "Eureka:    https://eureka.mybank.com"
echo "ArgoCD:    https://argocd.mybank.com"
echo "======================================"
