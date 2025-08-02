# Email Service Configuration

## Overview

The DeusExMachina authentication system uses SendGrid for sending transactional emails including:
- Email verification
- Password reset
- New device login notifications
- Multi-factor authentication codes
- Account security alerts

## Configuration Requirements

### 1. SendGrid Account Setup

1. Create a SendGrid account at https://sendgrid.com
2. Generate an API key with full access permissions
3. Configure sender authentication (domain verification)
4. Create dynamic templates for each email type

### 2. Environment Variables

The auth function requires the following environment variables:

```bash
# Required
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxx  # Your SendGrid API key
EMAIL_FROM_ADDRESS=noreply@yourdomain.com  # Verified sender email
EMAIL_FROM_NAME=YourAppName               # Display name for emails
APP_BASE_URL=https://yourdomain.com        # Base URL for email links

# Optional (uses defaults if not set)
GOOGLE_CLIENT_ID=xxxxx.apps.googleusercontent.com  # For OAuth integration
```

### 3. Google Cloud Secret Manager

For production deployments, sensitive values are stored in Secret Manager:

```bash
# Create SendGrid API key secret
echo -n "YOUR_SENDGRID_API_KEY" | gcloud secrets create sendgrid-api-key \
  --data-file=- \
  --project=your-project-id

# Grant Cloud Functions access to the secret
gcloud projects add-iam-policy-binding your-project-id \
  --member="serviceAccount:PROJECT_NUMBER-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### 4. SendGrid Template IDs

The following template IDs are used by the system (defined in SendGridEmailService.java):

- `d-verification-template` - Email verification template
- `d-password-reset-template` - Password reset template
- `d-new-device-template` - New device login notification
- `d-mfa-code-template` - Multi-factor authentication code
- `d-account-locked-template` - Account locked notification

Each template should include the following variables:
- `{{userEmail}}` - User's email address
- `{{userName}}` - User's display name
- `{{actionUrl}}` - Link for the action (verification, reset, etc.)
- `{{code}}` - Verification or MFA code (where applicable)
- `{{deviceInfo}}` - Device/browser information (for security emails)
- `{{timestamp}}` - Time of the event

### 5. Terraform Configuration

The email configuration is managed through Terraform in the auth module:

```hcl
# In infrastructure/environments/prod/main.tf or similar
module "auth" {
  source = "../../modules/auth"
  
  # ... other configuration ...
  
  email_from_address = "noreply@yourdomain.com"
  email_from_name    = "YourAppName"
  app_base_url       = "https://yourdomain.com"
}
```

### 6. Local Development

For local development, you can use environment variables:

```bash
# Create a .env file in the auth-function directory
SENDGRID_API_KEY=SG.test-key-for-development
EMAIL_FROM_ADDRESS=test@localhost
EMAIL_FROM_NAME=DevTest
APP_BASE_URL=http://localhost:3000
```

**Note**: Never commit .env files or API keys to version control.

## Troubleshooting

### Common Issues

1. **403 Permission Denied from SendGrid**
   - Verify the API key is correct
   - Check that the API key has full access permissions
   - Ensure sender authentication is completed

2. **Emails not being received**
   - Check SendGrid activity feed for bounces/blocks
   - Verify the from address is authenticated
   - Check spam folders
   - Ensure templates exist with correct IDs

3. **Template errors**
   - Verify all required variables are passed
   - Check template IDs match the code
   - Ensure templates are active in SendGrid

### Monitoring

Monitor email delivery through:
1. SendGrid dashboard - Activity feed and statistics
2. Cloud Functions logs - For function-level errors
3. Application monitoring - For end-to-end tracking

## Security Considerations

1. **API Key Protection**
   - Always use Secret Manager in production
   - Rotate API keys regularly
   - Use least-privilege API keys when possible

2. **Email Security**
   - Implement SPF, DKIM, and DMARC records
   - Use HTTPS for all links in emails
   - Include security headers in email templates
   - Time-limit sensitive links (password reset, verification)

3. **Rate Limiting**
   - Implement rate limiting on email-sending endpoints
   - Monitor for unusual sending patterns
   - Set up alerts for high-volume sending

## Testing

1. **Unit Tests**
   - Mock SendGrid client in tests
   - Test email content generation
   - Verify error handling

2. **Integration Tests**
   - Use SendGrid sandbox mode for testing
   - Verify end-to-end email flow
   - Test template rendering with various data

3. **Manual Testing**
   - Test with real email addresses
   - Verify emails render correctly across clients
   - Test all email types and edge cases