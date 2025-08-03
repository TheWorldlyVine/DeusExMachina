variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The region to deploy to"
  type        = string
}

variable "service_name" {
  description = "The name of the Cloud Run service"
  type        = string
}

variable "initial_image" {
  description = "Initial container image (CI/CD will update this)"
  type        = string
  default     = ""
}

variable "environment_variables" {
  description = "Environment variables for the service"
  type        = map(string)
  default     = {}
}

variable "allow_unauthenticated" {
  description = "Allow unauthenticated access"
  type        = bool
  default     = false
}

variable "service_account_email" {
  description = "Service account email"
  type        = string
  default     = null
}

variable "memory" {
  description = "Memory limit (e.g., '512Mi', '1Gi')"
  type        = string
  default     = "512Mi"
}

variable "cpu" {
  description = "CPU limit (e.g., '1', '2')"
  type        = string
  default     = "1"
}

variable "timeout_seconds" {
  description = "Request timeout in seconds"
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

variable "cpu_throttling" {
  description = "Enable CPU throttling"
  type        = bool
  default     = true
}