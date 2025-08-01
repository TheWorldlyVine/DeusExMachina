#!/bin/bash
set -e

echo "Building auth-function..."

# Clean previous build
./gradlew clean

# Run tests
echo "Running tests..."
./gradlew test

# Run static analysis
echo "Running static analysis..."
./gradlew spotbugsMain

# Build the function
echo "Building JAR..."
./gradlew build

# Create deployment package
echo "Creating deployment package..."
./gradlew jar

# Create a distribution directory
mkdir -p build/distributions/auth-function
cp -r build/libs/* build/distributions/auth-function/
cp -r src/main/resources/* build/distributions/auth-function/ 2>/dev/null || true

# Create the zip file for deployment
cd build/distributions
zip -r auth-function.zip auth-function/
cd ../..

echo "Build complete! Deployment package: build/distributions/auth-function.zip"