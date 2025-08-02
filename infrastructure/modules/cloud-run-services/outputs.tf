# Outputs for Cloud Run Services module

output "service_urls" {
  description = "The URLs of the Cloud Run services"
  value = {
    for k, v in data.google_cloud_run_service.services : k => v.status[0].url
  }
}

output "service_names" {
  description = "The names of the Cloud Run services"
  value = {
    for k, v in var.services : k => v.name
  }
}

output "service_regions" {
  description = "The regions where services are deployed"
  value = {
    for k, v in data.google_cloud_run_service.services : k => v.location
  }
}