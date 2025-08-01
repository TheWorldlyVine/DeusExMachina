output "auth_function_url" {
  description = "URL of the auth Cloud Function"
  value       = module.auth_function.function_url
}

output "auth_function_name" {
  description = "Name of the auth Cloud Function"
  value       = module.auth_function.function_name
}

output "firestore_database_name" {
  description = "Name of the Firestore database"
  value       = google_firestore_database.auth_db.name
}

output "jwt_secret_id" {
  description = "Secret Manager ID for JWT secret"
  value       = google_secret_manager_secret.jwt_secret.secret_id
}

output "jwt_signing_key_name" {
  description = "KMS key name for JWT signing"
  value       = google_kms_crypto_key.jwt_signing_key.name
}

output "sql_connection_name" {
  description = "Cloud SQL connection name"
  value       = var.use_cloud_sql ? google_sql_database_instance.auth_sessions[0].connection_name : null
}

output "redis_host" {
  description = "Redis instance host"
  value       = var.use_redis ? google_redis_instance.auth_cache[0].host : null
}

output "redis_port" {
  description = "Redis instance port"
  value       = var.use_redis ? google_redis_instance.auth_cache[0].port : null
}

output "required_apis" {
  description = "List of required GCP APIs for auth module"
  value = [
    "firestore.googleapis.com",
    "cloudfunctions.googleapis.com",
    "secretmanager.googleapis.com",
    "cloudkms.googleapis.com",
    "sqladmin.googleapis.com",
    "redis.googleapis.com",
  ]
}