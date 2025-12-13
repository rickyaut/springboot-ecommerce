#!/bin/bash

echo "=== Kubernetes Troubleshooting Script ==="
echo

# Function to check pod status
check_pods() {
    echo "📊 Current Pod Status:"
    kubectl get pods -n ecommerce
    echo
}

# Function to check resource usage
check_resources() {
    echo "💾 Resource Usage:"
    kubectl top pods -n ecommerce 2>/dev/null || echo "Metrics server not available"
    echo
}

# Function to check logs for a specific service
check_logs() {
    local service=$1
    echo "📋 Recent logs for $service:"
    local pod=$(kubectl get pods -n ecommerce -l app=$service -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    if [ -n "$pod" ]; then
        kubectl logs $pod -n ecommerce --tail=10
    else
        echo "No pod found for $service"
    fi
    echo
}

# Function to restart services
restart_services() {
    echo "🔄 Restarting all services..."
    kubectl rollout restart deployment/order-service -n ecommerce
    kubectl rollout restart deployment/payment-service -n ecommerce
    kubectl rollout restart deployment/erp-service -n ecommerce
    echo "Restart initiated. Wait 2-3 minutes for services to come up."
    echo
}

# Function to check service readiness
check_readiness() {
    echo "🏥 Health Check Status:"
    
    # Check if services are ready
    local order_ready=$(kubectl get pods -n ecommerce -l app=order-service -o jsonpath='{.items[0].status.containerStatuses[0].ready}' 2>/dev/null)
    local payment_ready=$(kubectl get pods -n ecommerce -l app=payment-service -o jsonpath='{.items[0].status.containerStatuses[0].ready}' 2>/dev/null)
    local erp_ready=$(kubectl get pods -n ecommerce -l app=erp-service -o jsonpath='{.items[0].status.containerStatuses[0].ready}' 2>/dev/null)
    
    echo "Order Service: ${order_ready:-false}"
    echo "Payment Service: ${payment_ready:-false}"
    echo "ERP Service: ${erp_ready:-false}"
    echo
}

# Function to show quick fixes
show_fixes() {
    echo "🔧 Quick Fixes:"
    echo "1. Restart services: ./k8s-troubleshoot.sh restart"
    echo "2. Check logs: ./k8s-troubleshoot.sh logs <service-name>"
    echo "3. Delete and redeploy: kubectl delete -k k8s/overlays/local && kubectl apply -k k8s/overlays/local"
    echo "4. Use Docker Compose instead: docker-compose up -d (recommended for local dev)"
    echo
}

# Main script logic
case "$1" in
    "restart")
        restart_services
        ;;
    "logs")
        if [ -n "$2" ]; then
            check_logs $2
        else
            echo "Usage: $0 logs <service-name>"
            echo "Available services: order-service, payment-service, erp-service"
        fi
        ;;
    "status")
        check_pods
        check_readiness
        ;;
    "resources")
        check_resources
        ;;
    *)
        echo "🚀 Kubernetes Service Status Check"
        echo "=================================="
        check_pods
        check_readiness
        check_resources
        show_fixes
        
        echo "💡 Recommendations:"
        echo "- Services are taking 2-3 minutes to start due to resource constraints"
        echo "- For faster development, use: docker-compose up -d"
        echo "- Monitor startup with: kubectl logs -f <pod-name> -n ecommerce"
        echo
        ;;
esac