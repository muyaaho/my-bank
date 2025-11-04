#!/bin/bash

# Port forward Istio Ingress Gateway for local access
# Run this script to access the deployed services via HTTPS

echo "🚀 Starting port forward for Istio Ingress Gateway..."
echo ""
echo "📌 Access URLs:"
echo "  Frontend: https://app.mybank.local"
echo "  API: https://api.mybank.local"
echo ""
echo "⚠️  Make sure you have:"
echo "  1. Trusted CA certificate (certs/ca.crt)"
echo "  2. Added hosts to /etc/hosts"
echo ""
echo "Press Ctrl+C to stop"
echo ""

kubectl port-forward -n istio-system svc/istio-ingressgateway 8443:443 8080:80
