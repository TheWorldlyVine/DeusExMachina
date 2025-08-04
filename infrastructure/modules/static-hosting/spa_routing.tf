# SPA Routing Configuration
# This file handles URL rewrites for Single Page Applications

# Local variables for SPA routing paths
locals {
  # Create a list of all static file extensions we want to serve directly
  static_extensions = [
    "js", "css", "png", "jpg", "jpeg", "gif", "svg", "ico",
    "woff", "woff2", "ttf", "eot", "json", "xml", "txt", "webp"
  ]

  # Generate path patterns for static files
  static_file_patterns = flatten([
    for ext in local.static_extensions : [
      "*.${ext}",
      "*/*.${ext}",
      "*/*/*.${ext}",
      "*/*/*/*.${ext}"
    ]
  ])
}

# Advanced URL Map with SPA routing support
resource "google_compute_url_map" "spa_url_map" {
  count = var.enable_spa_routing ? 1 : 0

  name            = "${var.project_name}-${var.environment}-spa-url-map"
  default_service = google_compute_backend_bucket.static_backend.id

  # Default host rule
  host_rule {
    hosts        = ["*"]
    path_matcher = "spa-path-matcher"
  }

  # Main path matcher with SPA routing logic
  path_matcher {
    name            = "spa-path-matcher"
    default_service = google_compute_backend_bucket.static_backend.id

    # Rule 1: Exact root path
    path_rule {
      paths   = ["/"]
      service = google_compute_backend_bucket.static_backend.id
    }

    # Rule 2: API routes (if enabled) - these bypass SPA routing
    dynamic "path_rule" {
      for_each = var.enable_api_routing ? [1] : []
      content {
        paths   = ["/api/*"]
        service = google_compute_backend_bucket.static_backend.id
      }
    }

    # Rule 3: Static file patterns - serve directly without rewriting
    path_rule {
      paths   = local.static_file_patterns
      service = google_compute_backend_bucket.static_backend.id
    }

    # Rule 4: Known static directories
    path_rule {
      paths = [
        "/assets/*",
        "/static/*",
        "/public/*",
        "/images/*",
        "/fonts/*",
        "/_next/*",      # Next.js static files
        "/.well-known/*" # Well-known paths
      ]
      service = google_compute_backend_bucket.static_backend.id
    }

    # Rule 5: SPA application routes with URL rewriting
    dynamic "path_rule" {
      for_each = var.spa_apps

      content {
        # Match the base path and all sub-paths
        paths = [
          path_rule.value.base_path,
          "${path_rule.value.base_path}/*"
        ]

        route_action {
          # Rewrite to the app's index.html while preserving the original path
          url_rewrite {
            path_template_rewrite = "${path_rule.value.base_path}/index.html"
          }

          weighted_backend_services {
            backend_service = google_compute_backend_bucket.static_backend.id
            weight          = 100

            # Add headers for debugging and client-side routing
            header_action {
              request_headers_to_add {
                header_name  = "X-Original-Path"
                header_value = "{path}"
                replace      = false
              }

              request_headers_to_add {
                header_name  = "X-SPA-App"
                header_value = path_rule.key
                replace      = false
              }
            }
          }
        }
      }
    }

    # Rule 6: Catch-all - serves as-is (for any missed static files)
    path_rule {
      paths   = ["/*"]
      service = google_compute_backend_bucket.static_backend.id
    }
  }

  # URL map test configuration
  dynamic "test" {
    for_each = var.enable_spa_routing ? [1] : []
    content {
      service = google_compute_backend_bucket.static_backend.id
      host    = var.domain_name != null ? var.domain_name : "test.example.com"
      path    = "/test"
    }
  }
}

# Backend bucket with custom 404 handling for SPAs
resource "google_compute_backend_bucket" "spa_backend" {
  count = var.enable_spa_routing ? 1 : 0

  name        = "${var.project_name}-${var.environment}-spa-backend"
  bucket_name = google_storage_bucket.static_site.name
  enable_cdn  = true

  cdn_policy {
    cache_mode        = "CACHE_ALL_STATIC"
    default_ttl       = var.cache_policies.html_ttl
    max_ttl           = 86400
    client_ttl        = var.cache_policies.html_ttl
    negative_caching  = true
    serve_while_stale = 86400

    # Cache 404s for a short time
    negative_caching_policy {
      code = 404
      ttl  = 60
    }

    # Custom cache key policy for SPA routes
    cache_key_policy {
      # Include the original path in cache key
      query_string_whitelist = ["spa_path"]
    }
  }

  # Custom response headers for SPAs
  custom_response_headers = [
    "Cache-Control: no-cache, no-store, must-revalidate",
    "X-Content-Type-Options: nosniff",
    "X-Frame-Options: SAMEORIGIN",
    "X-XSS-Protection: 1; mode=block",
    "X-SPA-Enabled: true"
  ]
}

# Cloud Function for advanced SPA routing (optional)
resource "google_storage_bucket_object" "spa_router_function" {
  count = var.enable_spa_routing && var.enable_advanced_cdn ? 1 : 0

  name   = "functions/spa-router.js"
  bucket = google_storage_bucket.static_site.name

  content = templatefile("${path.module}/templates/spa-router.js.tpl", {
    spa_apps = var.spa_apps
  })

  content_type = "application/javascript"
}

# Edge configuration for SPA routing
resource "google_storage_bucket_object" "spa_edge_config" {
  count = var.enable_spa_routing ? 1 : 0

  name   = ".well-known/spa-config.json"
  bucket = google_storage_bucket.static_site.name

  content = jsonencode({
    version = "1.0"
    apps    = var.spa_apps
    routing = {
      mode              = "spa"
      preserve_path     = true
      fallback_to_index = true
    }
  })

  content_type  = "application/json"
  cache_control = "public, max-age=300"
}