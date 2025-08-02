# DeusExMachina Scripts

This directory contains utility scripts for managing and testing the DeusExMachina application.

## Email Configuration Scripts

### update-sendgrid-key.sh
Updates the SendGrid API key in Google Secret Manager.

**Usage:**
```bash
./scripts/update-sendgrid-key.sh
```

**Prerequisites:**
1. You must have a SendGrid account and API key
2. You must be authenticated with gcloud (`gcloud auth login`)
3. You must have permissions to update secrets in the project

**Getting a SendGrid API Key:**
1. Sign up or log in at https://sendgrid.com
2. Navigate to Settings → API Keys
3. Click "Create API Key"
4. Name it (e.g., "DeusExMachina Production")
5. Select "Full Access" permissions
6. Copy the key (starts with `SG.`)

### test-email-sending.sh
Tests the email sending functionality by creating a test signup.

**Usage:**
```bash
./scripts/test-email-sending.sh
```

This will:
1. Generate a test email address
2. Attempt to sign up a new user
3. Report whether the signup was successful
4. Provide instructions for verifying email delivery

## Important Notes

- **Never commit API keys** to version control
- **Always use Secret Manager** for sensitive values in production
- **Rotate API keys regularly** for security
- **Monitor SendGrid usage** to avoid unexpected charges

## Troubleshooting

If emails are not being sent:

1. **Check the API key is valid:**
   - Log into SendGrid and verify the key exists
   - Ensure it has full access permissions

2. **Verify sender authentication:**
   - SendGrid requires domain or single sender verification
   - Check Settings → Sender Authentication in SendGrid

3. **Check Cloud Functions logs:**
   ```bash
   gcloud functions logs read auth-function --limit=50 --project=deus-ex-machina-prod
   ```

4. **Common error messages:**
   - `403 Permission denied` - Invalid API key
   - `401 Unauthorized` - API key not set or incorrect
   - `Failed to send email` - Various issues, check detailed error

5. **Verify the secret was updated:**
   ```bash
   gcloud secrets versions list sendgrid-api-key --project=deus-ex-machina-prod
   ```

## SendGrid Setup Checklist

- [ ] Create SendGrid account
- [ ] Generate API key with full access
- [ ] Complete sender authentication (domain or single sender)
- [ ] Update the secret using `update-sendgrid-key.sh`
- [ ] Test with `test-email-sending.sh`
- [ ] Monitor initial sends in SendGrid dashboard
- [ ] Set up email templates if needed