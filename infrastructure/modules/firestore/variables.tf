variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The region for Firestore"
  type        = string
  default     = "us-central1"
}

variable "deletion_policy" {
  description = "Deletion policy for Firestore database"
  type        = string
  default     = "DELETE"
  validation {
    condition     = contains(["DELETE", "ABANDON"], var.deletion_policy)
    error_message = "Deletion policy must be either DELETE or ABANDON"
  }
}