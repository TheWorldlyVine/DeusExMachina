#!/bin/bash
set -e

PROJECT_ID="deus-ex-machina-prod"
GITHUB_SA="github-actions-sa@${PROJECT_ID}.iam.gserviceaccount.com"

echo "Granting IAM permissions to GitHub Actions service account..."

# Grant permission to manage IAM policies
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${GITHUB_SA}" \
  --role="roles/iam.securityAdmin"

# Grant permission to manage Firestore
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${GITHUB_SA}" \
  --role="roles/datastore.owner"

# Grant permission to manage Firebase rules
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${GITHUB_SA}" \
  --role="roles/firebaserules.admin"

echo "Permissions granted successfully!"