# Cloud CDN Configuration

# Custom cache key policy for better cache performance
resource "google_compute_backend_bucket" "static_backend_custom" {
  count = var.enable_advanced_cdn ? 1 : 0

  name        = "${var.project_name}-${var.environment}-backend-bucket-custom"
  bucket_name = google_storage_bucket.static_site.name
  enable_cdn  = true

  cdn_policy {
    cache_mode = "USE_ORIGIN_HEADERS"

    # Cache based on query strings for cache busting
    cache_key_policy {
      include_http_headers   = []
      query_string_whitelist = ["v", "version"]
    }

    # Different TTLs for different content types
    default_ttl = var.cache_policies.html_ttl
    max_ttl     = var.cache_policies.static_ttl
    client_ttl  = var.cache_policies.html_ttl

    # Enable negative caching
    negative_caching = true

    negative_caching_policy {
      code = 404
      ttl  = 120
    }

    negative_caching_policy {
      code = 410
      ttl  = 120
    }

    # Serve stale content while revalidating
    serve_while_stale = 86400
  }

  # Custom headers for security
  custom_response_headers = concat(
    var.security_headers.enable_hsts ? ["Strict-Transport-Security: max-age=31536000; includeSubDomains"] : [],
    var.security_headers.enable_content_type_options ? ["X-Content-Type-Options: nosniff"] : [],
    var.security_headers.enable_xss_protection ? ["X-XSS-Protection: 1; mode=block"] : [],
    var.security_headers.enable_frame_options ? ["X-Frame-Options: SAMEORIGIN"] : [],
    var.security_headers.csp_policy != null ? ["Content-Security-Policy: ${var.security_headers.csp_policy}"] : []
  )
}

# Edge security policies using Cloud Armor
resource "google_compute_security_policy" "static_security" {
  count = var.enable_cloud_armor ? 1 : 0

  name = "${var.project_name}-${var.environment}-static-security"

  # Default rule
  rule {
    action   = "allow"
    priority = "2147483647"
    match {
      versioned_expr = "SRC_IPS_V1"
      config {
        src_ip_ranges = ["*"]
      }
    }
    description = "Default allow rule"
  }

  # Rate limiting rule
  rule {
    action   = "rate_based_ban"
    priority = "1000"
    match {
      versioned_expr = "SRC_IPS_V1"
      config {
        src_ip_ranges = ["*"]
      }
    }
    rate_limit_options {
      conform_action = "allow"
      exceed_action  = "deny(429)"
      enforce_on_key = "IP"

      rate_limit_threshold {
        count        = 100
        interval_sec = 60
      }

      ban_duration_sec = 600 # Ban for 10 minutes
    }
    description = "Rate limiting rule"
  }

  # Block sensitive files - Part 1
  rule {
    action   = "deny(403)"
    priority = "900"
    match {
      expr {
        expression = <<-EOT
          request.path.matches('\\.git$') || request.path.matches('\\.svn$') || request.path.matches('\\.env$') || request.path.matches('\\.config$') || request.path.matches('\\.bak$')
        EOT
      }
    }
    description = "Block access to sensitive files (git, svn, env, config, bak)"
  }

  # Block sensitive files - Part 2
  rule {
    action   = "deny(403)"
    priority = "901"
    match {
      expr {
        expression = <<-EOT
          request.path.matches('\\.backup$') || request.path.matches('\\.sql$') || request.path.matches('\\.db$') || request.path.matches('\\.log$')
        EOT
      }
    }
    description = "Block access to sensitive files (backup, sql, db, log)"
  }

  # Geo-blocking (example - adjust as needed)
  dynamic "rule" {
    for_each = length(var.blocked_countries) > 0 ? [1] : []
    content {
      action   = "deny(403)"
      priority = "800"
      match {
        expr {
          expression = "origin.region_code in ${jsonencode(var.blocked_countries)}"
        }
      }
      description = "Geo-blocking rule"
    }
  }
}

# Additional cache invalidation cloud function (optional)
resource "google_cloudfunctions2_function" "cache_invalidator" {
  count = var.enable_cache_invalidator ? 1 : 0

  name        = "${var.project_name}-${var.environment}-cache-invalidator"
  location    = var.region
  description = "Function to invalidate CDN cache on deployment"

  build_config {
    runtime     = "nodejs20"
    entry_point = "invalidateCache"
    source {
      storage_source {
        bucket = google_storage_bucket.static_site.name
        object = google_storage_bucket_object.cache_invalidator_source[0].name
      }
    }
  }

  service_config {
    max_instance_count = 1
    timeout_seconds    = 60

    environment_variables = {
      URL_MAP_NAME = google_compute_url_map.static_url_map.name
      PROJECT_ID   = var.project_id
    }

    service_account_email = google_service_account.cache_invalidator_sa[0].email
  }
}

# Service account for cache invalidator
resource "google_service_account" "cache_invalidator_sa" {
  count = var.enable_cache_invalidator ? 1 : 0

  account_id   = "${var.project_name}-${var.environment}-cache-inv"
  display_name = "Cache Invalidator Service Account"
  description  = "Service account for CDN cache invalidation"
}

# IAM permissions for cache invalidator
resource "google_project_iam_member" "cache_invalidator_permissions" {
  count = var.enable_cache_invalidator ? 1 : 0

  project = var.project_id
  role    = "roles/compute.urlMapAdmin"
  member  = "serviceAccount:${google_service_account.cache_invalidator_sa[0].email}"
}

# Source code for cache invalidator function
resource "google_storage_bucket_object" "cache_invalidator_source" {
  count = var.enable_cache_invalidator ? 1 : 0

  name   = "cache-invalidator-${data.archive_file.cache_invalidator_zip[0].output_md5}.zip"
  bucket = google_storage_bucket.static_site.name
  source = data.archive_file.cache_invalidator_zip[0].output_path
}

data "archive_file" "cache_invalidator_zip" {
  count = var.enable_cache_invalidator ? 1 : 0

  type        = "zip"
  output_path = "${path.module}/cache-invalidator.zip"

  source {
    content  = file("${path.module}/functions/cache-invalidator.js")
    filename = "index.js"
  }

  source {
    content  = file("${path.module}/functions/package.json")
    filename = "package.json"
  }
}
