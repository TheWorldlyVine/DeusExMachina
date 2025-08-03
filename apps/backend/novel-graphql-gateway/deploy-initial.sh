#!/bin/bash

# Initial deployment script for Novel GraphQL Gateway
# This creates the Cloud Run service for the first time

PROJECT_ID="deus-ex-machina-prod"
SERVICE_NAME="novel-graphql-gateway"
REGION="us-central1"

echo "Creating initial Cloud Run service for GraphQL Gateway..."

# Deploy a simple "hello world" container first to create the service
gcloud run deploy ${SERVICE_NAME} \
  --image gcr.io/cloudrun/hello \
  --platform managed \
  --region ${REGION} \
  --allow-unauthenticated \
  --project ${PROJECT_ID}

echo "Initial service created. The CI/CD pipeline will deploy the actual application."
echo "Service URL: https://${SERVICE_NAME}-xkv3zhqrha-uc.a.run.app"