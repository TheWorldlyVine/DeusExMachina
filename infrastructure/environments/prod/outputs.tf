# VPC Outputs (commented until VPC module is enabled)
# output "vpc_network_id" {
#   description = "The ID of the VPC network"
#   value       = module.vpc.network_id
# }

# output "vpc_subnet_id" {
#   description = "The ID of the private subnet"
#   value       = module.vpc.subnet_id
# }

# Cloud Functions Outputs (commented until functions are enabled)
# output "auth_function_url" {
#   description = "URL of the auth function"
#   value       = module.auth_function.function_url
# }

# output "api_function_url" {
#   description = "URL of the API function"
#   value       = module.api_function.function_url
# }

# Static Hosting Outputs
output "static_hosting_bucket" {
  description = "Name of the static hosting bucket"
  value       = module.static_hosting.bucket_name
}

output "static_hosting_ip" {
  description = "Static IP address for the frontend"
  value       = module.static_hosting.static_ip_address
}

output "static_hosting_url" {
  description = "URL to access the static site"
  value       = module.static_hosting.load_balancer_url
}

output "deployment_instructions" {
  description = "Instructions for deploying the frontend"
  value       = module.static_hosting.deployment_instructions
}

# Firestore Outputs
# NOTE: Commented out as Firestore module is disabled
# output "firestore_database_name" {
#   description = "Name of the Firestore database"
#   value       = module.firestore.database_name
# }

# Cloud Functions Outputs
output "auth_function_url" {
  description = "URL of the auth function"
  value       = module.auth_function_permissions.function_url
}

output "api_function_url" {
  description = "URL of the API function"
  value       = module.api_function_permissions.function_url
}

output "processor_function_url" {
  description = "URL of the processor function"
  value       = module.processor_function_permissions.function_url
}

# Email Service Outputs
output "email_topic_name" {
  description = "The name of the email events topic"
  value       = module.email_service.email_topic_name
}

output "email_subscription_name" {
  description = "The name of the email events subscription"
  value       = module.email_service.email_subscription_name
}

# Novel Services Outputs
output "novel_service_urls" {
  description = "URLs for the novel creator services"
  value       = module.novel_services.service_urls
}

output "novel_service_names" {
  description = "Names of the novel creator services"
  value       = module.novel_services.service_names
}

output "github_actions_service_account" {
  value       = module.github_actions.service_account_email
  description = "Email of the GitHub Actions service account with permissions"
}

output "enabled_apis" {
  value       = module.apis.enabled_apis
  description = "List of Google Cloud APIs enabled by Terraform"
}

# GraphQL Gateway Output
output "graphql_gateway_url" {
  description = "URL of the GraphQL Gateway"
  value       = module.graphql_gateway.service_url
}

# Artifact Registry Output
# output "artifact_registry_url" {
#   description = "URL of the Artifact Registry repository for Cloud Run"
#   value       = module.artifact_registry.repository_url
# }