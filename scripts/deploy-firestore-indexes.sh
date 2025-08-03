#!/bin/bash

# Deploy Firestore indexes
# This script deploys the Firestore composite indexes required for the application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Deploying Firestore indexes...${NC}"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI is not installed${NC}"
    echo "Please install the Google Cloud SDK: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if firebase is installed
if ! command -v firebase &> /dev/null; then
    echo -e "${YELLOW}Warning: Firebase CLI is not installed${NC}"
    echo "Installing Firebase CLI globally..."
    npm install -g firebase-tools
fi

# Get the project ID
PROJECT_ID=$(gcloud config get-value project)
if [ -z "$PROJECT_ID" ]; then
    echo -e "${RED}Error: No GCP project set${NC}"
    echo "Run: gcloud config set project YOUR_PROJECT_ID"
    exit 1
fi

echo -e "${GREEN}Using project: ${PROJECT_ID}${NC}"

# Deploy indexes using Firebase CLI
echo -e "${YELLOW}Deploying Firestore indexes...${NC}"
firebase deploy --only firestore:indexes --project "$PROJECT_ID"

echo -e "${GREEN}✅ Firestore indexes deployed successfully!${NC}"
echo ""
echo "Note: It may take a few minutes for the indexes to be built."
echo "You can check the status in the Firebase Console:"
echo "https://console.firebase.google.com/project/$PROJECT_ID/firestore/indexes"