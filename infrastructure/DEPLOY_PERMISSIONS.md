# Deploying GitHub Actions Permissions with Terraform

This guide explains how to apply the necessary IAM permissions for GitHub Actions to deploy the GraphQL gateway and other services.

## Prerequisites

- Google Cloud SDK (`gcloud`) installed and configured
- Terraform installed (version 1.0 or higher)
- Project Owner or IAM Admin permissions on the GCP project

## Steps

1. **Authenticate with admin credentials**:
   ```bash
   gcloud auth login
   gcloud auth application-default login
   ```

2. **Set your project ID**:
   ```bash
   export PROJECT_ID=your-project-id
   gcloud config set project $PROJECT_ID
   ```

3. **Navigate to the infrastructure directory**:
   ```bash
   cd infrastructure/environments/prod
   ```

4. **Initialize Terraform** (if not already done):
   ```bash
   terraform init
   ```

5. **Review the changes**:
   ```bash
   terraform plan -var="project_id=$PROJECT_ID"
   ```

   This will show:
   - APIs that will be enabled (Cloud Build, Cloud Run, Artifact Registry, etc.)
   - IAM permissions that will be granted to the GitHub Actions service account

6. **Apply the changes**:
   ```bash
   terraform apply -var="project_id=$PROJECT_ID"
   ```

   Type `yes` when prompted to confirm.

## What This Does

The Terraform configuration will:

1. **Enable required APIs**:
   - Cloud Build API
   - Cloud Run API
   - Artifact Registry API
   - Container Registry API
   - And other necessary APIs

2. **Grant IAM permissions** to `github-actions-sa@PROJECT_ID.iam.gserviceaccount.com`:
   - **Artifact Registry Admin** - To create repositories for Cloud Build
   - **Cloud Build Editor** - To submit builds
   - **Cloud Run Admin** - To deploy services
   - **Storage Admin** - To manage build artifacts
   - **Service Account User** - To act as the default compute service account
   - And other existing permissions for Cloud Functions, etc.

## Verify Permissions

After applying, you can verify the permissions:

```bash
# List all permissions for the service account
gcloud projects get-iam-policy $PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:github-actions-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --format="table(bindings.role)"

# Check enabled APIs
gcloud services list --enabled
```

## Troubleshooting

If you encounter permission errors:

1. Ensure you're authenticated with an account that has IAM Admin permissions
2. If the service account doesn't exist, create it first:
   ```bash
   gcloud iam service-accounts create github-actions-sa \
     --display-name="GitHub Actions Service Account"
   ```

3. If you get "API not enabled" errors, the APIs module will handle enabling them automatically

## Next Steps

After applying these permissions:

1. Re-run the GitHub Actions workflow
2. The GraphQL gateway deployment should now succeed
3. Monitor the Cloud Build logs if there are any issues

## Important Notes

- These permissions are quite broad for a CI/CD service account
- In a production environment, consider using more granular permissions
- The APIs module disables `disable_on_destroy` to prevent accidental API disabling
- Always review the Terraform plan before applying