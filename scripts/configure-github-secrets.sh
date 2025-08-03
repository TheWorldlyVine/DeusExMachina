#!/bin/bash
# Script to configure GitHub secrets for the DeusExMachina project

echo "GitHub Secrets Configuration"
echo "============================"
echo ""

# Check if GitHub CLI is installed and authenticated
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

# Generate JWT secret if needed
if ! gh secret list | grep -q "^JWT_SECRET"; then
    echo "Generating JWT_SECRET..."
    JWT_SECRET=$(openssl rand -base64 32)
    echo "$JWT_SECRET" | gh secret set JWT_SECRET
    echo "✓ JWT_SECRET configured"
else
    echo "✓ JWT_SECRET already exists"
fi

# Set EMAIL_TOPIC
if ! gh secret list | grep -q "^EMAIL_TOPIC"; then
    echo "Setting EMAIL_TOPIC..."
    echo "deus-ex-machina-email-events" | gh secret set EMAIL_TOPIC
    echo "✓ EMAIL_TOPIC configured"
else
    echo "✓ EMAIL_TOPIC already exists"
fi

echo ""
echo "Configuration complete!"
echo ""
echo "Current secrets:"
gh secret list