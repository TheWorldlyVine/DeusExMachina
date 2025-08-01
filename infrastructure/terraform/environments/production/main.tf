terraform {
  required_version = ">= 1.0"
  
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Static hosting for frontend applications
module "static_hosting" {
  source     = "../../modules/static-hosting"
  project_id = var.project_id
  region     = var.region
}

# Outputs
output "frontend_bucket_name" {
  value = module.static_hosting.bucket_name
}

output "frontend_website_url" {
  value = module.static_hosting.website_url
}