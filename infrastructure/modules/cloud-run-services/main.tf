# Cloud Run Services module for Novel Services
# This module manages Cloud Run services that are deployed via CI/CD

variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region"
  type        = string
  default     = "us-central1"
}

variable "services" {
  description = "Map of Cloud Run services to manage"
  type = map(object({
    name                  = string
    allow_unauthenticated = bool
    environment_variables = map(string)
  }))
}

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

# Output service URLs
output "service_urls" {
  description = "URLs of the Cloud Run services"
  value = {
    for k, v in data.google_cloud_run_service.services : k => v.status[0].url
  }
}

# Output service names
output "service_names" {
  description = "Names of the Cloud Run services"
  value = {
    for k, v in var.services : k => v.name
  }
}