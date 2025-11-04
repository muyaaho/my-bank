#!/bin/bash

# MyBank - Service Shutdown Script

echo "🛑 Stopping MyBank Services..."

# Stop Docker Compose services
echo "Stopping Docker infrastructure..."
docker-compose down

echo ""
echo "✅ All services stopped"
echo ""
echo "To remove volumes (WARNING: This will delete all data):"
echo "  docker-compose down -v"
