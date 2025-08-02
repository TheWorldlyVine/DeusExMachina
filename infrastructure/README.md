# DeusExMachina Infrastructure

This directory contains the Terraform configuration for the DeusExMachina infrastructure.

## Current State (as of August 2, 2025)

### Hybrid Infrastructure Management

Due to permission constraints with the GitHub Actions service account, we currently use a hybrid approach:

1. **Managed by Terraform**:
   - Static hosting infrastructure (Storage bucket, CDN, Load Balancer)
   - Cloud Functions public access permissions
   - Cloud Armor security policies
   - SSL certificates

2. **Created Manually**:
   - Firestore database
   - Secret Manager secrets (jwt-secret)
   - IAM bindings for service accounts
   - Cloud Functions themselves (deployed via CI/CD)

3. **Defined in Terraform but Not Applied**:
   - Firestore database configuration (firestore.tf)
   - Secret Manager resources (secrets.tf)
   - Full Cloud Functions module
   - VPC network
   - Cloud SQL and Redis instances

### Required Permissions for Full Terraform Management

To manage all resources via Terraform, the service account needs:
- `roles/datastore.admin` - For Firestore
- `roles/secretmanager.admin` - For Secret Manager
- `roles/cloudfunctions.admin` - For Cloud Functions
- `roles/iam.securityAdmin` - For IAM bindings
- `roles/compute.admin` - For networking resources

### Manual Resource Creation Commands

If you need to recreate the manually managed resources:

```bash
# Create Firestore database
gcloud firestore databases create \
  --location=us-central1 \
  --project=deus-ex-machina-prod

# Create JWT secret
echo -n "$(openssl rand -base64 32)" | gcloud secrets create jwt-secret \
  --data-file=- \
  --project=deus-ex-machina-prod

# Grant Firestore access
gcloud projects add-iam-policy-binding deus-ex-machina-prod \
  --member="serviceAccount:$(gcloud projects describe deus-ex-machina-prod --format='value(projectNumber)')-compute@developer.gserviceaccount.com" \
  --role="roles/datastore.user"

# Grant Secret Manager access
gcloud projects add-iam-policy-binding deus-ex-machina-prod \
  --member="serviceAccount:$(gcloud projects describe deus-ex-machina-prod --format='value(projectNumber)')-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### Environment Structure

- `/environments/prod/` - Production environment configuration
- `/modules/` - Reusable Terraform modules

### Deployment Process

1. Infrastructure changes are applied via GitHub Actions on merge to main
2. Cloud Functions are deployed separately via the CI/CD pipeline
3. Manual resources must be created before first deployment

### Future Improvements

1. Grant proper permissions to GitHub Actions service account
2. Import manually created resources into Terraform state
3. Enable full infrastructure-as-code management
4. Consider using Workload Identity Federation for better security