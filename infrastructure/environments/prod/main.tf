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
    bucket = "deus-ex-machina-terraform-state-prod"
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
  project_id  = "deus-ex-machina-prod"
  region      = "us-central1"
  
  common_labels = {
    environment = local.environment
    managed-by  = "terraform"
    project     = "deus-ex-machina"
  }
}

module "vpc" {
  source = "../../modules/vpc"
  
  project_name        = "deus-ex-machina"
  region             = local.region
  private_subnet_cidr = "10.0.0.0/20"
  pods_cidr          = "10.1.0.0/16"
  services_cidr      = "10.2.0.0/16"
}

module "auth_function" {
  source = "../../modules/cloud-functions"
  
  project_id            = local.project_id
  function_name        = "auth-function"
  region               = local.region
  description          = "Authentication service for DeusExMachina"
  entry_point          = "com.deusexmachina.functions.AuthFunction"
  source_archive_path  = "../../../apps/backend/auth-function/build/distributions/auth-function.zip"
  memory               = "512M"
  min_instances        = 1
  max_instances        = 100
  allow_unauthenticated = true
  
  env_vars = {
    ENVIRONMENT = local.environment
    LOG_LEVEL   = "INFO"
  }
  
  secret_env_vars = {
    JWT_SECRET = {
      secret_name = "jwt-secret"
      version     = "latest"
    }
    DATABASE_URL = {
      secret_name = "database-url"
      version     = "latest"
    }
  }
  
  service_account_roles = [
    "roles/logging.logWriter",
    "roles/monitoring.metricWriter",
    "roles/secretmanager.secretAccessor",
  ]
  
  labels = local.common_labels
}

module "api_function" {
  source = "../../modules/cloud-functions"
  
  project_id            = local.project_id
  function_name        = "api-function"
  region               = local.region
  description          = "Main API service for DeusExMachina"
  entry_point          = "com.deusexmachina.functions.ApiFunction"
  source_archive_path  = "../../../apps/backend/api-function/build/distributions/api-function.zip"
  memory               = "1024M"
  min_instances        = 2
  max_instances        = 200
  allow_unauthenticated = true
  
  env_vars = {
    ENVIRONMENT = local.environment
    LOG_LEVEL   = "INFO"
  }
  
  secret_env_vars = {
    DATABASE_URL = {
      secret_name = "database-url"
      version     = "latest"
    }
  }
  
  service_account_roles = [
    "roles/logging.logWriter",
    "roles/monitoring.metricWriter",
    "roles/secretmanager.secretAccessor",
    "roles/cloudsql.client",
  ]
  
  labels = local.common_labels
}