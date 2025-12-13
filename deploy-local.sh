#!/bin/bash

echo "Deploying to local Kubernetes..."

# Check if kubectl is available and cluster is running
if ! command -v kubectl &> /dev/null; then
    echo "Error: kubectl is not installed"
    echo "Install kubectl: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

if ! kubectl cluster-info &> /dev/null; then
    echo "Error: No Kubernetes cluster is running"
    echo ""
    echo "Start a local cluster with one of these options:"
    echo "1. Docker Desktop: Enable Kubernetes in Docker Desktop settings"
    echo "2. Minikube: minikube start"
    echo "3. Kind: kind create cluster"
    echo ""
    echo "Then run this script again."
    exit 1
fi

echo "Connected to cluster: $(kubectl config current-context)"

# Build Docker images for local Kubernetes
echo "Building Docker images..."

# Check if using minikube and configure Docker environment
if kubectl config current-context | grep -q minikube; then
    echo "Using minikube - configuring Docker environment"
    eval $(minikube docker-env)
fi

# Build images
./build.sh

# For Docker Desktop Kubernetes, load images
if kubectl config current-context | grep -q docker-desktop; then
    echo "Using Docker Desktop - images should be available locally"
fi

# Deploy using kustomize
echo "Deploying to Kubernetes..."
kubectl apply -k k8s/overlays/local

# Restart deployments to apply imagePullPolicy changes
echo "Restarting deployments to apply image pull policy..."
kubectl rollout restart deployment/order-service -n ecommerce 2>/dev/null || true
kubectl rollout restart deployment/payment-service -n ecommerce 2>/dev/null || true
kubectl rollout restart deployment/erp-service -n ecommerce 2>/dev/null || true

# Wait for infrastructure services
echo "Waiting for infrastructure services..."
kubectl wait --for=condition=available --timeout=300s deployment/postgres-order -n ecommerce
kubectl wait --for=condition=available --timeout=300s deployment/postgres-payment -n ecommerce
kubectl wait --for=condition=available --timeout=300s deployment/kafka -n ecommerce
kubectl wait --for=condition=available --timeout=300s deployment/vault -n ecommerce

# Check pod status before waiting
echo
echo "Current pod status:"
kubectl get pods -n ecommerce

# Wait for application services with shorter timeout
echo
echo "Waiting for application services..."
echo "Waiting for order-service..."
if ! kubectl wait --for=condition=available --timeout=120s deployment/order-service -n ecommerce; then
    echo "Order service failed to start. Checking logs:"
    kubectl logs -l app=order-service -n ecommerce --tail=50
    echo
    echo "Pod describe:"
    kubectl describe pods -l app=order-service -n ecommerce
    exit 1
fi

echo "Waiting for payment-service..."
if ! kubectl wait --for=condition=available --timeout=120s deployment/payment-service -n ecommerce; then
    echo "Payment service failed to start. Checking logs:"
    kubectl logs -l app=payment-service -n ecommerce --tail=50
    exit 1
fi

echo "Waiting for erp-service..."
if ! kubectl wait --for=condition=available --timeout=120s deployment/erp-service -n ecommerce; then
    echo "ERP service failed to start. Checking logs:"
    kubectl logs -l app=erp-service -n ecommerce --tail=50
    exit 1
fi

# Show service status
echo
echo "Deployment Status:"
kubectl get pods -n ecommerce

echo
echo "Setting up port forwarding for local access..."
echo "Note: NodePort doesn't work on macOS Docker Desktop. Using port-forward instead."

# Kill any existing port-forward processes
pkill -f "kubectl port-forward" 2>/dev/null || true

# Start port forwarding in background
echo "Starting port forwarding..."
kubectl port-forward svc/order-service 8080:8080 -n ecommerce > /dev/null 2>&1 &
ORDER_PF_PID=$!
kubectl port-forward svc/vault 8200:8200 -n ecommerce > /dev/null 2>&1 &
VAULT_PF_PID=$!
kubectl port-forward svc/zipkin 9411:9411 -n ecommerce > /dev/null 2>&1 &
ZIPKIN_PF_PID=$!

# Wait a moment for port forwarding to establish
sleep 3

echo
echo "Service URLs (via port-forward):"
echo "Order Service: http://localhost:8080"
echo "Vault UI: http://localhost:8200 (token: myroot)"
echo "Zipkin: http://localhost:9411"

echo
echo "Port-forward PIDs: Order=$ORDER_PF_PID, Vault=$VAULT_PF_PID, Zipkin=$ZIPKIN_PF_PID"
echo "To stop port forwarding: kill $ORDER_PF_PID $VAULT_PF_PID $ZIPKIN_PF_PID"
echo "Or run: pkill -f 'kubectl port-forward'"

echo
echo "Deployment completed successfully!"
echo "Monitor with: kubectl get pods -n ecommerce -w"