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
  environment  = "prod"
  project_id   = var.project_id
  project_name = "deus-ex-machina"
  region       = "us-central1"

  common_labels = {
    environment = local.environment
    managed-by  = "terraform"
    project     = local.project_name
  }
}

# Data source for project information
data "google_project" "project" {
  project_id = local.project_id
}

# VPC module commented out until needed for Cloud Functions
# module "vpc" {
#   source = "../../modules/vpc"
#
#   project_name        = "deus-ex-machina"
#   region              = local.region
#   private_subnet_cidr = "10.0.0.0/20"
#   pods_cidr           = "10.1.0.0/16"
#   services_cidr       = "10.2.0.0/16"
# }

# Cloud Functions commented out until backend is ready
# module "auth_function" {
#   source = "../../modules/cloud-functions"
#
#   project_id            = local.project_id
#   function_name         = "auth-function"
#   region                = local.region
#   description           = "Authentication service for DeusExMachina"
#   entry_point           = "com.deusexmachina.functions.AuthFunction"
#   source_archive_path   = "../../../apps/backend/auth-function/build/distributions/auth-function.zip"
#   memory                = "512M"
#   min_instances         = 1
#   max_instances         = 100
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
#   function_name         = "api-function"
#   region                = local.region
#   description           = "Main API service for DeusExMachina"
#   entry_point           = "com.deusexmachina.functions.ApiFunction"
#   source_archive_path   = "../../../apps/backend/api-function/build/distributions/api-function.zip"
#   memory                = "1024M"
#   min_instances         = 2
#   max_instances         = 200
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

  # Domain configuration
  domain_name = "god-in-a-box.com"

  # Enable test index.html for initial deployment
  deploy_test_index = true

  # Enable API routing to Cloud Functions
  enable_api_routing = true

  # Basic security features for production
  enable_cloud_armor = true

  # CORS configuration - updated for the new domain
  cors_origins = ["https://god-in-a-box.com", "https://www.god-in-a-box.com"]

  # Cache policies
  cache_policies = {
    html_ttl   = 300      # 5 minutes for HTML
    static_ttl = 31536000 # 1 year for hashed assets
    image_ttl  = 2592000  # 30 days for images
  }

  # Security headers
  security_headers = {
    enable_hsts                 = true
    enable_content_type_options = true
    enable_xss_protection       = true
    enable_frame_options        = true
    csp_policy                  = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://apis.google.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://*.googleapis.com;"
  }

  labels = local.common_labels
}

# Firestore Database
# NOTE: Commented out due to permission issues with GitHub Actions
# The database was created manually - see manual-resources.md
# module "firestore" {
#   source = "../../modules/firestore"
#
#   project_id      = local.project_id
#   region          = local.region
#   deletion_policy = "DELETE" # Use "ABANDON" for production
# }

# Cloud Functions Permissions (functions are deployed via CI/CD)
# Auth Function Permissions
module "auth_function_permissions" {
  source = "../../modules/cloud-functions-permissions"

  project_id            = local.project_id
  region                = local.region
  function_name         = "auth-function"
  allow_unauthenticated = true # Auth endpoints need public access
  enable_firestore      = true
  additional_roles      = []
}

# API Function Permissions
module "api_function_permissions" {
  source = "../../modules/cloud-functions-permissions"

  project_id            = local.project_id
  region                = local.region
  function_name         = "api-function"
  allow_unauthenticated = true # API endpoints need public access (auth handled in-app)
  enable_firestore      = true
  additional_roles      = []
}

# Processor Function Permissions
module "processor_function_permissions" {
  source = "../../modules/cloud-functions-permissions"

  project_id            = local.project_id
  region                = local.region
  function_name         = "processor-function"
  allow_unauthenticated = false # Internal use only
  enable_firestore      = true
  additional_roles      = ["roles/pubsub.subscriber"]
}

# Email Service Infrastructure
module "email_service" {
  source = "../../modules/email-service"

  project_id   = local.project_id
  project_name = local.project_name
  region       = local.region

  # Auth function service account for publishing
  auth_function_service_account = "${data.google_project.project.number}-compute@developer.gserviceaccount.com"

  # Enable DLQ handler when ready
  enable_dlq_handler = false

  labels = local.common_labels
}

# Firestore Indexes for Novel Services
module "firestore_indexes" {
  source = "../../modules/firestore-indexes"

  project_id = local.project_id
}

# Cloud Run Services for Novel Creator
# These services are deployed via CI/CD but we manage their IAM policies here
module "novel_services" {
  source = "../../modules/cloud-run-services"

  project_id = local.project_id
  region     = local.region

  services = {
    document_service = {
      name                  = "novel-document-service"
      allow_unauthenticated = true # Required for CORS preflight
      environment_variables = {
        GCP_PROJECT_ID = local.project_id
      }
    }
    ai_service = {
      name                  = "novel-ai-service"
      allow_unauthenticated = true # Required for CORS preflight
      environment_variables = {
        GCP_PROJECT_ID = local.project_id
      }
    }
    memory_service = {
      name                  = "novel-memory-service"
      allow_unauthenticated = true # Required for CORS preflight
      environment_variables = {
        GCP_PROJECT_ID = local.project_id
      }
    }
  }
}

# Deploy GraphQL Gateway using Terraform
module "graphql_gateway" {
  source = "../../modules/cloud-run-deployment"

  project_id   = local.project_id
  region       = local.region
  service_name = "novel-graphql-gateway"

  # Placeholder image - CI/CD will update this
  initial_image = "gcr.io/cloudrun/hello"

  # Environment variables (CI/CD will add more)
  environment_variables = {
    NODE_ENV             = "production"
    ALLOWED_ORIGINS      = "https://god-in-a-box.com,https://novel-creator.deusexmachina.app,https://deusexmachina.app,http://34.95.119.251,https://34.95.119.251"
    AUTH_SERVICE_URL     = "https://auth-function-xkv3zhqrha-uw.a.run.app"
    DOCUMENT_SERVICE_URL = "https://novel-document-service-xkv3zhqrha-uw.a.run.app"
    MEMORY_SERVICE_URL   = "https://novel-memory-service-xkv3zhqrha-uw.a.run.app"
    AI_SERVICE_URL       = "https://novel-ai-service-xkv3zhqrha-uw.a.run.app"
  }

  allow_unauthenticated = true
  memory                = "512Mi"
  cpu                   = "1"
  timeout_seconds       = 60
  min_instances         = 0
  max_instances         = 10
}

# Artifact Registry for Cloud Run deployments
module "artifact_registry" {
  source = "../../modules/artifact-registry"

  project_id                   = local.project_id
  region                       = local.region
  github_service_account_email = "github-actions-sa@${local.project_id}.iam.gserviceaccount.com"
}

# GitHub Actions Service Account Permissions
# NOTE: This module manages IAM permissions for the GitHub Actions service account.
# It must be applied by a user with IAM admin permissions, not by the service account itself.
# Uncomment and run locally with admin credentials to grant permissions.
#
# module "github_actions" {
#   source = "../../modules/github-actions"
#
#   project_id = local.project_id
#
#   # The service account email should match what's in your GitHub Actions workflow
#   github_service_account_email = "github-actions-sa@${local.project_id}.iam.gserviceaccount.com"
# }
