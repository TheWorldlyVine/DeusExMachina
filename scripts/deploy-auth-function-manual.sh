#!/bin/bash
set -e

# Manual deployment script for auth-function
# Use this for rapid debugging without waiting for CI/CD

PROJECT_ID="deus-ex-machina-prod"
REGION="us-central1"
FUNCTION_NAME="auth-function"
SOURCE_DIR="apps/backend/auth-function"

echo "=== Manual Auth Function Deployment ==="
echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo "Function: $FUNCTION_NAME"
echo ""

# Check if gcloud is authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
    echo "Error: Not authenticated. Run 'gcloud auth login' first."
    exit 1
fi

# Set the project
gcloud config set project $PROJECT_ID

echo "Building and deploying auth-function..."

# Deploy the function
gcloud functions deploy $FUNCTION_NAME \
    --gen2 \
    --runtime=java21 \
    --region=$REGION \
    --source=$SOURCE_DIR \
    --entry-point=com.deusexmachina.functions.AuthFunction \
    --trigger-http \
    --allow-unauthenticated \
    --memory=512Mi \
    --timeout=300s \
    --max-instances=100 \
    --set-env-vars="EMAIL_FROM_ADDRESS=noreply@deusexmachina.app,EMAIL_FROM_NAME=DeusExMachina,APP_BASE_URL=https://app.deusexmachina.com,GCP_PROJECT_ID=$PROJECT_ID,EMAIL_TOPIC_NAME=deus-ex-machina-email-events"

echo ""
echo "Deployment complete!"
echo ""
echo "Function URL: https://$REGION-$PROJECT_ID.cloudfunctions.net/$FUNCTION_NAME"
echo ""
echo "To view logs:"
echo "gcloud functions logs read $FUNCTION_NAME --region=$REGION --limit=50"
echo ""
echo "To test registration:"
echo "curl -X POST https://$REGION-$PROJECT_ID.cloudfunctions.net/$FUNCTION_NAME/auth/register \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"email\":\"test@example.com\",\"password\":\"TestPass123!\",\"name\":\"Test User\"}'"