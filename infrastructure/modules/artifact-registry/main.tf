# Artifact Registry Repository for Cloud Run deployments
resource "google_artifact_registry_repository" "cloud_run_source_deploy" {
  location      = var.region
  repository_id = "cloud-run-source-deploy"
  description   = "Docker repository for Cloud Run source deployments"
  format        = "DOCKER"
  project       = var.project_id
  
  # Allow project-level access
  cleanup_policies {
    id     = "delete-old-images"
    action = "DELETE"
    
    condition {
      older_than = "2592000s" # 30 days
    }
  }
}

# Grant GitHub Actions service account permissions
resource "google_artifact_registry_repository_iam_member" "github_actions_writer" {
  location   = google_artifact_registry_repository.cloud_run_source_deploy.location
  repository = google_artifact_registry_repository.cloud_run_source_deploy.name
  role       = "roles/artifactregistry.writer"
  member     = "serviceAccount:${var.github_service_account_email}"
  project    = var.project_id
}