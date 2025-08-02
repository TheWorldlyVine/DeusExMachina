# Firestore Database Configuration
# NOTE: This resource is managed separately to avoid "already exists" errors
# The database was created manually and should not be recreated

# IMPORTANT: Tagged with 'existing-resource' to skip in CI/CD
resource "google_firestore_database" "main" {
  project     = var.project_id
  name        = "(default)"
  location_id = "us-central1"
  type        = "FIRESTORE_NATIVE"

  # Enable delete protection in production
  deletion_policy = "DELETE"

  # Point in time recovery
  point_in_time_recovery_enablement = "POINT_IN_TIME_RECOVERY_ENABLED"

  # Prevent accidental deletion
  lifecycle {
    prevent_destroy = true
  }
}

# IAM binding for Cloud Functions to access Firestore
resource "google_project_iam_member" "firestore_user" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${data.google_project.project.number}-compute@developer.gserviceaccount.com"
}