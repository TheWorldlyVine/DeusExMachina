terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

locals {
  auth_function_name = "${var.project_name}-auth-function"
  labels = merge(var.labels, {
    module = "auth"
    managed-by = "terraform"
  })
}

# Firestore Database for User Storage
resource "google_firestore_database" "auth_db" {
  project     = var.project_id
  name        = var.firestore_database_name
  location_id = var.firestore_location
  type        = "FIRESTORE_NATIVE"

  # Enable delete protection in production
  deletion_policy = var.environment == "prod" ? "PREVENT_DELETION" : "DELETE"
}

# Create Firestore indexes
resource "google_firestore_index" "users_email_index" {
  project    = var.project_id
  database   = google_firestore_database.auth_db.name
  collection = "users"

  fields {
    field_path = "email"
    order      = "ASCENDING"
  }

  fields {
    field_path = "__name__"
    order      = "ASCENDING"
  }
}

resource "google_firestore_index" "permissions_user_resource_index" {
  project    = var.project_id
  database   = google_firestore_database.auth_db.name
  collection = "permissions"

  fields {
    field_path = "grantedTo"
    order      = "ASCENDING"
  }

  fields {
    field_path = "resourceId"
    order      = "ASCENDING"
  }

  fields {
    field_path = "__name__"
    order      = "ASCENDING"
  }
}

# Cloud SQL Instance for Session Storage
resource "google_sql_database_instance" "auth_sessions" {
  count = var.use_cloud_sql ? 1 : 0
  
  name             = "${var.project_name}-auth-sessions-${var.environment}"
  database_version = "POSTGRES_15"
  region           = var.region
  
  settings {
    tier = var.sql_tier
    
    ip_configuration {
      ipv4_enabled    = false
      private_network = var.vpc_network_id
      
      dynamic "authorized_networks" {
        for_each = var.sql_authorized_networks
        content {
          name  = authorized_networks.value.name
          value = authorized_networks.value.value
        }
      }
    }
    
    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = var.environment == "prod"
      transaction_log_retention_days = var.environment == "prod" ? 7 : 1
      
      backup_retention_settings {
        retained_backups = var.environment == "prod" ? 30 : 7
        retention_unit   = "COUNT"
      }
    }
    
    database_flags {
      name  = "max_connections"
      value = "200"
    }
  }
  
  deletion_protection = var.environment == "prod"
}

resource "google_sql_database" "auth_db" {
  count = var.use_cloud_sql ? 1 : 0
  
  name     = "auth"
  instance = google_sql_database_instance.auth_sessions[0].name
}

resource "google_sql_user" "auth_user" {
  count = var.use_cloud_sql ? 1 : 0
  
  name     = "auth-service"
  instance = google_sql_database_instance.auth_sessions[0].name
  password = random_password.sql_password[0].result
}

resource "random_password" "sql_password" {
  count = var.use_cloud_sql ? 1 : 0
  
  length  = 32
  special = true
}

# Store SQL password in Secret Manager
resource "google_secret_manager_secret" "sql_password" {
  count = var.use_cloud_sql ? 1 : 0
  
  secret_id = "${var.project_name}-auth-sql-password"
  
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "sql_password" {
  count = var.use_cloud_sql ? 1 : 0
  
  secret      = google_secret_manager_secret.sql_password[0].id
  secret_data = random_password.sql_password[0].result
}

# Redis Instance for Session Cache
resource "google_redis_instance" "auth_cache" {
  count = var.use_redis ? 1 : 0
  
  name               = "${var.project_name}-auth-cache-${var.environment}"
  tier               = var.redis_tier
  memory_size_gb     = var.redis_memory_size_gb
  region             = var.region
  authorized_network = var.vpc_network_id
  
  redis_version = "REDIS_7_0"
  display_name  = "Auth Session Cache"
  
  maintenance_policy {
    weekly_maintenance_window {
      day = "SUNDAY"
      start_time {
        hours   = 3
        minutes = 0
      }
    }
  }
  
  labels = local.labels
}

# JWT Signing Keys
resource "google_kms_key_ring" "auth_keyring" {
  name     = "${var.project_name}-auth-keyring"
  location = var.region
}

resource "google_kms_crypto_key" "jwt_signing_key" {
  name     = "jwt-signing-key"
  key_ring = google_kms_key_ring.auth_keyring.id
  
  rotation_period = "7776000s" # 90 days
  
  version_template {
    algorithm = "RSA_SIGN_PKCS1_4096_SHA256"
  }
}

# Store JWT secret in Secret Manager (for HMAC if not using KMS)
resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "${var.project_name}-jwt-secret"
  
  replication {
    auto {}
  }
}

