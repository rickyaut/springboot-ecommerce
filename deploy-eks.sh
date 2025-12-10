#!/bin/bash

# EKS Deployment Script
set -e

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION="us-west-2"
CLUSTER_NAME="ecommerce-cluster"

echo "Deploying to EKS cluster: $CLUSTER_NAME"
echo "Account ID: $ACCOUNT_ID"
echo "Region: $REGION"

# Update kubeconfig
aws eks update-kubeconfig --region $REGION --name $CLUSTER_NAME

# Update kustomization with actual values
sed -i "s/ACCOUNT_ID/$ACCOUNT_ID/g" k8s/overlays/eks/kustomization.yaml
sed -i "s/REGION/$REGION/g" k8s/overlays/eks/kustomization.yaml

# Deploy using kustomize
echo "Deploying to EKS..."
kubectl apply -k k8s/overlays/eks

# Wait for services to be ready
echo "Waiting for services to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/order-service
kubectl wait --for=condition=available --timeout=300s deployment/payment-service
kubectl wait --for=condition=available --timeout=300s deployment/erp-service

# Get service URLs
echo "Getting service URLs..."
kubectl get services

echo "Deployment completed successfully!"
echo "Check pod status: kubectl get pods"
echo "Check logs: kubectl logs -f deployment/order-service"