#!/bin/bash

# Formal verification script for Java code

set -e

echo "Running formal verification..."

# Check if OpenJML is available
if [ ! -f "tools/openjml.jar" ]; then
    echo "OpenJML not found. Please download it from: https://www.openjml.org/"
    exit 1
fi

# Run OpenJML verification
echo "Running OpenJML ESC verification..."
java -jar tools/openjml.jar -esc \
  -cp "apps/backend/*/build/classes/java/main:lib/*" \
  -sourcepath "apps/backend/*/src/main/java" \
  -strict \
  apps/backend/*/src/main/java/com/deusexmachina/functions/*.java

# Run static analysis
echo "Running SpotBugs..."
./gradlew spotbugsMain

echo "Running Error Prone..."
./gradlew build -x test

echo "Verification complete!"