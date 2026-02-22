#!/bin/bash

# Deploy all Kubernetes resources

echo "Creating namespace..."
kubectl apply -f k8s/deployment.yaml

echo "Deploying PostgreSQL..."
kubectl apply -f k8s/postgres.yaml

echo "Deploying Redis..."
kubectl apply -f k8s/redis.yaml

echo "Waiting for database to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n url-shortener --timeout=120s

echo "Waiting for Redis to be ready..."
kubectl wait --for=condition=ready pod -l app=redis -n url-shortener --timeout=60s

echo "Deploying application..."
kubectl rollout status deployment/url-shortener -n url-shortener

echo ""
echo "Deployment complete!"
echo ""
echo "Get service URL:"
echo "kubectl get svc app-service -n url-shortener"
echo ""
echo "View pods:"
echo "kubectl get pods -n url-shortener"
echo ""
echo "View logs:"
echo "kubectl logs -f deployment/url-shortener -n url-shortener"
