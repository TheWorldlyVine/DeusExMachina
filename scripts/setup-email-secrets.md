# Setting up Email Service Secrets

## Steps to Add GitHub Secrets

1. **Go to your GitHub repository**: https://github.com/[your-username]/DeusExMachina

2. **Navigate to Settings**:
   - Click on "Settings" tab in your repository
   - In the left sidebar, click on "Secrets and variables" → "Actions"

3. **Add the following secrets**:

### SMTP_FROM_EMAIL
- Click "New repository secret"
- Name: `SMTP_FROM_EMAIL`
- Value: Your Gmail address (e.g., `noreply@deusexmachina.com` or `your-email@gmail.com`)

### SMTP_USERNAME
- Click "New repository secret"
- Name: `SMTP_USERNAME`
- Value: Same as SMTP_FROM_EMAIL

### SMTP_PASSWORD
- Click "New repository secret"
- Name: `SMTP_PASSWORD`
- Value: Your Gmail App Password (see instructions below)

## How to Generate Gmail App Password

1. **Enable 2-Step Verification** (if not already enabled):
   - Go to https://myaccount.google.com/security
   - Click on "2-Step Verification"
   - Follow the setup process

2. **Create App Password**:
   - Go to https://myaccount.google.com/apppasswords
   - Or navigate: Google Account → Security → 2-Step Verification → App passwords
   - Select "Mail" from the dropdown
   - Select your device or choose "Other" and name it "DeusExMachina"
   - Click "Generate"
   - Copy the 16-character password (without spaces)

3. **Use this app password** as the value for `SMTP_PASSWORD` secret

## Alternative: Using GitHub CLI

If you have GitHub CLI installed, you can add secrets from command line:

```bash
# Set your email and app password
export GMAIL_ADDRESS="your-email@gmail.com"
export GMAIL_APP_PASSWORD="your-16-char-app-password"

# Add secrets using GitHub CLI
gh secret set SMTP_FROM_EMAIL --body="$GMAIL_ADDRESS"
gh secret set SMTP_USERNAME --body="$GMAIL_ADDRESS"
gh secret set SMTP_PASSWORD --body="$GMAIL_APP_PASSWORD"
```

## Verify Secrets

After adding, you should see these 3 secrets listed in your repository's secrets page:
- SMTP_FROM_EMAIL
- SMTP_USERNAME
- SMTP_PASSWORD

The values will be hidden, showing only asterisks.

## Important Notes

- **Never commit these values** to your repository
- The app password is different from your regular Gmail password
- App passwords can be revoked anytime from your Google Account settings
- For production, consider using a dedicated email account for sending
- Gmail has daily sending limits (500 emails/day for regular accounts)