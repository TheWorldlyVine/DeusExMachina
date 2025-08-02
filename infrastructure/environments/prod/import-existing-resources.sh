#!/bin/bash
set -e

# Script to import existing resources into Terraform state
# This prevents "resource already exists" errors

echo "Importing existing resources into Terraform state..."

PROJECT_ID="deus-ex-machina-prod"
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')

# Initialize Terraform if not already done
if [ ! -d ".terraform" ]; then
    echo "Initializing Terraform..."
    terraform init
fi

# Function to safely import a resource
import_resource() {
    local resource_type=$1
    local resource_name=$2
    local resource_id=$3
    
    echo "Checking if $resource_type.$resource_name exists in state..."
    if terraform state show "$resource_type.$resource_name" >/dev/null 2>&1; then
        echo "✓ $resource_type.$resource_name already in state"
    else
        echo "→ Importing $resource_type.$resource_name..."
        if terraform import "$resource_type.$resource_name" "$resource_id" >/dev/null 2>&1; then
            echo "✓ Successfully imported $resource_type.$resource_name"
        else
            echo "✗ Failed to import $resource_type.$resource_name (might not exist)"
        fi
    fi
}

# Import Firestore database
import_resource "google_firestore_database" "main" "projects/$PROJECT_ID/databases/(default)"

# Import JWT secret
import_resource "google_secret_manager_secret" "jwt_secret" "projects/$PROJECT_ID/secrets/jwt-secret"

# Import SendGrid API key secret
import_resource "google_secret_manager_secret" "sendgrid_api_key" "projects/$PROJECT_ID/secrets/sendgrid-api-key"

# Import IAM bindings
import_resource "google_project_iam_member" "firestore_user" \
    "$PROJECT_ID roles/datastore.user serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

import_resource "google_project_iam_member" "secret_accessor" \
    "$PROJECT_ID roles/secretmanager.secretAccessor serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

echo "Import complete!"