#!/bin/bash
# Script to grant Artifact Registry permissions for Cloud Build deployments

set -e

# Check if PROJECT_ID is set
if [ -z "$PROJECT_ID" ]; then
    echo "Please set PROJECT_ID environment variable"
    echo "Example: export PROJECT_ID=your-project-id"
    exit 1
fi

GITHUB_SA="github-actions-sa@${PROJECT_ID}.iam.gserviceaccount.com"
echo "Granting Artifact Registry permissions to: $GITHUB_SA"

# Enable Artifact Registry API
echo "Enabling Artifact Registry API..."
gcloud services enable artifactregistry.googleapis.com --project=$PROJECT_ID

# Grant Artifact Registry Administrator role
echo ""
echo "Granting Artifact Registry Administrator role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/artifactregistry.admin"

# Alternative: Grant specific permissions if admin is too broad
echo ""
echo "Alternatively, granting specific Artifact Registry permissions..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/artifactregistry.repoAdmin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${GITHUB_SA}" \
    --role="roles/artifactregistry.writer"

echo ""
echo "✅ Artifact Registry permissions granted successfully!"
echo ""
echo "The service account $GITHUB_SA now has:"
echo "  - Artifact Registry Administrator"
echo "  - Artifact Registry Repository Admin"
echo "  - Artifact Registry Writer"
echo ""
echo "This allows Cloud Build to create and use Artifact Registry repositories."