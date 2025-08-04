#!/bin/bash

# SPA Routing Test Script
# This script tests SPA routing functionality across different environments

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
BASE_URL="${BASE_URL:-https://god-in-a-box.com}"
APP_PATH="/novel-creator"

# Function to test a URL
test_url() {
    local url=$1
    local expected_status=${2:-200}
    local description=$3
    
    echo -n "Testing: $description ($url)... "
    
    # Get HTTP status code
    status=$(curl -s -o /dev/null -w "%{http_code}" -L "$url")
    
    if [ "$status" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ PASS${NC} (Status: $status)"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (Expected: $expected_status, Got: $status)"
        return 1
    fi
}

# Function to test content
test_content() {
    local url=$1
    local search_string=$2
    local description=$3
    
    echo -n "Testing content: $description... "
    
    # Get page content
    content=$(curl -s -L "$url")
    
    if echo "$content" | grep -q "$search_string"; then
        echo -e "${GREEN}✓ PASS${NC}"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (String not found: $search_string)"
        return 1
    fi
}

# Function to test headers
test_headers() {
    local url=$1
    local header_name=$2
    local expected_pattern=$3
    local description=$4
    
    echo -n "Testing headers: $description... "
    
    # Get headers
    headers=$(curl -s -I -L "$url")
    header_value=$(echo "$headers" | grep -i "^$header_name:" | cut -d' ' -f2-)
    
    if [ -n "$header_value" ]; then
        if echo "$header_value" | grep -q "$expected_pattern"; then
            echo -e "${GREEN}✓ PASS${NC} (Header: $header_value)"
            return 0
        else
            echo -e "${YELLOW}! WARN${NC} (Header found but doesn't match pattern)"
            return 1
        fi
    else
        echo -e "${RED}✗ FAIL${NC} (Header not found: $header_name)"
        return 1
    fi
}

echo "========================================="
echo "SPA Routing Test Suite"
echo "Base URL: $BASE_URL"
echo "App Path: $APP_PATH"
echo "========================================="

# Test counters
total_tests=0
passed_tests=0

# Test 1: Root path
if test_url "$BASE_URL$APP_PATH/" 200 "App root path"; then
    ((passed_tests++))
fi
((total_tests++))

# Test 2: Direct route access
routes=(
    "/documents"
    "/editor/test-doc-123"
    "/settings"
    "/settings/profile"
)

for route in "${routes[@]}"; do
    if test_url "$BASE_URL$APP_PATH$route" 200 "Direct access: $route"; then
        ((passed_tests++))
    fi
    ((total_tests++))
done

# Test 3: Content verification
if test_content "$BASE_URL$APP_PATH/documents" "<div id=\"root\"" "React root element"; then
    ((passed_tests++))
fi
((total_tests++))

# Test 4: Static file handling
if test_url "$BASE_URL$APP_PATH/assets/non-existent.js" 404 "Non-existent static file"; then
    ((passed_tests++))
fi
((total_tests++))

# Test 5: Cache headers
if test_headers "$BASE_URL$APP_PATH/" "cache-control" "max-age" "Cache headers present"; then
    ((passed_tests++))
fi
((total_tests++))

# Test 6: CORS headers (if applicable)
if test_headers "$BASE_URL$APP_PATH/" "access-control-allow-origin" "." "CORS headers"; then
    ((passed_tests++))
fi
((total_tests++))

# Test 7: Security headers
security_headers=(
    "x-content-type-options"
    "x-frame-options"
    "x-xss-protection"
)

for header in "${security_headers[@]}"; do
    if test_headers "$BASE_URL$APP_PATH/" "$header" "." "Security header: $header"; then
        ((passed_tests++))
    fi
    ((total_tests++))
done

echo "========================================="
echo "Test Summary:"
echo "Total Tests: $total_tests"
echo -e "Passed: ${GREEN}$passed_tests${NC}"
echo -e "Failed: ${RED}$((total_tests - passed_tests))${NC}"
echo "========================================="

# Return exit code based on test results
if [ "$passed_tests" -eq "$total_tests" ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
fi