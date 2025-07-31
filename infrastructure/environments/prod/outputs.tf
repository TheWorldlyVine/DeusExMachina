# VPC Outputs
output "vpc_network_id" {
  description = "The ID of the VPC network"
  value       = module.vpc.network_id
}

output "vpc_subnet_id" {
  description = "The ID of the private subnet"
  value       = module.vpc.subnet_id
}

# Cloud Functions Outputs
output "auth_function_url" {
  description = "URL of the auth function"
  value       = module.auth_function.function_url
}

output "api_function_url" {
  description = "URL of the API function"
  value       = module.api_function.function_url
}

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