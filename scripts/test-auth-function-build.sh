#!/bin/bash
set -e

# Test auth-function build and packaging
echo "Testing auth-function build..."

cd apps/backend/auth-function

# Clean and build
echo "Running gradle clean build..."
./gradlew clean build

# Check if the JAR was created
if [ -f build/libs/auth-function.jar ]; then
    echo "✓ JAR file created successfully"
    
    # Check JAR contents
    echo ""
    echo "Checking JAR contents for SLF4J..."
    jar tf build/libs/auth-function.jar | grep -E "(slf4j|logback)" | head -10 || echo "No SLF4J classes found in JAR"
    
    echo ""
    echo "Checking for main classes..."
    jar tf build/libs/auth-function.jar | grep "com/deusexmachina" | head -10
else
    echo "✗ JAR file not found!"
    exit 1
fi

# Check if functions plugin creates deployment package
echo ""
echo "Checking for Cloud Functions deployment artifacts..."
if [ -d build/deploy ]; then
    echo "✓ Deploy directory exists"
    ls -la build/deploy/
else
    echo "ℹ Deploy directory not found (may be created during deployment)"
fi

echo ""
echo "Build test complete!"