# Test fixture for static hosting module with SPA routing

terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 4.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

module "static_hosting" {
  source = "../../"

  project_id          = var.project_id
  project_name        = var.project_name
  environment         = var.environment
  region              = var.region
  enable_spa_routing  = var.enable_spa_routing
  spa_apps            = var.spa_apps
  deploy_test_index   = true
  force_destroy       = true
}

variable "project_id" {
  type = string
}

variable "project_name" {
  type = string
}

variable "environment" {
  type    = string
  default = "test"
}

variable "region" {
  type    = string
  default = "us-central1"
}

variable "enable_spa_routing" {
  type    = bool
  default = false
}

variable "spa_apps" {
  type = map(object({
    base_path = string
    routes    = optional(list(string), [])
  }))
  default = {}
}

output "bucket_name" {
  value = module.static_hosting.bucket_name
}

output "static_ip_address" {
  value = module.static_hosting.static_ip_address
}

output "url_map_name" {
  value = module.static_hosting.url_map_name
}