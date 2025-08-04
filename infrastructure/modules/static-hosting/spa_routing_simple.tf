# Simplified SPA Routing Configuration
# Uses Cloud Storage's built-in 404 handling for SPA routing

# Create 404.html files for each SPA app
resource "google_storage_bucket_object" "spa_404_handlers" {
  for_each = var.enable_spa_routing ? var.spa_apps : {}

  name   = "${each.value.base_path}/404.html"
  bucket = google_storage_bucket.static_site.name

  # The 404.html redirects to index.html while preserving the URL
  content = <<-EOT
<!DOCTYPE html>
<html>
<head>
  <script>
    // Preserve the current path for the SPA router
    sessionStorage.setItem('spa-redirect-path', location.pathname);
    // Redirect to the app's index.html
    location.replace('${each.value.base_path}/index.html');
  </script>
</head>
<body>
  Loading...
</body>
</html>
EOT

  content_type  = "text/html"
  cache_control = "no-cache, no-store, must-revalidate"
}
