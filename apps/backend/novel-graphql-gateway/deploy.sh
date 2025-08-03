#!/bin/bash

# Deploy Novel GraphQL Gateway to Cloud Run

PROJECT_ID="deus-ex-machina-prod"
SERVICE_NAME="novel-graphql-gateway"
REGION="us-central1"

# Build the Docker image
echo "Building Docker image..."
docker build -t gcr.io/${PROJECT_ID}/${SERVICE_NAME} .

# Push to Google Container Registry
echo "Pushing to GCR..."
docker push gcr.io/${PROJECT_ID}/${SERVICE_NAME}

# Deploy to Cloud Run
echo "Deploying to Cloud Run..."
gcloud run deploy ${SERVICE_NAME} \
  --image gcr.io/${PROJECT_ID}/${SERVICE_NAME} \
  --platform managed \
  --region ${REGION} \
  --allow-unauthenticated \
  --set-env-vars="NODE_ENV=production" \
  --set-env-vars="JWT_SECRET=${JWT_SECRET}" \
  --set-env-vars="AUTH_SERVICE_URL=https://auth-function-xkv3zhqrha-uw.a.run.app" \
  --set-env-vars="DOCUMENT_SERVICE_URL=https://novel-document-service-xkv3zhqrha-uw.a.run.app" \
  --set-env-vars="MEMORY_SERVICE_URL=https://novel-memory-service-xkv3zhqrha-uw.a.run.app" \
  --set-env-vars="AI_SERVICE_URL=https://novel-ai-service-xkv3zhqrha-uw.a.run.app" \
  --set-env-vars="ALLOWED_ORIGINS=https://god-in-a-box.com,https://novel-creator.deusexmachina.app,https://deusexmachina.app,http://localhost:3000,http://localhost:5173" \
  --memory=512Mi \
  --cpu=1 \
  --timeout=60 \
  --concurrency=100 \
  --max-instances=10

echo "Deployment complete!"
echo "Service URL: https://${SERVICE_NAME}-xkv3zhqrha-uc.a.run.app"