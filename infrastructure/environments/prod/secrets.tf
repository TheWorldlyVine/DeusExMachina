# Secret Manager Resources
# This represents the manually created secrets

resource "google_secret_manager_secret" "jwt_secret" {
  project   = var.project_id
  secret_id = "jwt-secret"
  
  replication {
    auto {}
  }
}

# Note: The actual secret version should be created manually or via CI/CD
# to avoid storing sensitive data in Terraform state
resource "google_secret_manager_secret_version" "jwt_secret" {
  secret      = google_secret_manager_secret.jwt_secret.id
  secret_data = var.jwt_secret_value # This should be provided via environment variable
  
  lifecycle {
    ignore_changes = [secret_data]
  }
}

# IAM binding for Cloud Functions to access secrets
resource "google_project_iam_member" "secret_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${data.google_project.project.number}-compute@developer.gserviceaccount.com"
  
  depends_on = [google_secret_manager_secret.jwt_secret]
}