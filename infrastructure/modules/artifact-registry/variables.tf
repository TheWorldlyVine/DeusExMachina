variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The region for the repository"
  type        = string
}

variable "github_service_account_email" {
  description = "Email of the GitHub Actions service account"
  type        = string
}