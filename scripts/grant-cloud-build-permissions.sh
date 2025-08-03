#!/bin/bash
# Script to grant Cloud Build permissions for deploying to Cloud Run

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

echo "Project ID: $PROJECT_ID"
echo "Project Number: $PROJECT_NUMBER"
echo "GitHub Actions SA: $GITHUB_SA"
echo "Cloud Build SA: $CLOUD_BUILD_SA"
echo ""

# Enable required APIs
echo "Enabling required APIs..."
gcloud services enable cloudbuild.googleapis.com --project=$PROJECT_ID
gcloud services enable run.googleapis.com --project=$PROJECT_ID
gcloud services enable containerregistry.googleapis.com --project=$PROJECT_ID

# Grant Cloud Build permissions to deploy to Cloud Run
echo ""
echo "Granting Cloud Run Admin role to Cloud Build service account..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${CLOUD_BUILD_SA}" \
    --role="roles/run.admin"

# Grant Cloud Build permission to act as the service account
echo ""
echo "Granting Service Account User role to Cloud Build..."
gcloud iam service-accounts add-iam-policy-binding \
    ${PROJECT_NUMBER}-compute@developer.gserviceaccount.com \
    --member="serviceAccount:${CLOUD_BUILD_SA}" \
    --role="roles/iam.serviceAccountUser" \
    --project=$PROJECT_ID

# Grant GitHub Actions SA permission to submit builds
echo ""
echo "Granting Cloud Build Editor role to GitHub Actions SA..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/cloudbuild.builds.editor"

# Grant GitHub Actions SA permission to view builds
echo ""
echo "Granting Cloud Build Viewer role to GitHub Actions SA..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/cloudbuild.builds.viewer"

# Grant GitHub Actions SA permission to view and create Cloud Run services
echo ""
echo "Granting Cloud Run Developer role to GitHub Actions SA..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/run.developer"

# Grant Storage permissions for Cloud Build artifacts
echo ""
echo "Granting Storage permissions for build artifacts..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${CLOUD_BUILD_SA}" \
    --role="roles/storage.admin"

echo ""
echo "✅ Cloud Build permissions granted successfully!"
echo ""
echo "Permissions granted:"
echo "  - Cloud Build SA can deploy to Cloud Run"
echo "  - Cloud Build SA can act as compute service account"
echo "  - GitHub Actions SA can submit and view builds"
echo "  - GitHub Actions SA can manage Cloud Run services"
echo "  - Cloud Build SA has Storage Admin for artifacts"
echo ""
echo "Next steps:"
echo "1. Push the workflow changes to trigger deployment"
echo "2. Monitor the Cloud Build logs for any issues"
echo "3. Verify the Cloud Run service is deployed successfully"