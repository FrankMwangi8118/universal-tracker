#!/bin/bash
set -e


echo "==> Building Docker image..."
docker build -t universal-tracker:latest .

echo "==> Stopping old containers..."
docker compose down

echo "==> Starting services..."
docker compose up -d

echo "==> Waiting for app to be healthy..."
sleep 5

docker compose ps

echo "==> Logs (last 50 lines):"
docker compose logs --tail=50 app

echo ""
echo "Done. App running at http://$(hostname -I | awk '{print $1}'):8080"
