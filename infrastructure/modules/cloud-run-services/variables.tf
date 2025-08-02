# Variables for Cloud Run Services module

variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region for Cloud Run services"
  type        = string
  default     = "us-central1"
}

variable "services" {
  description = "Configuration for Cloud Run services"
  type = map(object({
    name                  = string
    allow_unauthenticated = bool
    environment_variables = map(string)
  }))
  default = {}
}

variable "project_name" {
  description = "The project name for resource naming"
  type        = string
  default     = "deusexmachina"
}