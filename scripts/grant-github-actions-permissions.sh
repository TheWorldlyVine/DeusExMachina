#!/bin/bash
# This script grants necessary permissions to the GitHub Actions service account
# Run this locally with admin credentials

set -e

# Check if PROJECT_ID is set
if [ -z "$PROJECT_ID" ]; then
    echo "Please set PROJECT_ID environment variable"
    echo "Example: export PROJECT_ID=your-project-id"
    exit 1
fi

SERVICE_ACCOUNT="github-actions-sa@${PROJECT_ID}.iam.gserviceaccount.com"
echo "Granting permissions to: $SERVICE_ACCOUNT"

# Grant Storage Admin role
echo "Granting Storage Admin role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/storage.admin"

# Grant Cloud Functions Developer role
echo "Granting Cloud Functions Developer role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/cloudfunctions.developer"

# Grant Service Account User role
echo "Granting Service Account User role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/iam.serviceAccountUser"

# Grant Compute Network Viewer role
echo "Granting Compute Network Viewer role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/compute.networkViewer"

# Grant Compute Admin role (for URL maps and CDN operations)
echo "Granting Compute Admin role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/compute.admin"

# Grant Compute Security Admin role
echo "Granting Compute Security Admin role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/compute.securityAdmin"

# Additional roles that might be needed
echo "Granting Load Balancer Admin role..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/compute.loadBalancerAdmin"

echo ""
echo "✅ All permissions granted successfully!"
echo ""
echo "The service account $SERVICE_ACCOUNT now has:"
echo "  - Storage Admin (for bucket operations)"
echo "  - Cloud Functions Developer (for function deployments)"
echo "  - Service Account User (for using service accounts)"
echo "  - Compute Network Viewer (for network operations)"
echo "  - Compute URL Map Admin (for CDN invalidation)"
echo "  - Compute Security Admin (for Cloud Armor)"
echo "  - Load Balancer Admin (for load balancer operations)"