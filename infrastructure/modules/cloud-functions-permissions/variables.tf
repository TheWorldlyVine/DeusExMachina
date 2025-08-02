variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "function_name" {
  description = "The name of the Cloud Function"
  type        = string
}

variable "region" {
  description = "The region where the function is deployed"
  type        = string
}

variable "allow_unauthenticated" {
  description = "Whether to allow unauthenticated access"
  type        = bool
  default     = false
}

variable "enable_firestore" {
  description = "Whether to grant Firestore access"
  type        = bool
  default     = false
}

variable "additional_roles" {
  description = "Additional IAM roles to grant to the function"
  type        = list(string)
  default     = []
}