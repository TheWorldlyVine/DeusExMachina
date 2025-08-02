variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "jwt_secret_value" {
  description = "The JWT secret value (should be provided via environment variable)"
  type        = string
  sensitive   = true
  default     = ""
}