resource "random_password" "jwt_secret" {
  length  = 64
  special = false # Base64 safe
}

resource "google_secret_manager_secret_version" "jwt_secret" {
  secret      = google_secret_manager_secret.jwt_secret.id
  secret_data = random_password.jwt_secret.result
}

# SendGrid API Key Secret
resource "google_secret_manager_secret" "sendgrid_api_key" {
  secret_id = "${var.project_name}-sendgrid-api-key"
  
  replication {
    auto {}
  }
}

# Google OAuth Credentials Secrets
resource "google_secret_manager_secret" "google_oauth_client_id" {
  secret_id = "${var.project_name}-google-oauth-client-id"
  
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret" "google_oauth_client_secret" {
  secret_id = "${var.project_name}-google-oauth-client-secret"
  
  replication {
    auto {}
  }
}

# Deploy Auth Function
module "auth_function" {
  source = "../cloud-functions"
  
  project_id              = var.project_id
  region                  = var.region
  function_name           = local.auth_function_name
  description             = "Authentication service for DeusExMachina"
  runtime                 = "java21"
  entry_point             = "com.deusexmachina.functions.AuthFunction"
  source_archive_path     = var.auth_function_source_path
  memory                  = "512M"
  timeout                 = 60
  min_instances           = var.environment == "prod" ? 1 : 0
  max_instances           = var.environment == "prod" ? 100 : 10
  max_concurrent_requests = 80
  
  env_vars = {
    GOOGLE_CLOUD_PROJECT = var.project_id
    ENVIRONMENT          = var.environment
    APP_BASE_URL         = var.app_base_url
    EMAIL_FROM_ADDRESS   = var.email_from_address
    EMAIL_FROM_NAME      = var.email_from_name
    FIRESTORE_DATABASE   = google_firestore_database.auth_db.name
    USE_CLOUD_SQL        = var.use_cloud_sql ? "true" : "false"
    USE_REDIS            = var.use_redis ? "true" : "false"
  }
  
  secret_env_vars = {
    JWT_SECRET = {
      secret_name = google_secret_manager_secret.jwt_secret.secret_id
      version     = "latest"
    }
    SENDGRID_API_KEY = {
      secret_name = google_secret_manager_secret.sendgrid_api_key.secret_id
      version     = "latest"
    }
    GOOGLE_CLIENT_ID = {
      secret_name = google_secret_manager_secret.google_oauth_client_id.secret_id
      version     = "latest"
    }
    GOOGLE_CLIENT_SECRET = {
      secret_name = google_secret_manager_secret.google_oauth_client_secret.secret_id
      version     = "latest"
    }
  }
  
  # Add SQL connection if enabled
  dynamic "secret_env_vars" {
    for_each = var.use_cloud_sql ? { sql = true } : {}
    content {
      CLOUD_SQL_CONNECTION_NAME = {
        secret_name = "${var.project_id}:${var.region}:${google_sql_database_instance.auth_sessions[0].name}"
        version     = "latest"
      }
      SQL_PASSWORD = {
        secret_name = google_secret_manager_secret.sql_password[0].secret_id
        version     = "latest"
      }
    }
  }
  
  # Add Redis connection if enabled
  dynamic "secret_env_vars" {
    for_each = var.use_redis ? { redis = true } : {}
    content {
      REDIS_HOST = {
        secret_name = google_redis_instance.auth_cache[0].host
        version     = "latest"
      }
    }
  }
  
  service_account_roles = [
    "roles/datastore.user",           # Firestore access
    "roles/secretmanager.secretAccessor", # Secret Manager access
    "roles/cloudkms.signerVerifier",  # KMS for JWT signing
    "roles/cloudsql.client",          # Cloud SQL access
    "roles/redis.editor",             # Redis access
  ]
  
  labels = local.labels
}