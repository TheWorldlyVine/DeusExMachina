# Manual Resources

This document tracks resources that need to be created or configured manually in the Google Cloud Console.

## APIs to Enable

The following APIs need to be enabled for the project to work correctly:

### Core APIs (Already in README)
These are documented in the main README and should be enabled during initial setup:
- All APIs listed in README.md section "Enable all required Google Cloud APIs"

### Additional APIs for Novel Creator Features

1. **Eventarc API** (eventarc.googleapis.com)
   - Required for: Cloud Functions v2 with Pub/Sub triggers (email-processor-function)
   - Enable with: `./tools/enable-eventarc.sh`
   - Or manually: `gcloud services enable eventarc.googleapis.com --project=$PROJECT_ID`

2. **Vertex AI API** (aiplatform.googleapis.com)
   - Required for: Gemini AI text generation in novel-ai-service
   - Enable with: `gcloud services enable aiplatform.googleapis.com --project=$PROJECT_ID`

## Firestore Database

The Firestore database needs to be created manually due to permission constraints:

1. Go to [Firestore Console](https://console.cloud.google.com/firestore)
2. Select your project
3. Click "Create Database"
4. Choose:
   - Database ID: `(default)`
   - Mode: Native mode
   - Location: `us-central1`

## Service Accounts

### Email Processor Service Account
Create manually if deployment fails:
```bash
gcloud iam service-accounts create email-processor \
  --display-name="Email Processor Service Account" \
  --project=$PROJECT_ID
```

### Novel AI Service Accounts
For the novel creator services:
```bash
# AI Generation Service
gcloud iam service-accounts create novel-ai-service \
  --display-name="Novel AI Generation Service" \
  --project=$PROJECT_ID

# Memory Service  
gcloud iam service-accounts create novel-memory-service \
  --display-name="Novel Memory Service" \
  --project=$PROJECT_ID

# Document Service
gcloud iam service-accounts create novel-document-service \
  --display-name="Novel Document Service" \
  --project=$PROJECT_ID
```

## Secrets

The following secrets need to be created in Secret Manager:

### For Email Service
- `sendgrid-api-key`: SendGrid API key for email sending
- `smtp-username`: SMTP username (if using SMTP instead of SendGrid)
- `smtp-password`: SMTP password (if using SMTP instead of SendGrid)

### For Novel Creator
- `gemini-api-key`: API key for Gemini AI (if not using default project credentials)

Create secrets with:
```bash
echo -n "your-secret-value" | gcloud secrets create SECRET_NAME --data-file=-
```

## Pub/Sub Topics

The email service topic should be created automatically by Terraform, but if needed:
```bash
gcloud pubsub topics create deus-ex-machina-email-events --project=$PROJECT_ID
```

## Notes

- Most of these resources are created automatically by Terraform or CI/CD
- Manual creation is only needed if there are permission issues or initial setup requirements
- Always check the Terraform modules first before creating resources manually