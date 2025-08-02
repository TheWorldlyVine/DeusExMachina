#!/bin/bash
# Enable Eventarc API for Cloud Functions v2 Pub/Sub triggers

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Enabling Eventarc API for Cloud Functions v2...${NC}"

# Get the project ID
PROJECT_ID=$(gcloud config get-value project 2>/dev/null)

if [ -z "$PROJECT_ID" ]; then
    echo -e "${RED}Error: No project ID found. Please set your project with:${NC}"
    echo "gcloud config set project YOUR_PROJECT_ID"
    exit 1
fi

echo -e "${YELLOW}Project ID: ${PROJECT_ID}${NC}"

# Enable the Eventarc API
echo -e "${GREEN}Enabling eventarc.googleapis.com...${NC}"
gcloud services enable eventarc.googleapis.com --project="$PROJECT_ID"

# Wait a moment for the API to be fully enabled
echo -e "${YELLOW}Waiting for API to propagate...${NC}"
sleep 10

# Verify the API is enabled
if gcloud services list --enabled --project="$PROJECT_ID" | grep -q "eventarc.googleapis.com"; then
    echo -e "${GREEN}✓ Eventarc API successfully enabled!${NC}"
else
    echo -e "${RED}✗ Failed to enable Eventarc API${NC}"
    exit 1
fi

echo -e "${GREEN}Done! You can now deploy Cloud Functions v2 with Pub/Sub triggers.${NC}"