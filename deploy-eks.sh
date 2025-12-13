#!/bin/bash

# Production Kubernetes Deployment Script
set -e

REGION="us-west-2"
CLUSTER_NAME="ecommerce-cluster"
REGISTRY="your-registry.com"
VERSION="v1.0.0"

echo "Deploying to production Kubernetes cluster: $CLUSTER_NAME"
echo "Registry: $REGISTRY"
echo "Version: $VERSION"

# Check if kubectl is configured
if ! kubectl cluster-info &> /dev/null; then
    echo "Error: kubectl not configured or cluster not accessible"
    exit 1
fi

# Update kustomization with actual registry
echo "Updating image registry in kustomization..."
sed -i.bak "s|your-registry|$REGISTRY|g" k8s/overlays/production/kustomization.yaml
sed -i.bak "s|v1.0.0|$VERSION|g" k8s/overlays/production/kustomization.yaml

# Deploy using kustomize
echo "Deploying to production..."
kubectl apply -k k8s/overlays/production

# Wait for infrastructure services
echo "Waiting for infrastructure services..."
kubectl wait --for=condition=available --timeout=600s deployment/postgres-order -n ecommerce
kubectl wait --for=condition=available --timeout=600s deployment/postgres-payment -n ecommerce
kubectl wait --for=condition=available --timeout=600s deployment/kafka -n ecommerce
kubectl wait --for=condition=available --timeout=600s deployment/vault -n ecommerce

# Wait for application services
echo "Waiting for application services..."
kubectl wait --for=condition=available --timeout=600s deployment/order-service -n ecommerce
kubectl wait --for=condition=available --timeout=600s deployment/payment-service -n ecommerce
kubectl wait --for=condition=available --timeout=600s deployment/erp-service -n ecommerce

# Show deployment status
echo "\nDeployment Status:"
kubectl get pods -n ecommerce
kubectl get services -n ecommerce

# Get external IPs
echo "\nExternal Access:"
echo "Waiting for LoadBalancer to assign external IP..."
kubectl wait --for=jsonpath='{.status.loadBalancer.ingress}' service/order-service -n ecommerce --timeout=300s || true

ORDER_SERVICE_IP=$(kubectl get service order-service -n ecommerce -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
ORDER_SERVICE_HOSTNAME=$(kubectl get service order-service -n ecommerce -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)

if [ -n "$ORDER_SERVICE_IP" ]; then
    echo "Order Service: http://$ORDER_SERVICE_IP:8080"
elif [ -n "$ORDER_SERVICE_HOSTNAME" ]; then
    echo "Order Service: http://$ORDER_SERVICE_HOSTNAME:8080"
else
    echo "Order Service: LoadBalancer IP/hostname not yet assigned"
    echo "Check with: kubectl get svc order-service -n ecommerce"
fi

echo "\nDeployment completed successfully!"
echo "Monitor with: kubectl get pods -n ecommerce -w"
echo "View logs: kubectl logs -f deployment/order-service -n ecommerce"

# Restore original kustomization
mv k8s/overlays/production/kustomization.yaml.bak k8s/overlays/production/kustomization.yaml