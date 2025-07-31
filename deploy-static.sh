#!/bin/bash

# Simple static site deployment to GCP - NO TERRAFORM NEEDED

set -e

echo "🚀 Simple Static Site Deployment"
echo "================================"

# Get project ID
PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
if [ -z "$PROJECT_ID" ]; then
    echo "❌ No project set. Run: gcloud config set project YOUR-PROJECT-ID"
    exit 1
fi
echo "Using project: $PROJECT_ID"

# Create a bucket for static hosting
BUCKET_NAME="${PROJECT_ID}-static-site"
echo ""
echo "1️⃣ Creating storage bucket: $BUCKET_NAME"

# Create bucket if it doesn't exist
if ! gsutil ls -b gs://$BUCKET_NAME &> /dev/null; then
    gsutil mb -p $PROJECT_ID gs://$BUCKET_NAME
    echo "✅ Bucket created"
else
    echo "✅ Bucket already exists"
fi

# Make bucket public
echo ""
echo "2️⃣ Making bucket public..."
gsutil iam ch allUsers:objectViewer gs://$BUCKET_NAME

# Set website configuration
echo ""
echo "3️⃣ Configuring website settings..."
gsutil web set -m index.html -e 404.html gs://$BUCKET_NAME

# Upload the test index.html
echo ""
echo "4️⃣ Uploading test page..."
gsutil cp apps/frontend/web-app/dist/index.html gs://$BUCKET_NAME/

# Set cache headers
gsutil setmeta -h "Cache-Control:no-cache" gs://$BUCKET_NAME/index.html

echo ""
echo "✅ Deployment complete!"
echo ""
echo "🌐 Your site is available at:"
echo "   https://storage.googleapis.com/$BUCKET_NAME/index.html"
echo ""
echo "To deploy your React app later:"
echo "   gsutil -m rsync -r -d apps/frontend/web-app/dist/ gs://$BUCKET_NAME/"