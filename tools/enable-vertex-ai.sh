#!/bin/bash
# Enable Vertex AI API for Gemini text generation

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Enabling Vertex AI API for Gemini text generation...${NC}"

# Get the project ID
PROJECT_ID=$(gcloud config get-value project 2>/dev/null)

if [ -z "$PROJECT_ID" ]; then
    echo -e "${RED}Error: No project ID found. Please set your project with:${NC}"
    echo "gcloud config set project YOUR_PROJECT_ID"
    exit 1
fi

echo -e "${YELLOW}Project ID: ${PROJECT_ID}${NC}"

# Enable the Vertex AI API
echo -e "${GREEN}Enabling aiplatform.googleapis.com...${NC}"
gcloud services enable aiplatform.googleapis.com --project="$PROJECT_ID"

# Wait a moment for the API to be fully enabled
echo -e "${YELLOW}Waiting for API to propagate...${NC}"
sleep 10

# Verify the API is enabled
if gcloud services list --enabled --project="$PROJECT_ID" | grep -q "aiplatform.googleapis.com"; then
    echo -e "${GREEN}✓ Vertex AI API successfully enabled!${NC}"
else
    echo -e "${RED}✗ Failed to enable Vertex AI API${NC}"
    exit 1
fi

echo -e "${GREEN}Done! You can now use Gemini models through Vertex AI.${NC}"