#!/bin/bash

echo "=== GCP Debug Script ==="
echo ""

# Check current project
echo "1. Current GCP Project:"
gcloud config get-value project
echo ""

# List all projects
echo "2. All available projects:"
gcloud projects list --format="table(projectId,name,projectNumber)"
echo ""

# List all buckets
echo "3. All buckets in current project:"
gcloud storage buckets list --format="table(name,location,storageClass)"
echo ""

# Check for specific buckets
echo "4. Checking for DeusExMachina buckets:"
gcloud storage buckets list | grep -E "(deus|machina|static)" || echo "No buckets found matching 'deus', 'machina', or 'static'"
echo ""

# Check Terraform state bucket
echo "5. Checking for Terraform state:"
PROJECT_ID=$(gcloud config get-value project)
if gcloud storage buckets describe gs://${PROJECT_ID}-terraform-state &>/dev/null; then
    echo "Terraform state bucket exists: gs://${PROJECT_ID}-terraform-state"
    echo "Checking Terraform state files:"
    gsutil ls -la gs://${PROJECT_ID}-terraform-state/terraform/state/ 2>/dev/null || echo "No state files found"
else
    echo "Terraform state bucket does not exist"
fi
echo ""

# Check service account
echo "6. Current authentication:"
gcloud auth list --filter=status:ACTIVE --format="value(account)"
echo ""

echo "=== End Debug ==="