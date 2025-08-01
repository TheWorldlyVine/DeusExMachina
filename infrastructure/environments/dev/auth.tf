# Auth Module Configuration for Development Environment

module "auth" {
  source = "../../modules/auth"

  project_id     = var.project_id
  project_name   = var.project_name
  region         = var.region
  environment    = "dev"
  vpc_network_id = module.vpc.network_id # Assuming VPC module exists

  # Firestore Configuration
  firestore_database_name = "(default)"
  firestore_location      = "nam5"

  # Start with in-memory session storage for dev
  use_cloud_sql = false
  use_redis     = false

  # Auth Function Source
  auth_function_source_path = "${path.root}/../../../apps/backend/auth-function/build/distributions/auth-function.zip"

  # Email Configuration
  email_from_address = "noreply@${var.domain}"
  email_from_name    = "DeusExMachina Dev"
  app_base_url       = "https://dev.${var.domain}"

  labels = {
    environment = "dev"
    team        = "platform"
  }
}

# Output auth function URL for other services
output "auth_function_url" {
  value       = module.auth.auth_function_url
  description = "URL of the auth Cloud Function"
}

# Create placeholder secrets for local development
# In production, these would be manually set or imported
resource "google_secret_manager_secret_version" "sendgrid_api_key_dev" {
  secret      = module.auth.jwt_secret_id
  secret_data = "dev-sendgrid-api-key-placeholder"

  lifecycle {
    ignore_changes = [secret_data]
  }
}

resource "google_secret_manager_secret_version" "google_oauth_client_id_dev" {
  secret      = "${var.project_name}-google-oauth-client-id"
  secret_data = "dev-google-client-id-placeholder"

  lifecycle {
    ignore_changes = [secret_data]
  }
}

resource "google_secret_manager_secret_version" "google_oauth_client_secret_dev" {
  secret      = "${var.project_name}-google-oauth-client-secret"
  secret_data = "dev-google-client-secret-placeholder"

  lifecycle {
    ignore_changes = [secret_data]
  }
}