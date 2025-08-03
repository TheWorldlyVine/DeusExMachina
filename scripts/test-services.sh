#!/bin/bash

# Test script for Cloud Run services
# This script tests the health and basic functionality of the deployed services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Service URLs
AUTH_URL="https://auth-function-tbmcifixdq-uc.a.run.app"
DOCUMENT_URL="https://novel-document-service-tbmcifixdq-uc.a.run.app"
AI_URL="https://novel-ai-service-tbmcifixdq-uc.a.run.app"
MEMORY_URL="https://novel-memory-service-tbmcifixdq-uc.a.run.app"

echo -e "${BLUE}Testing Cloud Run Services...${NC}\n"

# Function to test a service
test_service() {
    local name=$1
    local url=$2
    local endpoint=$3
    
    echo -e "${YELLOW}Testing $name...${NC}"
    
    # Test health endpoint
    echo -n "  Health check: "
    response=$(curl -s -w "\n%{http_code}" "$url$endpoint" 2>/dev/null || echo "000")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ OK${NC}"
        if [ -n "$body" ]; then
            echo "  Response: $body" | jq . 2>/dev/null || echo "  Response: $body"
        fi
    elif [ "$http_code" = "000" ]; then
        echo -e "${RED}✗ Connection failed${NC}"
    else
        echo -e "${RED}✗ HTTP $http_code${NC}"
        if [ -n "$body" ]; then
            echo "  Error: $body"
        fi
    fi
    echo
}

# Test each service
test_service "Auth Service" "$AUTH_URL" "/health"
test_service "Document Service" "$DOCUMENT_URL" "/health"
test_service "AI Service" "$AI_URL" "/health"
test_service "Memory Service" "$MEMORY_URL" "/health"

# Test authentication
echo -e "${YELLOW}Testing Authentication...${NC}"
echo -n "  Login endpoint: "
login_response=$(curl -s -X POST "$AUTH_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"testpass"}' \
    -w "\n%{http_code}" 2>/dev/null || echo "000")
    
http_code=$(echo "$login_response" | tail -n1)
if [ "$http_code" = "200" ] || [ "$http_code" = "401" ]; then
    echo -e "${GREEN}✓ Responding${NC} (HTTP $http_code)"
else
    echo -e "${RED}✗ HTTP $http_code${NC}"
fi

echo -e "\n${BLUE}Service URLs:${NC}"
echo "  Auth:     $AUTH_URL"
echo "  Document: $DOCUMENT_URL"
echo "  AI:       $AI_URL"
echo "  Memory:   $MEMORY_URL"
echo
echo "Check Cloud Logging for detailed error messages:"
echo "  https://console.cloud.google.com/logs"