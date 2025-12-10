#!/bin/bash

echo "Building E-commerce Microservices..."

# Build all services
./gradlew build -x test

# Build Docker images
echo "Building Docker images..."
docker build --platform linux/amd64 -f Dockerfile.order -t order-service:latest .
docker build --platform linux/amd64 -f Dockerfile.payment -t payment-service:latest .
docker build --platform linux/amd64 -f Dockerfile.erp -t erp-service:latest .

echo "Build completed successfully!"
echo "To start locally: docker-compose up -d"
echo "To deploy to K8s: kubectl apply -f k8s/"