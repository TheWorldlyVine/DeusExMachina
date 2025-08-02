variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "project_name" {
  description = "The project name for resource naming"
  type        = string
  default     = "deusexmachina"
}

variable "function_name" {
  description = "The name of the Cloud Function"
  type        = string
}

variable "region" {
  description = "The region to deploy the function"
  type        = string
}

variable "description" {
  description = "Description of the function"
  type        = string
  default     = ""
}

variable "runtime" {
  description = "The runtime for the function"
  type        = string
  default     = "java21"
}

variable "entry_point" {
  description = "The entry point for the function"
  type        = string
}

variable "memory" {
  description = "Memory allocation for the function"
  type        = string
  default     = "256Mi"
}

variable "timeout" {
  description = "Timeout in seconds"
  type        = number
  default     = 60
}

variable "max_instances" {
  description = "Maximum number of instances"
  type        = number
  default     = 100
}

variable "min_instances" {
  description = "Minimum number of instances"
  type        = number
  default     = 0
}

variable "environment_variables" {
  description = "Environment variables for the function"
  type        = map(string)
  default     = {}
}

variable "source_archive_path" {
  description = "Path to the source code archive"
  type        = string
}

variable "source_archive_object" {
  description = "Name of the source archive object in the bucket"
  type        = string
  default     = "source.zip"
}

variable "enable_firestore" {
  description = "Whether to grant Firestore access"
  type        = bool
  default     = false
}

variable "force_destroy" {
  description = "Force destroy the bucket on deletion"
  type        = bool
  default     = false
}