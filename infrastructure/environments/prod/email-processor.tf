# Email Processor Function Permissions
# The function itself is deployed via GitHub Actions

module "email_processor_function_permissions" {
  source = "../../modules/cloud-functions-permissions"

  function_name = "email-processor-function"
  project_id    = local.project_id
  region        = local.region

  # Allow the function to be triggered by Pub/Sub
  allow_pubsub_trigger = true
  pubsub_topic         = module.email_service.email_topic_id

  # Service account (uses default compute service account)
  service_account_email = "email-processor@${local.project_id}.iam.gserviceaccount.com"

  labels = local.common_labels
}