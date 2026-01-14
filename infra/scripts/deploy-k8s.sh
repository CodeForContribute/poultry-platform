#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

NAMESPACE="poultry-platform"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$SCRIPT_DIR/../k8s"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Poultry Platform - Kubernetes Deployment${NC}"
echo -e "${GREEN}========================================${NC}"

# Check prerequisites
echo -e "\n${YELLOW}Checking prerequisites...${NC}"

command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}kubectl is required but not installed. Aborting.${NC}"; exit 1; }
echo -e "${GREEN}✓ kubectl is installed${NC}"

# Check cluster connection
if ! kubectl cluster-info >/dev/null 2>&1; then
    echo -e "${RED}Cannot connect to Kubernetes cluster. Please check your kubeconfig.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Connected to Kubernetes cluster${NC}"

# Parse arguments
ACTION=${1:-apply}
IMAGE_TAG=${2:-latest}

case $ACTION in
    apply|deploy)
        echo -e "\n${YELLOW}Deploying to Kubernetes...${NC}"

        # Apply with Kustomize
        kubectl apply -k "$K8S_DIR"

        # Update image tag if provided
        if [ "$IMAGE_TAG" != "latest" ]; then
            echo -e "\n${YELLOW}Updating image tag to: $IMAGE_TAG${NC}"
            kubectl set image deployment/poultry-backend \
                backend=poultry-platform/backend:$IMAGE_TAG \
                -n $NAMESPACE
        fi

        # Wait for rollout
        echo -e "\n${YELLOW}Waiting for deployment to complete...${NC}"
        kubectl rollout status deployment/poultry-backend -n $NAMESPACE --timeout=300s

        echo -e "\n${GREEN}✓ Deployment completed successfully${NC}"
        ;;

    delete|destroy)
        echo -e "\n${YELLOW}Deleting resources from Kubernetes...${NC}"
        kubectl delete -k "$K8S_DIR" --ignore-not-found
        echo -e "\n${GREEN}✓ Resources deleted${NC}"
        ;;

    status)
        echo -e "\n${YELLOW}Checking deployment status...${NC}"
        kubectl get all -n $NAMESPACE
        echo -e "\n${YELLOW}Pod status:${NC}"
        kubectl get pods -n $NAMESPACE -o wide
        ;;

    logs)
        POD_NAME=$(kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=poultry-backend -o jsonpath='{.items[0].metadata.name}')
        echo -e "\n${YELLOW}Showing logs for: $POD_NAME${NC}"
        kubectl logs -f $POD_NAME -n $NAMESPACE
        ;;

    *)
        echo -e "Usage: $0 {apply|delete|status|logs} [image-tag]"
        echo -e "  apply|deploy  - Deploy or update the application"
        echo -e "  delete|destroy - Remove all resources"
        echo -e "  status        - Show deployment status"
        echo -e "  logs          - Stream application logs"
        exit 1
        ;;
esac

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Done!${NC}"
echo -e "${GREEN}========================================${NC}"
