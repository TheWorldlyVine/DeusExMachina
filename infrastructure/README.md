# DeusExMachina Infrastructure

This directory contains the Terraform configuration for the DeusExMachina infrastructure.

## Current State (as of August 2, 2025)

### Infrastructure Management

The `terraform-sa` service account now has full permissions to manage all infrastructure resources.

**Permissions Granted**:
- `roles/datastore.owner` - For Firestore management
- `roles/secretmanager.admin` - For Secret Manager
- `roles/iam.securityAdmin` - For IAM bindings
- `roles/editor` - General project permissions

### Managed Resources

1. **Fully Managed by Terraform**:
   - Static hosting infrastructure (Storage bucket, CDN, Load Balancer)
   - Cloud Functions public access permissions
   - Cloud Armor security policies
   - SSL certificates
   - Firestore database
   - Secret Manager secrets
   - IAM bindings

2. **Deployed via CI/CD**:
   - Cloud Functions themselves (source code deployment)

3. **Future Resources** (defined but not yet created):
   - VPC network
   - Cloud SQL and Redis instances

### Initial Setup for Existing Resources

If you have existing resources that were created manually, you can import them:

```bash
cd infrastructure/environments/prod

# Import existing Firestore database
terraform import google_firestore_database.main "projects/deus-ex-machina-prod/databases/(default)"

# Import existing JWT secret
terraform import google_secret_manager_secret.jwt_secret "projects/deus-ex-machina-prod/secrets/jwt-secret"

# Import existing IAM bindings (if they exist)
PROJECT_NUMBER=$(gcloud projects describe deus-ex-machina-prod --format='value(projectNumber)')
terraform import google_project_iam_member.firestore_user "deus-ex-machina-prod roles/datastore.user serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
terraform import google_project_iam_member.secret_accessor "deus-ex-machina-prod roles/secretmanager.secretAccessor serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
```

### Manual Commands (for reference)

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