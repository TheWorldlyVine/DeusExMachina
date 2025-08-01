# Google Cloud Storage bucket for static frontend hosting
resource "google_storage_bucket" "frontend_static" {
  name          = "${var.project_id}-frontend-static"
  location      = var.region
  force_destroy = false

  website {
    main_page_suffix = "index.html"
    not_found_page   = "404.html"
  }

  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD"]
    response_header = ["*"]
    max_age_seconds = 3600
  }

  uniform_bucket_level_access = true
}

# Make bucket publicly readable
resource "google_storage_bucket_iam_member" "public_read" {
  bucket = google_storage_bucket.frontend_static.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}

# Output the bucket name
output "bucket_name" {
  value = google_storage_bucket.frontend_static.name
}

# Output the website URL
output "website_url" {
  value = "https://storage.googleapis.com/${google_storage_bucket.frontend_static.name}"
}