#!/bin/bash
# Script to help configure GitHub secrets for the DeusExMachina project

echo "GitHub Secrets Configuration Guide"
echo "=================================="
echo ""
echo "This project requires the following GitHub secrets to be configured:"
echo ""
echo "1. GCP_PROJECT_ID - Your Google Cloud project ID (e.g., 'deus-ex-machina-prod')"
echo "2. GCP_SA_KEY - Service account JSON key for deployment"
echo "3. JWT_SECRET - Secret key for JWT token generation (generate a secure random string)"
echo "4. EMAIL_TOPIC_NAME - Pub/Sub topic name for email sending (e.g., 'deus-ex-machina-email-events')"
echo "5. SMTP_USERNAME - SendGrid API username (optional for email)"
echo "6. SMTP_PASSWORD - SendGrid API key (optional for email)"
echo "7. SMTP_FROM_EMAIL - From email address (optional for email)"
echo ""
echo "To configure these secrets:"
echo "1. Go to: https://github.com/TheWorldlyVine/DeusExMachina/settings/secrets/actions"
echo "2. Click 'New repository secret' for each secret"
echo "3. Enter the name and value"
echo ""
echo "To generate a secure JWT_SECRET, you can use:"
echo "  openssl rand -base64 32"
echo ""
echo "Current environment check:"
echo ""

# Check if we can access the GitHub API
if command -v gh &> /dev/null; then
    echo "GitHub CLI (gh) is installed. Checking configured secrets..."
    
    # List current secrets (names only, not values)
    if gh secret list 2>/dev/null; then
        echo "✓ GitHub CLI authenticated"
    else
        echo "✗ GitHub CLI not authenticated. Run: gh auth login"
    fi
else
    echo "✗ GitHub CLI not installed. Install from: https://cli.github.com/"
fi

echo ""
echo "Required secrets that need to be set:"
echo ""

# List of required secrets
REQUIRED_SECRETS=(
    "GCP_PROJECT_ID"
    "GCP_SA_KEY"
    "JWT_SECRET"
    "EMAIL_TOPIC_NAME"
)

# Check each secret
for secret in "${REQUIRED_SECRETS[@]}"; do
    if command -v gh &> /dev/null && gh auth status &>/dev/null; then
        if gh secret list | grep -q "^$secret"; then
            echo "✓ $secret is configured"
        else
            echo "✗ $secret is NOT configured"
        fi
    else
        echo "? $secret (unable to check - GitHub CLI not available)"
    fi
done

echo ""
echo "Optional email-related secrets:"
echo "- SMTP_USERNAME"
echo "- SMTP_PASSWORD"
echo "- SMTP_FROM_EMAIL"