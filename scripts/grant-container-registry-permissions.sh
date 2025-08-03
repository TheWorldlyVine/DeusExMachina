#!/bin/bash
# Script to grant Container Registry permissions to GitHub Actions service account

set -e

# Check if PROJECT_ID is set
if [ -z "$PROJECT_ID" ]; then
    echo "Please set PROJECT_ID environment variable"
    echo "Example: export PROJECT_ID=your-project-id"
    exit 1
fi

SERVICE_ACCOUNT="github-actions-sa@${PROJECT_ID}.iam.gserviceaccount.com"
echo "Granting Container Registry permissions to: $SERVICE_ACCOUNT"

# Enable Container Registry API if not already enabled
echo "Enabling Container Registry API..."
gcloud services enable containerregistry.googleapis.com --project=$PROJECT_ID || true

# Grant Storage Admin role on the GCR bucket
# Container Registry uses Cloud Storage buckets named artifacts.PROJECT-ID.appspot.com
echo "Granting Storage Admin role on GCR buckets..."
for region in us eu asia gcr.io; do
    BUCKET="gs://${region}.artifacts.${PROJECT_ID}.appspot.com"
    echo "  Checking bucket: $BUCKET"
    if gsutil ls $BUCKET &>/dev/null; then
        echo "  Granting access to: $BUCKET"
        gsutil iam ch serviceAccount:${SERVICE_ACCOUNT}:objectAdmin $BUCKET || true
    fi
done

# Alternative: Grant Storage Admin role at project level (broader permission)
echo ""
echo "Granting Storage Admin role at project level..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/storage.admin"

# Grant Container Registry Service Agent role
echo ""
echo "Granting Container Registry Service Agent role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/containerregistry.ServiceAgent" || true

# Grant Cloud Run Admin for deploying services
echo ""
echo "Granting Cloud Run Admin role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/run.admin"

# Grant Service Account User on Cloud Run service account
echo ""
echo "Granting Service Account User role on compute service account..."
COMPUTE_SA="${PROJECT_ID}@appspot.gserviceaccount.com"
gcloud iam service-accounts add-iam-policy-binding $COMPUTE_SA \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/iam.serviceAccountUser" \
    --project=$PROJECT_ID || true

echo ""
echo "✅ Container Registry permissions granted successfully!"
echo ""
echo "The service account $SERVICE_ACCOUNT now has:"
echo "  - Storage Admin (for GCR bucket operations)"
echo "  - Container Registry Service Agent (if available)"
echo "  - Cloud Run Admin (for deploying services)"
echo "  - Service Account User (for using compute service account)"
echo ""
echo "You can verify access with:"
echo "  gcloud container images list --project=$PROJECT_ID"