locals {
  function_name_normalized = replace(var.function_name, "-", "_")
}

resource "google_service_account" "function_sa" {
  account_id   = "${var.function_name}-sa"
  display_name = "Service Account for ${var.function_name}"
  description  = "Service account for Cloud Function ${var.function_name}"
}

resource "google_project_iam_member" "function_roles" {
  for_each = toset(var.service_account_roles)
  
  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.function_sa.email}"
}

resource "google_storage_bucket" "source" {
  name          = "${var.project_id}-${var.function_name}-source"
  location      = var.region
  force_destroy = true

  uniform_bucket_level_access = true

  lifecycle_rule {
    condition {
      age = 7
    }
    action {
      type = "Delete"
    }
  }
}

resource "google_storage_bucket_object" "function_source" {
  name   = "${var.function_name}-${filemd5(var.source_archive_path)}.zip"
  bucket = google_storage_bucket.source.name
  source = var.source_archive_path
}

resource "google_cloudfunctions2_function" "function" {
  name        = var.function_name
  location    = var.region
  description = var.description
  
  build_config {
    runtime     = var.runtime
    entry_point = var.entry_point
    source {
      storage_source {
        bucket = google_storage_bucket.source.name
        object = google_storage_bucket_object.function_source.name
      }
    }
  }
  
  service_config {
    max_instance_count               = var.max_instances
    min_instance_count               = var.min_instances
    available_memory                 = var.memory
    timeout_seconds                  = var.timeout
    service_account_email            = google_service_account.function_sa.email
    ingress_settings                 = var.ingress_settings
    all_traffic_on_latest_revision   = true
    max_instance_request_concurrency = var.max_concurrent_requests
    
    environment_variables = var.env_vars
    
    dynamic "secret_environment_variables" {
      for_each = var.secret_env_vars
      content {
        key        = secret_environment_variables.key
        project_id = var.project_id
        secret     = secret_environment_variables.value.secret_name
        version    = secret_environment_variables.value.version
      }
    }
  }

  labels = merge(
    var.labels,
    {
      managed-by = "terraform"
      function   = local.function_name_normalized
    }
  )
}

resource "google_cloud_run_service_iam_member" "invoker" {
  count = var.allow_unauthenticated ? 1 : 0

  project  = google_cloudfunctions2_function.function.project
  location = google_cloudfunctions2_function.function.location
  service  = google_cloudfunctions2_function.function.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}