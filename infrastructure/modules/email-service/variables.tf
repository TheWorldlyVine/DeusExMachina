variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "project_name" {
  description = "The project name used for resource naming"
  type        = string
}

variable "region" {
  description = "The GCP region"
  type        = string
}

variable "message_retention_duration" {
  description = "How long to retain messages in the topic"
  type        = string
  default     = "604800s" # 7 days
}

variable "retry_minimum_backoff" {
  description = "Minimum backoff for retry policy"
  type        = string
  default     = "60s"
}

variable "retry_maximum_backoff" {
  description = "Maximum backoff for retry policy"
  type        = string
  default     = "600s" # 10 minutes (max allowed by Pub/Sub)
}

variable "max_delivery_attempts" {
  description = "Maximum delivery attempts before sending to DLQ"
  type        = number
  default     = 3
}

variable "auth_function_service_account" {
  description = "Service account email for auth function"
  type        = string
}

variable "enable_dlq_handler" {
  description = "Whether to deploy the dead letter queue handler function"
  type        = bool
  default     = false # Enable when we have the function code ready
}

variable "functions_bucket" {
  description = "GCS bucket for Cloud Functions source code"
  type        = string
  default     = ""
}

variable "dlq_handler_source_object" {
  description = "GCS object path for DLQ handler function source"
  type        = string
  default     = ""
}

variable "alert_email" {
  description = "Email address for DLQ alerts"
  type        = string
  default     = "ops@deusexmachina.com"
}

variable "labels" {
  description = "Labels to apply to all resources"
  type        = map(string)
  default     = {}
}