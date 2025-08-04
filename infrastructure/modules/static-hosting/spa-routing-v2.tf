# SPA Routing v2 - Simple approach using redirects
# This creates route-specific index.html files that redirect to the main app

# Create route handlers for each SPA app (only for non-wildcard routes)
resource "google_storage_bucket_object" "spa_route_handlers" {
  for_each = var.enable_spa_routing ? merge([
    for app_name, app_config in var.spa_apps : {
      for route in app_config.routes :
      "${app_name}/${trimprefix(route, "/")}/index.html" => {
        app_name  = app_name
        base_path = app_config.base_path
        route     = route
      }
      if !endswith(route, "*") && !contains(split("/", route), "*")
    }
  ]...) : {}

  name   = each.key
  bucket = google_storage_bucket.static_site.name

  # Serve the main app's index.html content
  content = <<-EOT
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Loading...</title>
  <script>
    // This file exists to handle direct navigation to SPA routes
    // Load the main app and let the router handle the route
    window.location.replace('${each.value.base_path}/');
  </script>
</head>
<body>
  <noscript>
    <meta http-equiv="refresh" content="0; url=${each.value.base_path}/">
  </noscript>
  <p>Loading application...</p>
</body>
</html>
EOT

  content_type  = "text/html"
  cache_control = "no-cache, no-store, must-revalidate"
}

# Create a catch-all 404.html for each app
resource "google_storage_bucket_object" "spa_app_404" {
  for_each = var.enable_spa_routing ? var.spa_apps : {}

  name   = "${each.value.base_path}/404.html"
  bucket = google_storage_bucket.static_site.name

  content = <<-EOT
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Loading...</title>
  <script>
    // Store the intended path and redirect to app root
    const path = window.location.pathname;
    if (path.startsWith('${each.value.base_path}/')) {
      sessionStorage.setItem('spa-redirect-path', path);
      window.location.replace('${each.value.base_path}/');
    }
  </script>
</head>
<body>
  <noscript>
    <meta http-equiv="refresh" content="0; url=${each.value.base_path}/">
  </noscript>
  <p>Loading application...</p>
</body>
</html>
EOT

  content_type  = "text/html"
  cache_control = "no-cache, no-store, must-revalidate"
}

# Update the main 404.html to handle all apps
resource "google_storage_bucket_object" "main_404" {
  count = var.enable_spa_routing ? 1 : 0

  name   = "404.html"
  bucket = google_storage_bucket.static_site.name

  content = <<-EOT
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Page Not Found</title>
  <script>
    // Check if this is a known SPA route
    const path = window.location.pathname;
    const spaApps = ${jsonencode(
  { for k, v in var.spa_apps : v.base_path => v.base_path }
)};
    
    for (const appPath of Object.values(spaApps)) {
      if (path.startsWith(appPath + '/')) {
        sessionStorage.setItem('spa-redirect-path', path);
        window.location.replace(appPath + '/');
        break;
      }
    }
  </script>
</head>
<body>
  <div style="text-align: center; padding: 50px; font-family: Arial, sans-serif;">
    <h1>404 - Page Not Found</h1>
    <p>The page you are looking for does not exist.</p>
    <p><a href="/">Go to Home</a></p>
  </div>
</body>
</html>
EOT

content_type  = "text/html"
cache_control = "no-cache, no-store, must-revalidate"
}