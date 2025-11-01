#!/bin/bash

# Script to add MyBank domains to /etc/hosts

echo "🌐 Setting up /etc/hosts entries for MyBank..."
echo ""

HOSTS_ENTRY="127.0.0.1 app.mybank.com api.mybank.com eureka.mybank.com grafana.mybank.com kafka-ui.mybank.com prometheus.mybank.com argocd.mybank.com"

# Check if entries already exist
if grep -q "mybank.com" /etc/hosts; then
  echo "⚠️  MyBank entries already exist in /etc/hosts"
  echo ""
  echo "Current entries:"
  grep "mybank.com" /etc/hosts
  echo ""
  read -p "Do you want to remove old entries and add new ones? (y/n) " -n 1 -r
  echo ""
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Remove old entries
    sudo sed -i.bak '/mybank.com/d' /etc/hosts
    echo "✅ Old entries removed"
  else
    echo "Skipping..."
    exit 0
  fi
fi

# Add new entries
echo "$HOSTS_ENTRY" | sudo tee -a /etc/hosts > /dev/null

echo "✅ /etc/hosts updated successfully!"
echo ""
echo "Added entries:"
echo "$HOSTS_ENTRY"
echo ""
echo "📝 You can now access:"
echo "   https://app.mybank.com (Frontend)"
echo "   https://api.mybank.com (API Gateway)"
echo "   https://eureka.mybank.com (Service Discovery)"
echo "   https://grafana.mybank.com (Grafana)"
echo "   https://kafka-ui.mybank.com (Kafka UI)"
echo "   https://prometheus.mybank.com (Prometheus)"
echo "   https://argocd.mybank.com (ArgoCD)"
echo ""
echo "⚠️  Note: These are HTTPS URLs. Make sure to:"
echo "   1. Run ./scripts/generate-certs.sh to create certificates"
echo "   2. Trust the CA certificate on your system"
