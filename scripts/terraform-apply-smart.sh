#!/bin/bash

# Smart Terraform apply script that handles different error types appropriately
# - Continues on 409 (already exists) errors
# - Fails on 403 (permission denied) or other errors

set -o pipefail

# Run terraform apply and capture output
echo "Running terraform apply..."
if terraform apply -auto-approve tfplan 2>&1 | tee /tmp/terraform-apply.log; then
    echo "Terraform apply succeeded!"
    exit 0
fi

# Terraform failed, analyze the errors
echo ""
echo "Terraform apply failed. Analyzing errors..."

# Count different types of errors
# Look for the actual error lines in Terraform output
error_409_count=$(grep -c "Error 409:" /tmp/terraform-apply.log || true)
error_403_count=$(grep -c "Error 403:" /tmp/terraform-apply.log || true)
error_400_count=$(grep -c "Error 400:" /tmp/terraform-apply.log || true)
provider_error_count=$(grep -c "Provider produced inconsistent result" /tmp/terraform-apply.log || true)

# Count total error blocks (the ╷ and ╵ delimited blocks)
total_error_count=$(grep -c "^╷$" /tmp/terraform-apply.log || true)

# Calculate non-409 errors correctly
non_409_errors=$((total_error_count - error_409_count))
other_errors=$((total_error_count - error_409_count - error_403_count - error_400_count - provider_error_count))

echo "Found errors:"
echo "  - Total error blocks: $total_error_count"
echo "  - 409 (Already Exists): $error_409_count"
echo "  - 403 (Permission Denied): $error_403_count"
echo "  - 400 (Bad Request): $error_400_count"
echo "  - Provider errors: $provider_error_count"
echo "  - Other errors: $other_errors"

# If we only have 409 errors, that's acceptable
if [ "$total_error_count" -gt 0 ] && [ "$non_409_errors" -eq 0 ]; then
    echo ""
    echo "::warning::Only 'already exists' errors found. These are expected for existing resources."
    echo "Continuing with deployment..."
    exit 0
fi

# If we have permission errors, fail
if [ "$error_403_count" -gt 0 ]; then
    echo ""
    echo "::error::Permission denied errors found! The service account needs additional permissions."
    echo "Please grant the necessary permissions to the GitHub Actions service account."
    
    # Extract and display permission errors
    echo ""
    echo "Permission errors:"
    grep -A5 "Error 403:" /tmp/terraform-apply.log | grep -E "(Error 403:|permission|denied)" || true
fi

# If we have bad request errors, fail
if [ "$error_400_count" -gt 0 ]; then
    echo ""
    echo "::error::Bad request errors found! Configuration issues need to be fixed."
    
    # Extract and display 400 errors
    echo ""
    echo "Configuration errors:"
    grep -A5 "Error 400:" /tmp/terraform-apply.log || true
fi

# If we have provider errors, fail
if [ "$provider_error_count" -gt 0 ]; then
    echo ""
    echo "::error::Provider errors found! This may be a Terraform provider bug."
    
    # Extract and display provider errors
    echo ""
    echo "Provider errors:"
    grep -A5 "Provider produced inconsistent result" /tmp/terraform-apply.log || true
fi

if [ "$non_409_errors" -gt 0 ]; then
    echo ""
    echo "::error::Critical errors found that prevent deployment."
    echo "Please review the errors above and fix them before retrying."
    exit 1
fi

# This shouldn't happen, but just in case
echo "::error::Unexpected error state. Please review the Terraform output."
exit 1