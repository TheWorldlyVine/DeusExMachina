# SPA Routing Configuration for Single Page Applications
# This file handles the routing configuration to ensure SPAs work correctly
# with client-side routing when deployed to Cloud Storage + Load Balancer

# Custom path matcher for SPA routes
# This ensures all routes under specific SPA paths are served the index.html
locals {
  # Define SPA applications and their base paths
  spa_apps = {
    novel_creator = "/novel-creator"
    web_app      = "/web-app"
    # Add more SPA apps here as needed
  }
}

# Create a more comprehensive URL map that handles SPA routing
resource "google_compute_url_map" "static_url_map_spa" {
  count = var.enable_spa_routing ? 1 : 0
  
  name            = "${var.project_name}-${var.environment}-static-url-map-spa"
  default_service = google_compute_backend_bucket.static_backend.id

  # Host rule for all hosts
  host_rule {
    hosts        = ["*"]
    path_matcher = "all-paths"
  }

  # Path matcher with SPA-aware routing
  path_matcher {
    name            = "all-paths"
    default_service = google_compute_backend_bucket.static_backend.id

    # API routes (if enabled)
    dynamic "path_rule" {
      for_each = var.enable_api_routing ? [1] : []
      content {
        paths   = ["/api/*"]
        service = google_compute_backend_bucket.static_backend.id # Replace with actual API backend
      }
    }

    # Static assets that should be served directly
    path_rule {
      paths = [
        "*.js",
        "*.css",
        "*.png",
        "*.jpg",
        "*.jpeg",
        "*.gif",
        "*.svg",
        "*.ico",
        "*.woff",
        "*.woff2",
        "*.ttf",
        "*.eot",
        "/assets/*",
        "/static/*"
      ]
      service = google_compute_backend_bucket.static_backend.id
    }

    # SPA routes - serve index.html for all other paths under SPA base paths
    dynamic "path_rule" {
      for_each = local.spa_apps
      content {
        paths = [
          "${path_rule.value}",
          "${path_rule.value}/*"
        ]
        service = google_compute_backend_bucket.static_backend.id
        
        # Route action to rewrite the path to the SPA's index.html
        route_action {
          url_rewrite {
            path_prefix_rewrite = "${path_rule.value}/index.html"
          }
        }
      }
    }
  }
}

# Backend bucket with custom 404 handling for SPAs
resource "google_compute_backend_bucket" "static_backend_spa" {
  count = var.enable_spa_routing ? 1 : 0
  
  name        = "${var.project_name}-${var.environment}-backend-bucket-spa"
  bucket_name = google_storage_bucket.static_site.name
  enable_cdn  = true

  cdn_policy {
    cache_mode       = "CACHE_ALL_STATIC"
    default_ttl      = 300   # 5 minutes for HTML
    max_ttl          = 86400 # 24 hours max
    client_ttl       = 300
    negative_caching = true

    # Don't cache 404s for too long in case of deployment delays
    negative_caching_policy {
      code = 404
      ttl  = 10 # Only cache 404s for 10 seconds
    }

    negative_caching_policy {
      code = 410
      ttl  = 120
    }

    # Custom cache key policy to handle SPA routes
    cache_key_policy {
      include_host         = true
      include_protocol     = true
      include_query_string = false # Ignore query strings for caching
    }
  }
}