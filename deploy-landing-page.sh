#!/bin/bash

# Landing page deployment to GCP

set -e

echo "🚀 Landing Page Deployment"
echo "========================="

# Get project ID
PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
if [ -z "$PROJECT_ID" ]; then
    echo "❌ No project set. Run: gcloud config set project YOUR-PROJECT-ID"
    exit 1
fi
echo "Using project: $PROJECT_ID"

# Create a bucket for landing page
BUCKET_NAME="${PROJECT_ID}-landing-page"
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

# Build the landing page
echo ""
echo "4️⃣ Building landing page..."
cd apps/frontend/landing-page

# Check if node_modules exists, if not, install dependencies
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    if command -v pnpm &> /dev/null; then
        pnpm install
    else
        npm install
    fi
fi

# Build the project
if command -v pnpm &> /dev/null; then
    pnpm run build
else
    npm run build
fi

cd ../../..

# Upload the landing page
echo ""
echo "5️⃣ Uploading landing page..."
gsutil -m rsync -r -d apps/frontend/landing-page/dist/ gs://$BUCKET_NAME/

# Set cache headers
echo ""
echo "6️⃣ Setting cache headers..."
# HTML files - no cache
gsutil -m setmeta -h "Cache-Control:no-cache, must-revalidate" gs://$BUCKET_NAME/**.html

# CSS and JS files - long cache (versioned by build)
gsutil -m setmeta -h "Cache-Control:public, max-age=31536000" gs://$BUCKET_NAME/**.js
gsutil -m setmeta -h "Cache-Control:public, max-age=31536000" gs://$BUCKET_NAME/**.css

# Images - moderate cache
gsutil -m setmeta -h "Cache-Control:public, max-age=86400" gs://$BUCKET_NAME/**.{png,jpg,jpeg,gif,svg,webp}

echo ""
echo "✅ Deployment complete!"
echo ""
echo "🌐 Your landing page is available at:"
echo "   https://storage.googleapis.com/$BUCKET_NAME/index.html"
echo ""
echo "📝 Next steps:"
echo "   - Set up Cloud CDN for better performance"
echo "   - Configure custom domain"
echo "   - Enable Cloud Load Balancer for HTTPS"