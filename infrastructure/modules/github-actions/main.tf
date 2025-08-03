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
  count   = var.enable_artifact_registry_permissions ? 1 : 0
  project = var.project_id
  role    = "roles/artifactregistry.admin"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Cloud Build Editor for submitting builds
resource "google_project_iam_member" "github_actions_cloudbuild_editor" {
  count   = var.enable_cloud_run_permissions ? 1 : 0
  project = var.project_id
  role    = "roles/cloudbuild.builds.editor"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Cloud Run Admin for deploying services
resource "google_project_iam_member" "github_actions_run_admin" {
  count   = var.enable_cloud_run_permissions ? 1 : 0
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Service Usage Consumer role for using APIs
resource "google_project_iam_member" "github_actions_service_usage_consumer" {
  project = var.project_id
  role    = "roles/serviceusage.serviceUsageConsumer"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant Service Usage Admin role for enabling APIs (optional)
resource "google_project_iam_member" "github_actions_service_usage_admin" {
  count   = var.enable_api_management ? 1 : 0
  project = var.project_id
  role    = "roles/serviceusage.serviceUsageAdmin"
  member  = "serviceAccount:${var.github_service_account_email}"
}

# Grant permission to act as the default compute service account for Cloud Run
# Using data source to get the actual compute service account
data "google_compute_default_service_account" "default" {
  count   = var.enable_cloud_run_permissions ? 1 : 0
  project = var.project_id
}

resource "google_service_account_iam_member" "github_actions_act_as_compute" {
  count              = var.enable_cloud_run_permissions ? 1 : 0
  service_account_id = "projects/${var.project_id}/serviceAccounts/${data.google_compute_default_service_account.default[0].email}"
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${var.github_service_account_email}"
}

# Grant Cloud Build service account permissions to deploy to Cloud Run
data "google_project" "project" {
  project_id = var.project_id
}

locals {
  cloud_build_service_account = "${data.google_project.project.number}@cloudbuild.gserviceaccount.com"
}

# Grant Cloud Build SA permission to deploy to Cloud Run
resource "google_project_iam_member" "cloudbuild_run_admin" {
  count   = var.enable_cloud_run_permissions ? 1 : 0
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${local.cloud_build_service_account}"
}

# Grant Cloud Build SA permission to act as the compute service account
resource "google_service_account_iam_member" "cloudbuild_act_as_compute" {
  count              = var.enable_cloud_run_permissions ? 1 : 0
  service_account_id = "projects/${var.project_id}/serviceAccounts/${data.google_compute_default_service_account.default[0].email}"
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${local.cloud_build_service_account}"
}

# Grant Cloud Build SA permission to use services
resource "google_project_iam_member" "cloudbuild_service_usage" {
  count   = var.enable_cloud_run_permissions ? 1 : 0
  project = var.project_id
  role    = "roles/serviceusage.serviceUsageConsumer"
  member  = "serviceAccount:${local.cloud_build_service_account}"
}

# Grant Cloud Build SA permission to write logs
resource "google_project_iam_member" "cloudbuild_logs_writer" {
  count   = var.enable_cloud_run_permissions ? 1 : 0
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${local.cloud_build_service_account}"
}

# Grant Cloud Build SA permission to push to Artifact Registry
resource "google_project_iam_member" "cloudbuild_artifact_registry_writer" {
  count   = var.enable_artifact_registry_permissions ? 1 : 0
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${local.cloud_build_service_account}"
}

output "service_account_email" {
  value       = var.github_service_account_email
  description = "The email of the GitHub Actions service account"
}