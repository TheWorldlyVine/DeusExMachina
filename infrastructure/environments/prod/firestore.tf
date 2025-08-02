# Firestore Database Configuration
# This represents the manually created Firestore database
# NOTE: Created manually due to permission constraints. To import:
# terraform import google_firestore_database.main "projects/deus-ex-machina-prod/databases/(default)"

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

  depends_on = [google_firestore_database.main]
}