variable "project_name" {
  description = "Name of the project"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
}

variable "private_subnet_cidr" {
  description = "CIDR range for private subnet"
  type        = string
  default     = "10.0.0.0/20"
}

variable "pods_cidr" {
  description = "CIDR range for Kubernetes pods"
  type        = string
  default     = "10.1.0.0/16"
}

variable "services_cidr" {
  description = "CIDR range for Kubernetes services"
  type        = string
  default     = "10.2.0.0/16"
}