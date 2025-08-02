#!/bin/bash
set -e

# Import existing Terraform resources script
# This script imports resources that already exist in GCP into Terraform state

PROJECT_ID="deus-ex-machina-prod"

echo "=== Terraform Resource Import Script ==="
echo ""
echo "This script will import existing GCP resources into Terraform state."
echo "Project: $PROJECT_ID"
echo ""

# Change to infrastructure directory
cd ../infrastructure/environments/prod

# Initialize Terraform
echo "Initializing Terraform..."
terraform init -backend-config="bucket=${PROJECT_ID}-terraform-state"

# Import Firestore database
echo ""
echo "Importing Firestore database..."
terraform import google_firestore_database.main "${PROJECT_ID}/(default)"

# Import Secret Manager secrets
echo ""
echo "Importing Secret Manager secrets..."
terraform import google_secret_manager_secret.jwt_secret "projects/${PROJECT_ID}/secrets/jwt-secret"
terraform import google_secret_manager_secret.sendgrid_api_key "projects/${PROJECT_ID}/secrets/sendgrid-api-key"

# Import secret versions if they exist
echo ""
echo "Checking for secret versions..."
if gcloud secrets versions list jwt-secret --project=$PROJECT_ID --format="value(name)" | grep -q "1"; then
    echo "Importing jwt-secret version..."
    terraform import google_secret_manager_secret_version.jwt_secret "projects/${PROJECT_ID}/secrets/jwt-secret/versions/1"
fi

if gcloud secrets versions list sendgrid-api-key --project=$PROJECT_ID --format="value(name)" | grep -q "1"; then
    echo "Importing sendgrid-api-key version..."
    terraform import google_secret_manager_secret_version.sendgrid_api_key "projects/${PROJECT_ID}/secrets/sendgrid-api-key/versions/1"
fi

echo ""
echo "✓ Import complete!"
echo ""
echo "Next steps:"
echo "1. Run 'terraform plan' to verify the imported state"
echo "2. Grant additional permissions to the GitHub Actions service account"
echo "3. Re-run the GitHub Actions workflow"