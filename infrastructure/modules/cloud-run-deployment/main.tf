# Cloud Run Service Deployment Module
# This module creates and manages Cloud Run services

resource "google_cloud_run_service" "service" {
  name     = var.service_name
  location = var.region
  project  = var.project_id

  metadata {
    annotations = {
      # Allow unauthenticated access if specified
      "run.googleapis.com/ingress" = "all"
    }
  }

  template {
    spec {
      containers {
        # Use a placeholder image initially - CI/CD will update this
        image = var.initial_image != "" ? var.initial_image : "gcr.io/cloudrun/hello"
        
        # Environment variables
        dynamic "env" {
          for_each = var.environment_variables
          content {
            name  = env.key
            value = env.value
          }
        }

        # Resource limits
        resources {
          limits = {
            cpu    = var.cpu
            memory = var.memory
          }
        }
      }

      # Service account
      service_account_name = var.service_account_email

      # Timeout
      timeout_seconds = var.timeout_seconds
    }

    metadata {
      annotations = {
        # Autoscaling
        "autoscaling.knative.dev/minScale" = tostring(var.min_instances)
        "autoscaling.knative.dev/maxScale" = tostring(var.max_instances)
        
        # CPU allocation
        "run.googleapis.com/cpu-throttling" = var.cpu_throttling ? "true" : "false"
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }

  lifecycle {
    # Ignore changes to the image since CI/CD will manage it
    ignore_changes = [
      template[0].spec[0].containers[0].image,
      template[0].metadata[0].annotations["client.knative.dev/user-image"],
      template[0].metadata[0].annotations["run.googleapis.com/client-name"],
      template[0].metadata[0].annotations["run.googleapis.com/client-version"],
      metadata[0].annotations["serving.knative.dev/creator"],
      metadata[0].annotations["serving.knative.dev/lastModifier"],
      metadata[0].annotations["run.googleapis.com/launch-stage"],
    ]
  }
}

# IAM binding for unauthenticated access
resource "google_cloud_run_service_iam_member" "invoker" {
  count = var.allow_unauthenticated ? 1 : 0

  service  = google_cloud_run_service.service.name
  location = google_cloud_run_service.service.location
  project  = var.project_id
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# IAM binding for service account
resource "google_cloud_run_service_iam_member" "service_account" {
  count = var.service_account_email != null ? 1 : 0

  service  = google_cloud_run_service.service.name
  location = google_cloud_run_service.service.location
  project  = var.project_id
  role     = "roles/run.invoker"
  member   = "serviceAccount:${var.service_account_email}"
}