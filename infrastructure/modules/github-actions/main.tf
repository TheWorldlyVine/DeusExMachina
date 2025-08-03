terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

# This module manages IAM permissions for the GitHub Actions service account
# The service account itself is created manually and its key is stored in GitHub secrets

variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "github_service_account_email" {
  description = "The email of the GitHub Actions service account"
  type        = string
  default     = "github-actions-sa@deusexmachina-demo.iam.gserviceaccount.com"
}

# Grant Storage Admin role for deploying static assets
resource "google_project_iam_member" "github_actions_storage_admin" {
  project = var.project_id
  role    = "roles/storage.admin"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Cloud Functions Developer for deploying functions
resource "google_project_iam_member" "github_actions_functions_developer" {
  project = var.project_id
  role    = "roles/cloudfunctions.developer"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Service Account User for using service accounts
resource "google_project_iam_member" "github_actions_service_account_user" {
  project = var.project_id
  role    = "roles/iam.serviceAccountUser"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Compute Network Viewer for CDN cache invalidation
resource "google_project_iam_member" "github_actions_compute_viewer" {
  project = var.project_id
  role    = "roles/compute.networkViewer"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Compute Admin role for managing compute resources including CDN
resource "google_project_iam_member" "github_actions_compute_admin" {
  project = var.project_id
  role    = "roles/compute.admin"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Compute Security Admin for managing Cloud Armor policies
resource "google_project_iam_member" "github_actions_compute_security_admin" {
  project = var.project_id
  role    = "roles/compute.securityAdmin"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Artifact Registry Admin for creating and managing repositories
resource "google_project_iam_member" "github_actions_artifact_registry_admin" {
  project = var.project_id
  role    = "roles/artifactregistry.admin"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Cloud Build Editor for submitting builds
resource "google_project_iam_member" "github_actions_cloudbuild_editor" {
  project = var.project_id
  role    = "roles/cloudbuild.builds.editor"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Cloud Run Admin for deploying services
resource "google_project_iam_member" "github_actions_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant permission to act as the default compute service account for Cloud Run
resource "google_service_account_iam_member" "github_actions_act_as_compute" {
  service_account_id = "${var.project_id}@appspot.gserviceaccount.com"
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${var.github_service_account_email}"
}

output "service_account_email" {
  value       = var.github_service_account_email
  description = "The email of the GitHub Actions service account"
}