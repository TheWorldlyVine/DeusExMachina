output "function_url" {
  description = "The URL of the deployed Cloud Function"
  value       = google_cloudfunctions2_function.function.service_config[0].uri
}

output "function_name" {
  description = "The name of the Cloud Function"
  value       = google_cloudfunctions2_function.function.name
}

output "service_account_email" {
  description = "The email of the function's service account"
  value       = google_service_account.function_sa.email
}