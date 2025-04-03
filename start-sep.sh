#!/bin/bash
set -e

if ! docker info > /dev/null 2>&1; then
  echo "Start Docker Desktop to continue..."
  exit 1
fi

echo "Initializing SEP services..."
docker compose -f docker-compose.yml up -d --build

echo "Check the status of SEP services..."
docker ps

echo "Press any key to exit..."
read -n 1 -s