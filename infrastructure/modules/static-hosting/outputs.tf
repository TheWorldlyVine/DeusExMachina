output "bucket_name" {
  description = "Name of the Cloud Storage bucket"
  value       = google_storage_bucket.static_site.name
}

output "bucket_url" {
  description = "URL of the Cloud Storage bucket"
  value       = google_storage_bucket.static_site.url
}

output "static_ip_address" {
  description = "The static IP address for the load balancer"
  value       = google_compute_global_address.static_ip.address
}

output "load_balancer_url" {
  description = "URL to access the load balancer"
  value       = "https://${google_compute_global_address.static_ip.address}"
}

output "domain_url" {
  description = "URL with custom domain (if configured)"
  value       = var.domain_name != null ? "https://${var.domain_name}" : null
}

output "cdn_backend_id" {
  description = "ID of the CDN backend bucket"
  value       = google_compute_backend_bucket.static_backend.id
}

output "deployment_instructions" {
  description = "Instructions for deploying static files"
  value       = <<-EOT
    To deploy static files to the bucket:

    1. Build your frontend application:
       pnpm run build

    2. Deploy to the bucket:
       gsutil -m rsync -r -d dist/ gs://${google_storage_bucket.static_site.name}/

    3. Invalidate CDN cache (if needed):
       gcloud compute url-maps invalidate-cdn-cache ${google_compute_url_map.static_url_map.name} --path "/*"

    Access your site at: ${var.domain_name != null ? "https://${var.domain_name}" : "https://${google_compute_global_address.static_ip.address}"}
  EOT
}
