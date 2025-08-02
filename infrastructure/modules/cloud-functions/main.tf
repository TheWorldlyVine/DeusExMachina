# Cloud Functions module for DeusExMachina

# Import existing service account if it exists
data "google_service_account" "existing" {
  account_id = "${var.project_name}-${var.function_name}-sa"
  project    = var.project_id
}

# Service account for Cloud Functions
resource "google_service_account" "function_sa" {
  count        = can(data.google_service_account.existing.email) ? 0 : 1
  account_id   = "${var.project_name}-${var.function_name}-sa"
  display_name = "Service Account for ${var.function_name}"
  project      = var.project_id
}

locals {
  service_account_email = try(data.google_service_account.existing.email, google_service_account.function_sa[0].email)
}

# IAM roles for the service account
resource "google_project_iam_member" "function_invoker" {
  project = var.project_id
  role    = "roles/cloudfunctions.invoker"
  member  = "serviceAccount:${local.service_account_email}"
}

# Allow service account to access Firestore
resource "google_project_iam_member" "firestore_user" {
  count   = var.enable_firestore ? 1 : 0
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${local.service_account_email}"
}

# Allow service account to write logs
resource "google_project_iam_member" "log_writer" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${local.service_account_email}"
}

# Storage bucket for function source code
resource "google_storage_bucket" "function_bucket" {
  name     = "${var.project_id}-${var.function_name}-source"
  location = var.region
  project  = var.project_id

  uniform_bucket_level_access = true
  force_destroy               = var.force_destroy
}

# Cloud Function
resource "google_cloudfunctions2_function" "function" {
  name        = var.function_name
  location    = var.region
  project     = var.project_id
  description = var.description

  build_config {
    runtime     = var.runtime
    entry_point = var.entry_point

    source {
      storage_source {
        bucket = google_storage_bucket.function_bucket.name
        object = var.source_archive_object
      }
    }
  }

  service_config {
    max_instance_count    = var.max_instances
    min_instance_count    = var.min_instances
    available_memory      = var.memory
    timeout_seconds       = var.timeout
    environment_variables = var.environment_variables

    service_account_email = local.service_account_email
  }

  # Allow unauthenticated access (will be controlled by application-level auth)
  lifecycle {
    replace_triggered_by = [
      google_storage_bucket_object.function_source
    ]
  }
}

# Upload function source code (placeholder - actual deployment via CI/CD)
resource "google_storage_bucket_object" "function_source" {
  name   = "${var.function_name}-${var.source_archive_object}"
  bucket = google_storage_bucket.function_bucket.name
  source = var.source_archive_path
}

# Allow public access to the function
resource "google_cloudfunctions2_function_iam_member" "invoker" {
  project        = google_cloudfunctions2_function.function.project
  location       = google_cloudfunctions2_function.function.location
  cloud_function = google_cloudfunctions2_function.function.name
  role           = "roles/cloudfunctions.invoker"
  member         = "allUsers"
}