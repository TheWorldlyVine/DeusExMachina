# Fixing GitHub Actions Permission Errors

The GitHub Actions workflow is currently failing with permission errors. Here's how to fix it:

## The Problem

The GitHub Actions service account (`github-actions-sa@deus-ex-machina-prod.iam.gserviceaccount.com`) lacks permissions to:
- Create Pub/Sub topics, subscriptions, and schemas
- Create service accounts
- Manage IAM policy bindings

## The Solution

You need to run the permissions script with an account that has IAM admin privileges:

```bash
# Set the project ID
export PROJECT_ID=deus-ex-machina-prod

# Run the permissions script
./scripts/grant-github-actions-permissions.sh
```

This will grant the following roles to the GitHub Actions service account:
- `roles/pubsub.admin` - For creating Pub/Sub resources
- `roles/iam.serviceAccountAdmin` - For creating service accounts
- `roles/resourcemanager.projectIamAdmin` - For managing IAM bindings

## After Running the Script

Once permissions are granted:
1. The GitHub Actions workflow will automatically retry (or you can manually re-run it)
2. The terraform-apply-smart.sh script will properly handle any remaining "already exists" errors
3. The email service infrastructure will be deployed

## Important Note

The terraform-apply-smart.sh script has been fixed to:
- Only ignore 409 (already exists) errors
- Fail immediately on 403 (permission denied) errors
- Fail on any other errors

This ensures we don't accidentally ignore real permission or configuration issues.