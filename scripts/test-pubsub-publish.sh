#!/bin/bash
set -e

PROJECT_ID="deus-ex-machina-prod"
TOPIC="deus-ex-machina-email-events"

echo "Testing Pub/Sub publish to topic: $TOPIC"

# Create a test message
TEST_MESSAGE=$(cat <<EOF
{
  "messageId": "test-$(date +%s)",
  "emailType": "VERIFICATION_EMAIL",
  "recipient": {
    "email": "test@example.com",
    "displayName": "Test User"
  },
  "templateData": {
    "actionUrl": "https://example.com/verify",
    "token": "test-token"
  },
  "metadata": {
    "source": "test-script",
    "correlationId": "test-$(date +%s)",
    "priority": "HIGH"
  }
}
EOF
)

echo "Publishing test message..."
gcloud pubsub topics publish $TOPIC \
  --message="$TEST_MESSAGE" \
  --attribute="emailType=VERIFICATION_EMAIL,source=test-script" \
  --project=$PROJECT_ID

echo ""
echo "Checking subscription for messages..."
gcloud pubsub subscriptions pull deus-ex-machina-email-events-sub \
  --auto-ack \
  --limit=5 \
  --project=$PROJECT_ID