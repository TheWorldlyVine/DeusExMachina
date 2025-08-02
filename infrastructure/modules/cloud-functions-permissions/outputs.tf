output "function_url" {
  description = "The URL of the Cloud Function"
  value       = data.google_cloudfunctions2_function.existing.service_config[0].uri
}

output "service_account_email" {
  description = "The service account email of the function"
  value       = data.google_cloudfunctions2_function.existing.service_config[0].service_account_email
}