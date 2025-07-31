variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "function_name" {
  description = "Name of the Cloud Function"
  type        = string
}

variable "region" {
  description = "GCP region for the function"
  type        = string
}

variable "description" {
  description = "Description of the function"
  type        = string
  default     = ""
}

variable "runtime" {
  description = "Runtime for the function"
  type        = string
  default     = "java21"
}

variable "entry_point" {
  description = "Entry point for the function"
  type        = string
}

variable "source_archive_path" {
  description = "Path to the source archive"
  type        = string
  default     = null
}

variable "memory" {
  description = "Memory allocation for the function"
  type        = string
  default     = "512M"
}

variable "timeout" {
  description = "Timeout in seconds"
  type        = number
  default     = 60
}

variable "min_instances" {
  description = "Minimum number of instances"
  type        = number
  default     = 0
}

variable "max_instances" {
  description = "Maximum number of instances"
  type        = number
  default     = 100
}

variable "max_concurrent_requests" {
  description = "Maximum concurrent requests per instance"
  type        = number
  default     = 100
}

variable "env_vars" {
  description = "Environment variables"
  type        = map(string)
  default     = {}
}

variable "secret_env_vars" {
  description = "Secret environment variables"
  type = map(object({
    secret_name = string
    version     = string
  }))
  default = {}
}

variable "ingress_settings" {
  description = "Ingress settings for the function"
  type        = string
  default     = "ALLOW_ALL"
}

variable "allow_unauthenticated" {
  description = "Whether to allow unauthenticated invocations"
  type        = bool
  default     = false
}

variable "service_account_roles" {
  description = "IAM roles to grant to the function's service account"
  type        = list(string)
  default     = []
}

variable "labels" {
  description = "Labels to apply to the function"
  type        = map(string)
  default     = {}
}