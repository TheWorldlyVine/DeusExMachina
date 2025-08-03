variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "github_service_account_email" {
  description = "The email of the GitHub Actions service account"
  type        = string
}

variable "enable_cloud_run_permissions" {
  description = "Whether to grant Cloud Run deployment permissions"
  type        = bool
  default     = true
}

variable "enable_artifact_registry_permissions" {
  description = "Whether to grant Artifact Registry permissions"
  type        = bool
  default     = true
}

variable "enable_api_management" {
  description = "Whether to grant Service Usage Admin role for enabling APIs"
  type        = bool
  default     = false
}