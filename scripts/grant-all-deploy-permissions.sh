#!/bin/bash
# Comprehensive script to grant all necessary permissions for GraphQL gateway deployment

set -e

# Check if PROJECT_ID is set
if [ -z "$PROJECT_ID" ]; then
    echo "Please set PROJECT_ID environment variable"
    echo "Example: export PROJECT_ID=your-project-id"
    exit 1
fi

GITHUB_SA="github-actions-sa@${PROJECT_ID}.iam.gserviceaccount.com"
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
CLOUD_BUILD_SA="${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"

echo "==================================="
echo "Granting deployment permissions for:"
echo "Project ID: $PROJECT_ID"
echo "Project Number: $PROJECT_NUMBER"
echo "GitHub Actions SA: $GITHUB_SA"
echo "Cloud Build SA: $CLOUD_BUILD_SA"
echo "==================================="
echo ""

# Enable all required APIs
echo "Step 1: Enabling required APIs..."
gcloud services enable cloudbuild.googleapis.com --project=$PROJECT_ID
gcloud services enable run.googleapis.com --project=$PROJECT_ID
gcloud services enable containerregistry.googleapis.com --project=$PROJECT_ID
gcloud services enable artifactregistry.googleapis.com --project=$PROJECT_ID
echo "✅ APIs enabled"
echo ""

# Grant Artifact Registry permissions
echo "Step 2: Granting Artifact Registry permissions..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/artifactregistry.admin" \
    --condition=None
echo "✅ Artifact Registry admin granted"
echo ""

# Grant Cloud Build permissions
echo "Step 3: Granting Cloud Build permissions..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/cloudbuild.builds.builder" \
    --condition=None

# Also grant to Cloud Build SA
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${CLOUD_BUILD_SA}" \
    --role="roles/artifactregistry.writer" \
    --condition=None
echo "✅ Cloud Build permissions granted"
echo ""

# Grant Cloud Run permissions
echo "Step 4: Granting Cloud Run permissions..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/run.admin" \
    --condition=None

# Grant service account user permission
gcloud iam service-accounts add-iam-policy-binding \
    ${PROJECT_NUMBER}-compute@developer.gserviceaccount.com \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/iam.serviceAccountUser" \
    --project=$PROJECT_ID
echo "✅ Cloud Run permissions granted"
echo ""

# Grant Storage permissions
echo "Step 5: Granting Storage permissions..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/storage.admin" \
    --condition=None
echo "✅ Storage permissions granted"
echo ""

# Summary
echo "==================================="
echo "✅ ALL PERMISSIONS GRANTED!"
echo "==================================="
echo ""
echo "The GitHub Actions service account now has:"
echo "  ✓ Artifact Registry Admin - Can create and manage repositories"
echo "  ✓ Cloud Build Builder - Can submit builds"
echo "  ✓ Cloud Run Admin - Can deploy services"
echo "  ✓ Storage Admin - Can manage build artifacts"
echo "  ✓ Service Account User - Can act as compute service account"
echo ""
echo "The Cloud Build service account now has:"
echo "  ✓ Artifact Registry Writer - Can push images"
echo ""
echo "Next steps:"
echo "1. Re-run the GitHub Actions workflow"
echo "2. The deployment should now succeed"
echo ""
echo "To verify permissions:"
echo "  gcloud projects get-iam-policy $PROJECT_ID --flatten=\"bindings[].members\" --filter=\"bindings.members:serviceAccount:${GITHUB_SA}\""