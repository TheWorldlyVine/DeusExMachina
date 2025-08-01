terraform {
  required_version = ">= 1.6.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.10.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 5.10.0"
    }
  }

  backend "gcs" {
    # This will be configured during terraform init
    # bucket = "your-project-id-terraform-state"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = local.project_id
  region  = local.region
}

provider "google-beta" {
  project = local.project_id
  region  = local.region
}

locals {
  environment = "prod"
  project_id  = var.project_id
  region      = "us-central1"

  common_labels = {
    environment = local.environment
    managed-by  = "terraform"
    project     = "deus-ex-machina"
  }
}

# VPC module commented out until needed for Cloud Functions
# module "vpc" {
#   source = "../../modules/vpc"
#   
#   project_name        = "deus-ex-machina"
#   region             = local.region
#   private_subnet_cidr = "10.0.0.0/20"
#   pods_cidr          = "10.1.0.0/16"
#   services_cidr      = "10.2.0.0/16"
# }

# Cloud Functions commented out until backend is ready
# module "auth_function" {
#   source = "../../modules/cloud-functions"
#   
#   project_id            = local.project_id
#   function_name        = "auth-function"
#   region               = local.region
#   description          = "Authentication service for DeusExMachina"
#   entry_point          = "com.deusexmachina.functions.AuthFunction"
#   source_archive_path  = "../../../apps/backend/auth-function/build/distributions/auth-function.zip"
#   memory               = "512M"
#   min_instances        = 1
#   max_instances        = 100
#   allow_unauthenticated = true
#   
#   env_vars = {
#     ENVIRONMENT = local.environment
#     LOG_LEVEL   = "INFO"
#   }
#   
#   secret_env_vars = {
#     JWT_SECRET = {
#       secret_name = "jwt-secret"
#       version     = "latest"
#     }
#     DATABASE_URL = {
#       secret_name = "database-url"
#       version     = "latest"
#     }
#   }
#   
#   service_account_roles = [
#     "roles/logging.logWriter",
#     "roles/monitoring.metricWriter",
#     "roles/secretmanager.secretAccessor",
#   ]
#   
#   labels = local.common_labels
# }

# module "api_function" {
#   source = "../../modules/cloud-functions"
#   
#   project_id            = local.project_id
#   function_name        = "api-function"
#   region               = local.region
#   description          = "Main API service for DeusExMachina"
#   entry_point          = "com.deusexmachina.functions.ApiFunction"
#   source_archive_path  = "../../../apps/backend/api-function/build/distributions/api-function.zip"
#   memory               = "1024M"
#   min_instances        = 2
#   max_instances        = 200
#   allow_unauthenticated = true
#   
#   env_vars = {
#     ENVIRONMENT = local.environment
#     LOG_LEVEL   = "INFO"
#   }
#   
#   secret_env_vars = {
#     DATABASE_URL = {
#       secret_name = "database-url"
#       version     = "latest"
#     }
#   }
#   
#   service_account_roles = [
#     "roles/logging.logWriter",
#     "roles/monitoring.metricWriter",
#     "roles/secretmanager.secretAccessor",
#     "roles/cloudsql.client",
#   ]
#   
#   labels = local.common_labels
# }

# Static Hosting for Frontend
module "static_hosting" {
  source = "../../modules/static-hosting"

  project_id   = local.project_id
  project_name = "deus-ex-machina"
  environment  = local.environment
  region       = local.region

  # Enable test index.html for initial deployment
  deploy_test_index = true

  # Enable API routing to Cloud Functions
  enable_api_routing = true

  # Basic security features for production
  enable_cloud_armor = true

  # CORS configuration - adjust as needed
  cors_origins = ["https://deus-ex-machina.com", "https://www.deus-ex-machina.com"]

  # Cache policies
  cache_policies = {
    html_ttl   = 300      # 5 minutes for HTML
    static_ttl = 31536000 # 1 year for hashed assets
    image_ttl  = 2592000  # 30 days for images
  }

  # Security headers
  security_headers = {
    enable_hsts               = true
    enable_content_type_options = true
    enable_xss_protection     = true
    enable_frame_options      = true
    csp_policy               = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://apis.google.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://*.googleapis.com;"
  }

  labels = local.common_labels
}