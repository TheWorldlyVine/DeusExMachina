# Module to manage permissions for existing Cloud Functions

# Data source to get the existing function
data "google_cloudfunctions2_function" "existing" {
  name     = var.function_name
  location = var.region
  project  = var.project_id
}

# Get the underlying Cloud Run service
data "google_cloud_run_service" "function_service" {
  name     = data.google_cloudfunctions2_function.existing.name
  location = var.region
  project  = var.project_id
}

# Allow public access to the function (for auth endpoints)
resource "google_cloud_run_service_iam_member" "invoker" {
  count    = var.allow_unauthenticated ? 1 : 0
  project  = var.project_id
  location = var.region
  service  = data.google_cloud_run_service.function_service.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Grant Firestore access to the function's service account
resource "google_project_iam_member" "firestore_user" {
  count   = var.enable_firestore ? 1 : 0
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${data.google_cloudfunctions2_function.existing.service_config[0].service_account_email}"
}

# Grant additional roles as needed
resource "google_project_iam_member" "additional_roles" {
  for_each = toset(var.additional_roles)
  
  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${data.google_cloudfunctions2_function.existing.service_config[0].service_account_email}"
}