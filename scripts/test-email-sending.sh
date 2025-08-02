#!/bin/bash
set -e

# Script to test email sending functionality
# This will attempt to trigger a test email through the auth function

AUTH_FUNCTION_URL="https://auth-function-tbmcifixdq-uc.a.run.app"

echo "=== Email Sending Test Script ==="
echo ""
echo "This script will test the email sending functionality by attempting a signup."
echo ""

# Generate test email
TEST_EMAIL="test-$(date +%s)@example.com"
TEST_PASSWORD="TestPassword123!"

echo "Test email: $TEST_EMAIL"
echo ""

# Attempt signup
echo "Sending signup request..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_FUNCTION_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\",
    \"displayName\": \"Test User\",
    \"acceptedTerms\": true
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Response code: $HTTP_CODE"
echo "Response body: $BODY"
echo ""

if [[ "$HTTP_CODE" == "201" ]]; then
    echo "✓ Signup successful!"
    echo ""
    echo "Check the following to verify email sending:"
    echo "1. SendGrid Dashboard (https://app.sendgrid.com) - Activity feed"
    echo "2. Cloud Functions logs:"
    echo "   gcloud functions logs read auth-function --limit=50 --project=deus-ex-machina-prod"
    echo ""
    echo "If you see 'Failed to send email' errors, the API key may be incorrect."
else
    echo "✗ Signup failed. This could be due to:"
    echo "- Network issues"
    echo "- Function not deployed correctly"
    echo "- Other validation errors"
    echo ""
    echo "Check the logs for more details:"
    echo "gcloud functions logs read auth-function --limit=50 --project=deus-ex-machina-prod"
fi