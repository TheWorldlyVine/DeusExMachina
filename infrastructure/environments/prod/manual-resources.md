# Manually Created Resources

Due to permission constraints with the GitHub Actions service account, the following resources were created manually and should be imported into Terraform state if needed:

## Firestore Database
```bash
gcloud firestore databases create --location=us-central1 --project=deus-ex-machina-prod
```

## IAM Permissions
The following IAM permissions were granted manually:

### Function Service Accounts - Firestore Access
```bash
# All functions use the default compute service account
gcloud projects add-iam-policy-binding deus-ex-machina-prod \
  --member="serviceAccount:97677897945-compute@developer.gserviceaccount.com" \
  --role="roles/datastore.user"
```

### Secret Manager Access
```bash
gcloud secrets create jwt-secret --data-file=- --project=deus-ex-machina-prod <<< "$(openssl rand -base64 32)"

gcloud secrets add-iam-policy-binding jwt-secret \
  --member="serviceAccount:97677897945-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project=deus-ex-machina-prod
```

## To Import into Terraform

If you have sufficient permissions, you can import these resources:

```bash
cd infrastructure/environments/prod
terraform import module.firestore.google_firestore_database.database "projects/deus-ex-machina-prod/databases/(default)"
```

## Required Permissions for GitHub Actions

To allow GitHub Actions to manage these resources via Terraform, grant:

```bash
# IAM Admin for managing service account permissions
gcloud projects add-iam-policy-binding deus-ex-machina-prod \
  --member="serviceAccount:github-actions-sa@deus-ex-machina-prod.iam.gserviceaccount.com" \
  --role="roles/iam.securityAdmin"

# Firestore Admin for managing databases
gcloud projects add-iam-policy-binding deus-ex-machina-prod \
  --member="serviceAccount:github-actions-sa@deus-ex-machina-prod.iam.gserviceaccount.com" \
  --role="roles/datastore.owner"
```