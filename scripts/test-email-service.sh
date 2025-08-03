#!/bin/bash
set -e

# Test Email Service Script
# This script sends a test email through the Pub/Sub email processing pipeline

echo "=== Email Service Test Script ==="
echo ""

# Default values
PROJECT_ID="${PROJECT_ID:-deus-ex-machina-prod}"
TOPIC_NAME="deus-ex-machina-email-events"
TEST_EMAIL="${1:-test@example.com}"
EMAIL_TYPE="${2:-VERIFICATION_EMAIL}"

echo "Project: $PROJECT_ID"
echo "Topic: $TOPIC_NAME"
echo "Test Email: $TEST_EMAIL"
echo "Email Type: $EMAIL_TYPE"
echo ""

# Generate test data
MESSAGE_ID="test-$(date +%s)"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
EXPIRY_TIME=$(date -u -d "+1 hour" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -v+1H +"%Y-%m-%dT%H:%M:%SZ")
TOKEN=$(openssl rand -hex 3 | tr '[:lower:]' '[:upper:]')

# Create the message based on email type
case $EMAIL_TYPE in
    "VERIFICATION_EMAIL")
        MESSAGE=$(cat <<EOF
{
  "messageId": "$MESSAGE_ID",
  "timestamp": "$TIMESTAMP",
  "emailType": "VERIFICATION_EMAIL",
  "recipient": {
    "email": "$TEST_EMAIL",
    "displayName": "Test User"
  },
  "templateData": {
    "actionUrl": "https://god-in-a-box.com/verify?token=test-token-123",
    "token": "$TOKEN",
    "expiryTime": "$EXPIRY_TIME"
  },
  "metadata": {
    "source": "test-script",
    "correlationId": "$MESSAGE_ID",
    "priority": "HIGH"
  }
}
EOF
        )
        ;;
    
    "PASSWORD_RESET")
        MESSAGE=$(cat <<EOF
{
  "messageId": "$MESSAGE_ID",
  "timestamp": "$TIMESTAMP",
  "emailType": "PASSWORD_RESET",
  "recipient": {
    "email": "$TEST_EMAIL",
    "displayName": "Test User"
  },
  "templateData": {
    "actionUrl": "https://god-in-a-box.com/reset-password?token=test-reset-123",
    "token": "test-reset-123",
    "expiryTime": "$EXPIRY_TIME"
  },
  "metadata": {
    "source": "test-script",
    "correlationId": "$MESSAGE_ID",
    "priority": "HIGH"
  }
}
EOF
        )
        ;;
    
    "MFA_CODE")
        MESSAGE=$(cat <<EOF
{
  "messageId": "$MESSAGE_ID",
  "timestamp": "$TIMESTAMP",
  "emailType": "MFA_CODE",
  "recipient": {
    "email": "$TEST_EMAIL",
    "displayName": "Test User"
  },
  "templateData": {
    "code": "$TOKEN",
    "expiryTime": "$EXPIRY_TIME"
  },
  "metadata": {
    "source": "test-script",
    "correlationId": "$MESSAGE_ID",
    "priority": "HIGH"
  }
}
EOF
        )
        ;;
    
    *)
        echo "Error: Unknown email type: $EMAIL_TYPE"
        echo "Supported types: VERIFICATION_EMAIL, PASSWORD_RESET, MFA_CODE"
        exit 1
        ;;
esac

echo "Publishing test message..."
echo ""
echo "Message content:"
echo "$MESSAGE" | jq '.' 2>/dev/null || echo "$MESSAGE"
echo ""

# Publish the message
if gcloud pubsub topics publish "$TOPIC_NAME" \
    --project="$PROJECT_ID" \
    --message="$MESSAGE"; then
    
    echo "✓ Test message published successfully!"
    echo ""
    echo "Message ID: $MESSAGE_ID"
    echo ""
    echo "To check the results:"
    echo "1. Check Pub/Sub subscription for pending messages:"
    echo "   gcloud pubsub subscriptions pull deus-ex-machina-email-events-sub --auto-ack --limit=1"
    echo ""
    echo "2. Check Cloud Function logs:"
    echo "   gcloud functions logs read email-processor-function --limit=50"
    echo ""
    echo "3. Check if email was received at: $TEST_EMAIL"
else
    echo "✗ Failed to publish test message"
    exit 1
fi