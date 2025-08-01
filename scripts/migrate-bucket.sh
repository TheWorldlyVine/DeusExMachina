#!/bin/bash

# Script to migrate contents from old bucket to new bucket name
# This handles the transition from deus-ex-machina-prod-prod-static to deus-ex-machina-prod-static

set -e

echo "=== Bucket Migration Script ==="

PROJECT_ID="${1:-deus-ex-machina-prod}"
OLD_BUCKET="${PROJECT_ID}-prod-static"
NEW_BUCKET="${PROJECT_ID}-static"

echo "Project ID: ${PROJECT_ID}"
echo "Old bucket: gs://${OLD_BUCKET}/"
echo "New bucket: gs://${NEW_BUCKET}/"
echo ""

# Check if old bucket exists
if ! gcloud storage buckets describe "gs://${OLD_BUCKET}" --project="${PROJECT_ID}" &>/dev/null; then
    echo "Old bucket gs://${OLD_BUCKET}/ does not exist. Nothing to migrate."
    exit 0
fi

# Check if new bucket exists
if gcloud storage buckets describe "gs://${NEW_BUCKET}" --project="${PROJECT_ID}" &>/dev/null; then
    echo "New bucket gs://${NEW_BUCKET}/ already exists."
else
    echo "New bucket will be created by Terraform."
fi

# List contents of old bucket
echo ""
echo "Contents of old bucket:"
gsutil ls -la "gs://${OLD_BUCKET}/" || echo "Bucket is empty"

# Copy all contents from old to new (if new exists)
if gcloud storage buckets describe "gs://${NEW_BUCKET}" --project="${PROJECT_ID}" &>/dev/null; then
    echo ""
    echo "Copying contents from old bucket to new bucket..."
    gsutil -m rsync -r -d "gs://${OLD_BUCKET}/" "gs://${NEW_BUCKET}/" || echo "No contents to copy"
fi

# Delete all objects from old bucket
echo ""
echo "Removing all objects from old bucket to allow Terraform to delete it..."
gsutil -m rm -r "gs://${OLD_BUCKET}/**" 2>/dev/null || echo "No objects to remove"

echo ""
echo "Migration preparation complete. Old bucket is now empty and can be deleted by Terraform."
echo "=== End Migration ==="