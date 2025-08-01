variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "project_name" {
  description = "Name of the project"
  type        = string
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod"
  }
}

variable "region" {
  description = "GCP region for the bucket"
  type        = string
  default     = "us-central1"
}

variable "domain_name" {
  description = "Custom domain name for the frontend (optional)"
  type        = string
  default     = null
}

variable "cors_origins" {
  description = "List of origins allowed for CORS"
  type        = list(string)
  default     = ["*"]
}

variable "enable_api_routing" {
  description = "Enable routing /api/* to Cloud Functions"
  type        = bool
  default     = false
}

variable "cloud_functions_backend_url" {
  description = "URL of the Cloud Functions backend (if enable_api_routing is true)"
  type        = string
  default     = null
}

variable "force_destroy" {
  description = "Allow destruction of bucket with contents"
  type        = bool
  default     = false
}

variable "deploy_test_index" {
  description = "Deploy a test index.html file"
  type        = bool
  default     = false
}

variable "labels" {
  description = "Labels to apply to resources"
  type        = map(string)
  default     = {}
}

variable "cache_policies" {
  description = "Cache policies for different file types"
  type = object({
    html_ttl   = optional(number, 300)      # 5 minutes
    static_ttl = optional(number, 31536000) # 1 year for hashed assets
    image_ttl  = optional(number, 2592000)  # 30 days
  })
  default = {}
}

variable "security_headers" {
  description = "Security headers to apply"
  type = object({
    enable_hsts                 = optional(bool, true)
    enable_content_type_options = optional(bool, true)
    enable_xss_protection       = optional(bool, true)
    enable_frame_options        = optional(bool, true)
    csp_policy                  = optional(string, null)
  })
  default = {}
}

variable "enable_advanced_cdn" {
  description = "Enable advanced CDN configuration"
  type        = bool
  default     = false
}

variable "enable_cloud_armor" {
  description = "Enable Cloud Armor security policies"
  type        = bool
  default     = false
}

variable "blocked_countries" {
  description = "List of country codes to block (e.g., ['CN', 'RU'])"
  type        = list(string)
  default     = []
}

variable "enable_cache_invalidator" {
  description = "Enable automatic cache invalidation function"
  type        = bool
  default     = false
}
