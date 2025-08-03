#!/bin/bash
# Script to configure GitHub secrets from Terraform outputs

set -e

echo "Configuring GitHub Secrets from Terraform Outputs"
echo "================================================"
echo ""

# Check prerequisites
if ! command -v gh &> /dev/null; then
    echo "Error: GitHub CLI (gh) is not installed."
    echo "Install from: https://cli.github.com/"
    exit 1
fi

if ! gh auth status &>/dev/null; then
    echo "Error: GitHub CLI is not authenticated."
    echo "Run: gh auth login"
    exit 1
fi

if ! command -v terraform &> /dev/null; then
    echo "Error: Terraform is not installed."
    exit 1
fi

# Get the project ID
PROJECT_ID=${GCP_PROJECT_ID:-deus-ex-machina-prod}
echo "Using project: $PROJECT_ID"
echo ""

# Navigate to Terraform directory
cd infrastructure/environments/prod

# Get Terraform outputs
echo "Getting values from Terraform..."
terraform init -backend-config="bucket=${PROJECT_ID}-terraform-state" >/dev/null 2>&1

# Get email topic name from Terraform
EMAIL_TOPIC_NAME=$(terraform output -raw email_topic_name 2>/dev/null || echo "")
if [ -z "$EMAIL_TOPIC_NAME" ]; then
    echo "Warning: Could not get email_topic_name from Terraform"
    echo "Using default: deus-ex-machina-email-events"
    EMAIL_TOPIC_NAME="deus-ex-machina-email-events"
fi

# Back to root directory
cd ../../../

echo ""
echo "Values from infrastructure:"
echo "- EMAIL_TOPIC_NAME: $EMAIL_TOPIC_NAME"
echo ""

# Configure EMAIL_TOPIC_NAME secret
echo "Setting EMAIL_TOPIC_NAME secret..."
echo "$EMAIL_TOPIC_NAME" | gh secret set EMAIL_TOPIC_NAME
echo "✓ EMAIL_TOPIC_NAME configured with: $EMAIL_TOPIC_NAME"

# Generate JWT secret if needed
if ! gh secret list | grep -q "^JWT_SECRET"; then
    echo ""
    echo "Generating JWT_SECRET..."
    JWT_SECRET=$(openssl rand -base64 32)
    echo "$JWT_SECRET" | gh secret set JWT_SECRET
    echo "✓ JWT_SECRET configured"
else
    echo "✓ JWT_SECRET already exists"
fi

echo ""
echo "Configuration complete!"
echo ""
echo "Current secrets:"
gh secret list

echo ""
echo "Note: The EMAIL_TOPIC_NAME value is retrieved from your Terraform infrastructure."
echo "This ensures consistency between what's deployed and what the services expect."