#!/bin/bash

echo "Deploying to local Minikube..."

# Build images for minikube
eval $(minikube docker-env)
./build.sh

# Deploy using kustomize
kubectl apply -k k8s/overlays/local

# Wait for deployments
kubectl wait --for=condition=available --timeout=300s deployment/order-service
kubectl wait --for=condition=available --timeout=300s deployment/payment-service
kubectl wait --for=condition=available --timeout=300s deployment/erp-service

# Get service URLs
echo "Service URLs:"
minikube service order-service --url
minikube service payment-service --url
minikube service erp-service --url

echo "Deployment completed!"