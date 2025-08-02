#!/bin/bash
set -e

# Script to update SendGrid API key in Google Secret Manager
# This ensures the key is updated securely without exposing it in shell history

PROJECT_ID="deus-ex-machina-prod"
SECRET_NAME="sendgrid-api-key"

echo "=== SendGrid API Key Update Script ==="
echo ""
echo "This script will help you update the SendGrid API key in Google Secret Manager."
echo "Make sure you have your SendGrid API key ready (it should start with 'SG.')."
echo ""

# Check if user is authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
    echo "Error: You are not authenticated with gcloud. Please run 'gcloud auth login' first."
    exit 1
fi

# Confirm project
echo "Project: $PROJECT_ID"
echo -n "Is this correct? (y/N): "
read -r confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

# Read API key securely
echo ""
echo "Please enter your SendGrid API key (input will be hidden):"
read -s -r SENDGRID_KEY
echo ""

# Validate key format
if [[ ! "$SENDGRID_KEY" =~ ^SG\. ]]; then
    echo "Error: Invalid API key format. SendGrid API keys should start with 'SG.'"
    exit 1
fi

# Create new version
echo "Adding new secret version..."
if echo -n "$SENDGRID_KEY" | gcloud secrets versions add "$SECRET_NAME" \
    --data-file=- \
    --project="$PROJECT_ID"; then
    echo "✓ Successfully updated SendGrid API key!"
    echo ""
    echo "The auth function will automatically use the new key on its next invocation."
    echo ""
    echo "To verify emails are working:"
    echo "1. Try signing up with a new account"
    echo "2. Check the SendGrid dashboard for activity"
    echo "3. Check Cloud Functions logs for any errors"
else
    echo "✗ Failed to update secret. Please check your permissions and try again."
    exit 1
fi