variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "project_name" {
  description = "The project name used for resource naming"
  type        = string
}

variable "region" {
  description = "The GCP region for resources"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod"
  }
}

variable "vpc_network_id" {
  description = "VPC network ID for private resources"
  type        = string
  default     = null
}

# Firestore Configuration
variable "firestore_database_name" {
  description = "Name of the Firestore database"
  type        = string
  default     = "(default)"
}

variable "firestore_location" {
  description = "Location for Firestore database"
  type        = string
  default     = "nam5" # US multi-region
}

# Cloud SQL Configuration
variable "use_cloud_sql" {
  description = "Whether to use Cloud SQL for session storage"
  type        = bool
  default     = false # Start with in-memory, enable for production
}

variable "sql_tier" {
  description = "Cloud SQL instance tier"
  type        = string
  default     = "db-f1-micro"
}

variable "sql_authorized_networks" {
  description = "Authorized networks for Cloud SQL"
  type = list(object({
    name  = string
    value = string
  }))
  default = []
}

# Redis Configuration
variable "use_redis" {
  description = "Whether to use Redis for session caching"
  type        = bool
  default     = false # Start without caching
}

variable "redis_tier" {
  description = "Redis instance tier"
  type        = string
  default     = "BASIC"
}

variable "redis_memory_size_gb" {
  description = "Redis memory size in GB"
  type        = number
  default     = 1
}

# Auth Function Configuration
variable "auth_function_source_path" {
  description = "Path to the auth function source archive"
  type        = string
}

# Email Configuration
variable "email_from_address" {
  description = "From email address for auth emails"
  type        = string
  default     = "noreply@god-in-a-box.com"
}

variable "email_from_name" {
  description = "From name for auth emails"
  type        = string
  default     = "God in a Box"
}

variable "app_base_url" {
  description = "Base URL of the application for email links"
  type        = string
}

# Labels
variable "labels" {
  description = "Labels to apply to all resources"
  type        = map(string)
  default     = {}
}