output "email_topic_id" {
  description = "The ID of the email events topic"
  value       = google_pubsub_topic.email_events.id
}

output "email_topic_name" {
  description = "The name of the email events topic"
  value       = google_pubsub_topic.email_events.name
}

output "dlq_topic_id" {
  description = "The ID of the dead letter queue topic"
  value       = google_pubsub_topic.email_events_dlq.id
}

output "email_subscription_name" {
  description = "The name of the email events subscription"
  value       = google_pubsub_subscription.email_events.name
}

output "dlq_subscription_name" {
  description = "The name of the DLQ subscription"
  value       = google_pubsub_subscription.email_events_dlq.name
}

output "email_processor_service_account" {
  description = "Email of the email processor service account"
  value       = google_service_account.email_processor.email
}

output "schema_id" {
  description = "The ID of the email message schema"
  value       = google_pubsub_schema.email_message.id
}