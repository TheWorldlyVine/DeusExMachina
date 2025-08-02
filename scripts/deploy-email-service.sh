#!/bin/bash
set -e

# Deploy Email Service Infrastructure
# This script deploys the Application Integration workflow for email processing

PROJECT_ID="deus-ex-machina-prod"
REGION="us-central1"
WORKFLOW_FILE="../infrastructure/modules/email-service/application-integration-workflow.yaml"

echo "=== Email Service Deployment Script ==="
echo ""
echo "This script will deploy the Application Integration workflow for email processing."
echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo ""

# Check if user is authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
    echo "Error: You are not authenticated with gcloud. Please run 'gcloud auth login' first."
    exit 1
fi

# Check if Application Integration API is enabled
echo "Checking if Application Integration API is enabled..."
if ! gcloud services list --enabled --filter="name:integrations.googleapis.com" --format="value(name)" | grep -q "integrations"; then
    echo "Enabling Application Integration API..."
    gcloud services enable integrations.googleapis.com --project="$PROJECT_ID"
    echo "Waiting for API to be enabled..."
    sleep 30
fi

# Get the service account email
SERVICE_ACCOUNT="email-processor@$PROJECT_ID.iam.gserviceaccount.com"

# Check if service account exists
echo "Checking service account..."
if ! gcloud iam service-accounts describe "$SERVICE_ACCOUNT" --project="$PROJECT_ID" >/dev/null 2>&1; then
    echo "Error: Service account $SERVICE_ACCOUNT not found."
    echo "Please run 'terraform apply' first to create the infrastructure."
    exit 1
fi

# Deploy the Application Integration workflow
echo ""
echo "Deploying Application Integration workflow..."
echo "Note: This uses the gcloud alpha command as Application Integration is in preview"
echo ""

# Create the integration
if gcloud alpha integrations list --location="$REGION" --project="$PROJECT_ID" 2>/dev/null | grep -q "email-processor"; then
    echo "Updating existing email-processor integration..."
    gcloud alpha integrations update email-processor \
        --project="$PROJECT_ID" \
        --location="$REGION" \
        --service-account="$SERVICE_ACCOUNT" \
        --integration-file="$WORKFLOW_FILE" \
        --quiet
else
    echo "Creating new email-processor integration..."
    gcloud alpha integrations create email-processor \
        --project="$PROJECT_ID" \
        --location="$REGION" \
        --service-account="$SERVICE_ACCOUNT" \
        --integration-file="$WORKFLOW_FILE" \
        --quiet
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Application Integration workflow deployed successfully!"
    echo ""
    echo "Next steps:"
    echo "1. The workflow will automatically process emails from the Pub/Sub topic"
    echo "2. Monitor the workflow at: https://console.cloud.google.com/integrations/list?project=$PROJECT_ID"
    echo "3. Check logs at: gcloud logging read 'resource.type=application_integration' --limit=50"
    echo ""
    echo "To test the email service:"
    echo "1. Deploy the updated auth function with CloudPubSubEmailService"
    echo "2. Try creating a new user account to trigger a verification email"
    echo "3. Check the Application Integration logs for processing details"
else
    echo ""
    echo "✗ Deployment failed. Please check the error messages above."
    echo ""
    echo "Common issues:"
    echo "1. Application Integration API not enabled"
    echo "2. Missing permissions for the service account"
    echo "3. Invalid workflow YAML syntax"
    exit 1
fi