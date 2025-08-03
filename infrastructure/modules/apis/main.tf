terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

# This module enables required Google Cloud APIs for the project
variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

# Enable Cloud Build API
resource "google_project_service" "cloudbuild" {
  project                    = var.project_id
  service                    = "cloudbuild.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Cloud Run API
resource "google_project_service" "run" {
  project                    = var.project_id
  service                    = "run.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Container Registry API
resource "google_project_service" "containerregistry" {
  project                    = var.project_id
  service                    = "containerregistry.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Artifact Registry API
resource "google_project_service" "artifactregistry" {
  project                    = var.project_id
  service                    = "artifactregistry.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Cloud Functions API
resource "google_project_service" "cloudfunctions" {
  project                    = var.project_id
  service                    = "cloudfunctions.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Compute Engine API (for Load Balancers, CDN, etc.)
resource "google_project_service" "compute" {
  project                    = var.project_id
  service                    = "compute.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Firestore API
resource "google_project_service" "firestore" {
  project                    = var.project_id
  service                    = "firestore.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Cloud Storage API
resource "google_project_service" "storage" {
  project                    = var.project_id
  service                    = "storage.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable IAM API
resource "google_project_service" "iam" {
  project                    = var.project_id
  service                    = "iam.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Service Usage API (for managing other APIs)
resource "google_project_service" "serviceusage" {
  project                    = var.project_id
  service                    = "serviceusage.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Secret Manager API
resource "google_project_service" "secretmanager" {
  project                    = var.project_id
  service                    = "secretmanager.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

# Enable Pub/Sub API
resource "google_project_service" "pubsub" {
  project                    = var.project_id
  service                    = "pubsub.googleapis.com"
  disable_on_destroy         = false
  disable_dependent_services = false
}

output "enabled_apis" {
  value = [
    google_project_service.cloudbuild.service,
    google_project_service.run.service,
    google_project_service.containerregistry.service,
    google_project_service.artifactregistry.service,
    google_project_service.cloudfunctions.service,
    google_project_service.compute.service,
    google_project_service.firestore.service,
    google_project_service.storage.service,
    google_project_service.iam.service,
    google_project_service.serviceusage.service,
    google_project_service.secretmanager.service,
    google_project_service.pubsub.service,
  ]
  description = "List of enabled Google Cloud APIs"
}