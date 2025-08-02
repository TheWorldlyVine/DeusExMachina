terraform {
  required_version = ">= 1.6.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

# Main email events topic
resource "google_pubsub_topic" "email_events" {
  name = "${var.project_name}-email-events"

  message_retention_duration = var.message_retention_duration

  schema_settings {
    schema   = google_pubsub_schema.email_message.id
    encoding = "JSON"
  }

  labels = merge(var.labels, {
    component = "email-service"
    type      = "events"
  })
}

# Dead letter topic for failed emails
resource "google_pubsub_topic" "email_events_dlq" {
  name = "${var.project_name}-email-events-dlq"

  message_retention_duration = "2592000s" # 30 days

  labels = merge(var.labels, {
    component = "email-service"
    type      = "dead-letter"
  })
}

# Schema for email messages
resource "google_pubsub_schema" "email_message" {
  name = "${var.project_name}-email-message-schema"
  type = "AVRO"

  definition = jsonencode({
    type = "record"
    name = "EmailMessage"
    fields = [
      {
        name = "messageId"
        type = "string"
        doc  = "Unique identifier for the message"
      },
      {
        name = "timestamp"
        type = "string"
        doc  = "ISO 8601 timestamp"
      },
      {
        name = "emailType"
        type = "string"
        doc  = "Type of email (e.g., VERIFICATION_EMAIL, ORDER_CONFIRMATION)"
      },
      {
        name = "recipient"
        type = {
          type = "record"
          name = "Recipient"
          fields = [
            { name = "email", type = "string" },
            { name = "displayName", type = ["null", "string"], default = null }
          ]
        }
      },
      {
        name = "sender"
        type = ["null", {
          type = "record"
          name = "Sender"
          fields = [
            { name = "email", type = "string" },
            { name = "name", type = ["null", "string"], default = null }
          ]
        }]
        default = null
        doc     = "Optional custom sender, defaults to system sender"
      },
      {
        name = "templateData"
        type = {
          type   = "map"
          values = "string"
        }
        doc = "Template variables for email rendering"
      },
      {
        name = "metadata"
        type = {
          type = "record"
          name = "Metadata"
          fields = [
            { name = "userId", type = ["null", "string"], default = null },
            { name = "correlationId", type = "string" },
            { name = "retryCount", type = "int", default = 0 },
            { name = "source", type = "string", doc = "Source service/function" },
            { name = "priority", type = "string", default = "normal", doc = "Email priority: high, normal, low" }
          ]
        }
      },
      {
        name = "attachments"
        type = ["null", {
          type = "array"
          items = {
            type = "record"
            name = "Attachment"
            fields = [
              { name = "filename", type = "string" },
              { name = "contentType", type = "string" },
              { name = "data", type = "string", doc = "Base64 encoded data" }
            ]
          }
        }]
        default = null
        doc     = "Optional file attachments"
      }
    ]
  })
}

# Main subscription for email processing
resource "google_pubsub_subscription" "email_events" {
  name  = "${var.project_name}-email-events-sub"
  topic = google_pubsub_topic.email_events.id

  ack_deadline_seconds = 60

  retry_policy {
    minimum_backoff = var.retry_minimum_backoff
    maximum_backoff = var.retry_maximum_backoff
  }

  dead_letter_policy {
    dead_letter_topic     = google_pubsub_topic.email_events_dlq.id
    max_delivery_attempts = var.max_delivery_attempts
  }

  expiration_policy {
    ttl = "" # Never expire
  }

  # Enable exactly once delivery for critical emails
  enable_exactly_once_delivery = true

  labels = merge(var.labels, {
    component = "email-service"
    type      = "subscription"
  })
}

# Subscription for dead letter processing
resource "google_pubsub_subscription" "email_events_dlq" {
  name  = "${var.project_name}-email-events-dlq-sub"
  topic = google_pubsub_topic.email_events_dlq.id

  ack_deadline_seconds = 300 # 5 minutes for manual processing

  expiration_policy {
    ttl = "" # Never expire
  }

  labels = merge(var.labels, {
    component = "email-service"
    type      = "dead-letter-subscription"
  })
}

# Service account for Application Integration
resource "google_service_account" "email_processor" {
  account_id   = "${var.project_name}-email-processor"
  display_name = "Email Processor Service Account"
  description  = "Service account for Application Integration email workflow"
}

# IAM roles for the service account
resource "google_project_iam_member" "email_processor_subscriber" {
  project = var.project_id
  role    = "roles/pubsub.subscriber"
  member  = "serviceAccount:${google_service_account.email_processor.email}"
}

resource "google_project_iam_member" "email_processor_viewer" {
  project = var.project_id
  role    = "roles/pubsub.viewer"
  member  = "serviceAccount:${google_service_account.email_processor.email}"
}

# Grant auth function permission to publish
resource "google_pubsub_topic_iam_member" "auth_function_publisher" {
  topic  = google_pubsub_topic.email_events.id
  role   = "roles/pubsub.publisher"
  member = "serviceAccount:${var.auth_function_service_account}"
}

# Dead letter handler Cloud Function
resource "google_cloudfunctions2_function" "dlq_handler" {
  count = var.enable_dlq_handler ? 1 : 0

  name        = "${var.project_name}-email-dlq-handler"
  location    = var.region
  description = "Handles dead letter emails for manual intervention"

  build_config {
    runtime     = "java21"
    entry_point = "com.deusexmachina.email.DeadLetterHandler"
    source {
      storage_source {
        bucket = var.functions_bucket
        object = var.dlq_handler_source_object
      }
    }
  }

  service_config {
    max_instance_count    = 10
    available_memory      = "256M"
    timeout_seconds       = 300
    service_account_email = google_service_account.email_processor.email

    environment_variables = {
      PROJECT_ID         = var.project_id
      ALERT_EMAIL        = var.alert_email
      MONITORING_ENABLED = "true"
    }
  }

  event_trigger {
    event_type   = "google.cloud.pubsub.topic.v1.messagePublished"
    pubsub_topic = google_pubsub_topic.email_events_dlq.id
    retry_policy = "RETRY_POLICY_DO_NOT_RETRY"
  }

  labels = merge(var.labels, {
    component = "email-service"
    type      = "dlq-handler"
  })
}