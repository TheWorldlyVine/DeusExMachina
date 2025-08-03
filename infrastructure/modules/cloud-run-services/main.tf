# Cloud Run Services module for Novel Services
# This module manages Cloud Run services that are deployed via CI/CD

# Data source to get existing Cloud Run services
data "google_cloud_run_service" "services" {
  for_each = var.services

  name     = each.value.name
  location = var.region
  project  = var.project_id
}

# IAM policy to allow unauthenticated access where needed
resource "google_cloud_run_service_iam_member" "invoker" {
  for_each = { for k, v in var.services : k => v if v.allow_unauthenticated }

  service  = data.google_cloud_run_service.services[each.key].name
  location = var.region
  project  = var.project_id
  role     = "roles/run.invoker"
  member   = "allUsers"
}