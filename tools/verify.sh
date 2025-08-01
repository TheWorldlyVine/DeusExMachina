#!/bin/bash

# Formal verification script for Java backend code
# Uses OpenJML for JML specification verification and static analysis tools

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    case $status in
        "info")
            echo -e "${YELLOW}[INFO]${NC} $message"
            ;;
        "success")
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            ;;
        "error")
            echo -e "${RED}[ERROR]${NC} $message"
            ;;
    esac
}

# Check if we're in the right directory
if [ ! -f "package.json" ] || [ ! -d "apps/backend" ]; then
    print_status "error" "This script must be run from the project root directory"
    exit 1
fi

print_status "info" "Starting formal verification process..."

# Check if OpenJML is installed
OPENJML_JAR="${OPENJML_JAR:-/usr/local/lib/openjml.jar}"
if [ ! -f "$OPENJML_JAR" ]; then
    print_status "error" "OpenJML not found at $OPENJML_JAR"
    print_status "info" "Please download OpenJML from https://www.openjml.org/"
    print_status "info" "Set OPENJML_JAR environment variable to point to the JAR file"
    exit 1
fi

# Create reports directory
REPORTS_DIR="reports/verification"
mkdir -p "$REPORTS_DIR"

# Function to verify a module
verify_module() {
    local module=$1
    local module_path="apps/backend/$module"
    
    if [ ! -d "$module_path" ]; then
        print_status "info" "Skipping $module (not found)"
        return
    fi
    
    print_status "info" "Verifying $module..."
    
    # Find all Java files
    local java_files=$(find "$module_path/src/main/java" -name "*.java" 2>/dev/null || true)
    
    if [ -z "$java_files" ]; then
        print_status "info" "No Java files found in $module"
        return
    fi
    
    # Run OpenJML verification
    local report_file="$REPORTS_DIR/$module-jml-report.txt"
    print_status "info" "Running JML verification for $module..."
    
    if java -jar "$OPENJML_JAR" -esc \
        -cp "build/classes/java/main:lib/*" \
        -sourcepath "$module_path/src/main/java" \
        -strict \
        $java_files > "$report_file" 2>&1; then
        print_status "success" "JML verification passed for $module"
    else
        print_status "error" "JML verification failed for $module (see $report_file)"
        # Don't exit on failure, continue with other modules
    fi
}

# Run Gradle build first to ensure classes are compiled
print_status "info" "Building Java backend..."
if ./gradlew :apps:backend:auth-function:build \
           :apps:backend:api-function:build \
           :apps:backend:processor-function:build \
           :apps:backend:shared:build 2>&1 | grep -E "(BUILD SUCCESSFUL|FAILED)"; then
    print_status "success" "Build completed"
else
    print_status "error" "Build failed"
    exit 1
fi

# Verify each backend module
for module in auth-function api-function processor-function shared; do
    verify_module "$module"
done

# Run SpotBugs static analysis
print_status "info" "Running SpotBugs static analysis..."
if ./gradlew spotbugsMain > "$REPORTS_DIR/spotbugs-report.txt" 2>&1; then
    print_status "success" "SpotBugs analysis completed"
else
    print_status "error" "SpotBugs analysis failed (see $REPORTS_DIR/spotbugs-report.txt)"
fi

# Run additional static analysis with Error Prone (included in build)
print_status "info" "Error Prone analysis was run during build phase"

# Generate summary report
SUMMARY_FILE="$REPORTS_DIR/verification-summary.txt"
{
    echo "Formal Verification Summary"
    echo "=========================="
    echo "Date: $(date)"
    echo ""
    echo "Modules Verified:"
    for module in auth-function api-function processor-function shared; do
        if [ -f "$REPORTS_DIR/$module-jml-report.txt" ]; then
            echo "- $module: $(grep -c "error" "$REPORTS_DIR/$module-jml-report.txt" || echo "0") errors"
        fi
    done
    echo ""
    echo "Static Analysis:"
    echo "- SpotBugs: $(grep -c "Bug:" "$REPORTS_DIR/spotbugs-report.txt" 2>/dev/null || echo "0") issues"
} > "$SUMMARY_FILE"

print_status "success" "Verification process completed"
print_status "info" "Reports available in $REPORTS_DIR/"
print_status "info" "Summary: $SUMMARY_FILE"

# Exit with error if any critical issues found
if grep -q "error" "$REPORTS_DIR"/*-jml-report.txt 2>/dev/null; then
    print_status "error" "Verification errors found. Please review the reports."
    exit 1
fi

print_status "success" "All verifications passed!"