# Email Processor Function Permissions
# The function itself is deployed via GitHub Actions

module "email_processor_function_permissions" {
  source = "../../modules/cloud-functions-permissions"

  function_name = "email-processor-function"
  project_id    = local.project_id
  region        = local.region

  # The email processor needs to be invoked by Pub/Sub, not HTTP
  allow_unauthenticated = false
  
  # Enable Firestore access for storing email logs
  enable_firestore = true
  
  # Additional roles needed for Pub/Sub subscription
  additional_roles = [
    "roles/pubsub.subscriber",
    "roles/secretmanager.secretAccessor"  # For SMTP credentials
  ]
}