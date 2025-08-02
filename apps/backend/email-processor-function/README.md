# Email Processor Function

This Cloud Function processes email messages from the Pub/Sub queue and sends them using Gmail SMTP.

## Setup

### 1. Gmail App Password

To send emails via Gmail SMTP, you need to create an app-specific password:

1. Go to your Google Account settings: https://myaccount.google.com/
2. Navigate to Security → 2-Step Verification (must be enabled)
3. Scroll down and click on "App passwords"
4. Select "Mail" and your device
5. Generate the password and save it securely

### 2. GitHub Secrets

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

- `SMTP_FROM_EMAIL`: Your Gmail address (e.g., `noreply@yourdomain.com`)
- `SMTP_USERNAME`: Same as SMTP_FROM_EMAIL
- `SMTP_PASSWORD`: The app password generated in step 1

### 3. Deployment

The function is automatically deployed via GitHub Actions when changes are pushed to the main branch.

To deploy manually:

```bash
cd apps/backend/email-processor-function
./gradlew build

gcloud functions deploy email-processor-function \
  --gen2 \
  --runtime=java21 \
  --region=us-central1 \
  --source=. \
  --entry-point=com.deusexmachina.email.processor.EmailProcessorFunction \
  --trigger-topic=deus-ex-machina-email-events \
  --memory=512Mi \
  --max-instances=50 \
  --timeout=300s \
  --service-account=email-processor@deus-ex-machina-prod.iam.gserviceaccount.com \
  --set-env-vars="SMTP_FROM_EMAIL=your-email@gmail.com,SMTP_FROM_NAME=DeusExMachina,SMTP_USERNAME=your-email@gmail.com,SMTP_PASSWORD=your-app-password"
```

## Architecture

1. **Trigger**: Pub/Sub messages from `deus-ex-machina-email-events` topic
2. **Processing**: Parses email message, renders template, sends via Gmail SMTP
3. **Retries**: Automatic retries with exponential backoff
4. **Dead Letter**: Failed messages go to DLQ after 3 attempts

## Email Types Supported

- `VERIFICATION_EMAIL`: Account email verification
- `PASSWORD_RESET`: Password reset requests
- `PASSWORD_CHANGED`: Password change notifications
- `NEW_DEVICE_LOGIN`: New device login alerts
- `MFA_CODE`: Multi-factor authentication codes
- `ACCOUNT_LOCKED`: Account lockout notifications

## Testing

To test the email service:

1. Publish a test message to the Pub/Sub topic:

```bash
gcloud pubsub topics publish deus-ex-machina-email-events \
  --message='{
    "messageId": "test-123",
    "timestamp": "2024-01-01T12:00:00Z",
    "emailType": "VERIFICATION_EMAIL",
    "recipient": {
      "email": "test@example.com",
      "displayName": "Test User"
    },
    "templateData": {
      "actionUrl": "https://app.deusexmachina.com/verify?token=abc123",
      "token": "123456",
      "expiryTime": "2024-01-01T13:00:00Z"
    },
    "metadata": {
      "source": "test",
      "correlationId": "test-correlation-123",
      "priority": "HIGH"
    }
  }'
```

2. Check Cloud Function logs:

```bash
gcloud functions logs read email-processor-function --limit=50
```

## Monitoring

- **Logs**: Cloud Logging shows all email processing events
- **Metrics**: Cloud Monitoring tracks success/failure rates
- **Alerts**: Set up alerts for high failure rates or DLQ messages

## Troubleshooting

1. **Authentication Failed**: Check SMTP credentials in environment variables
2. **Connection Timeout**: Verify firewall rules allow outbound SMTP (port 587)
3. **Invalid Email**: Check email validation and template rendering
4. **Rate Limits**: Gmail has sending limits (500/day for regular accounts)