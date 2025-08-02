#!/bin/bash
set -e

# Deploy auth-function to Google Cloud Functions
# This script updates the function with the correct environment variables and secrets

PROJECT_ID="deus-ex-machina-prod"
REGION="us-central1"
FUNCTION_NAME="auth-function"

echo "Deploying $FUNCTION_NAME to $PROJECT_ID in $REGION..."

# Check if the function exists
if gcloud functions describe $FUNCTION_NAME --region=$REGION --project=$PROJECT_ID >/dev/null 2>&1; then
    echo "Function exists, updating configuration..."
    
    # Update the function with environment variables and secrets
    # Note: Now using Cloud Pub/Sub for email delivery instead of SendGrid
    gcloud functions deploy $FUNCTION_NAME \
        --project=$PROJECT_ID \
        --region=$REGION \
        --update-env-vars="EMAIL_FROM_ADDRESS=noreply@deusexmachina.app,EMAIL_FROM_NAME=DeusExMachina,APP_BASE_URL=https://34.95.119.251,GCP_PROJECT_ID=$PROJECT_ID,EMAIL_TOPIC_NAME=deusexmachina-email-events" \
        --remove-env-vars="SENDGRID_API_KEY" \
        --quiet
        
    echo "Function configuration updated successfully!"
else
    echo "Error: Function $FUNCTION_NAME does not exist in $PROJECT_ID"
    echo "The function needs to be created first through the full deployment process"
    exit 1
fi

echo "Deployment complete!"