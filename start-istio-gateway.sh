#!/bin/bash

echo "🚀 Starting Istio Ingress Gateway port forwarding..."
echo ""
echo "This will forward:"
echo "  - localhost:80 -> Istio Gateway HTTP"
echo "  - localhost:443 -> Istio Gateway HTTPS"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Forward both HTTP and HTTPS ports
kubectl port-forward -n istio-system svc/istio-ingressgateway 80:80 443:443
