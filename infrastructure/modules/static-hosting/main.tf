# Static Hosting Module for DeusExMachina Frontend

locals {
  bucket_name = "${var.project_id}-static"
}

# Cloud Storage bucket for static files
resource "google_storage_bucket" "static_site" {
  name          = local.bucket_name
  location      = var.region
  force_destroy = var.force_destroy

  # Security: Use uniform bucket-level access
  uniform_bucket_level_access = true

  # Security: Enable encryption
  dynamic "encryption" {
    for_each = var.kms_key_name != null ? [1] : []
    content {
      default_kms_key_name = var.kms_key_name
    }
  }

  # Logging configuration
  dynamic "logging" {
    for_each = var.enable_logging ? [1] : []
    content {
      log_bucket        = var.log_bucket
      log_object_prefix = "storage-logs/${local.bucket_name}/"
    }
  }

  website {
    main_page_suffix = "index.html"
    # For SPAs, we use a special 404.html that contains the app
    not_found_page = var.enable_spa_routing ? "404.html" : "404.html"
  }

  cors {
    origin          = var.cors_origins
    method          = ["GET", "HEAD", "OPTIONS"]
    response_header = ["*"]
    max_age_seconds = 3600
  }

  versioning {
    enabled = true
  }

  lifecycle_rule {
    condition {
      num_newer_versions = 5
      with_state         = "ARCHIVED"
    }
    action {
      type = "Delete"
    }
  }

  labels = merge(
    var.labels,
    {
      component = "frontend"
      type      = "static-hosting"
    }
  )
}

# Make bucket publicly readable
resource "google_storage_bucket_iam_member" "public_read" {
  bucket = google_storage_bucket.static_site.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}

# Backend bucket for CDN
resource "google_compute_backend_bucket" "static_backend" {
  name        = "${var.project_name}-${var.environment}-backend-bucket"
  bucket_name = google_storage_bucket.static_site.name
  enable_cdn  = true

  cdn_policy {
    cache_mode       = "CACHE_ALL_STATIC"
    default_ttl      = 300   # 5 minutes for HTML
    max_ttl          = 86400 # 24 hours max
    client_ttl       = 300
    negative_caching = true

    negative_caching_policy {
      code = 404
      ttl  = 120
    }

    negative_caching_policy {
      code = 410
      ttl  = 120
    }
  }
}

# Reserved external IP
resource "google_compute_global_address" "static_ip" {
  name = "${var.project_name}-${var.environment}-static-ip"
}

# SSL Certificate (Google-managed)
resource "google_compute_managed_ssl_certificate" "static_cert" {
  count = var.domain_name != null ? 1 : 0

  name = "${var.project_name}-${var.environment}-static-cert"

  managed {
    domains = [var.domain_name]
  }
}

# Self-signed SSL Certificate for testing (when no domain is provided)
resource "google_compute_ssl_certificate" "static_self_signed" {
  count = var.domain_name == null ? 1 : 0

  name        = "${var.project_name}-${var.environment}-static-self-signed"
  private_key = tls_private_key.static_key[0].private_key_pem
  certificate = tls_self_signed_cert.static_cert[0].cert_pem

  lifecycle {
    create_before_destroy = true
  }
}

# TLS private key for self-signed cert
resource "tls_private_key" "static_key" {
  count     = var.domain_name == null ? 1 : 0
  algorithm = "RSA"
  rsa_bits  = 2048
}

# Self-signed certificate
resource "tls_self_signed_cert" "static_cert" {
  count           = var.domain_name == null ? 1 : 0
  private_key_pem = tls_private_key.static_key[0].private_key_pem

  subject {
    common_name  = "${var.project_name}.example.com"
    organization = "DeusExMachina"
  }

  validity_period_hours = 8760 # 1 year

  allowed_uses = [
    "key_encipherment",
    "digital_signature",
    "server_auth",
  ]
}

# Health check for backend
resource "google_compute_health_check" "static_health" {
  name               = "${var.project_name}-${var.environment}-static-health"
  check_interval_sec = 10
  timeout_sec        = 5

  http_health_check {
    port         = 443
    request_path = "/"
  }
}

# URL Map for routing
resource "google_compute_url_map" "static_url_map" {
  name            = "${var.project_name}-${var.environment}-static-url-map"
  default_service = google_compute_backend_bucket.static_backend.id

  # Route API calls to Cloud Functions
  dynamic "host_rule" {
    for_each = var.enable_api_routing ? [1] : []
    content {
      hosts        = ["*"]
      path_matcher = "api-routes"
    }
  }

  dynamic "path_matcher" {
    for_each = var.enable_api_routing ? [1] : []
    content {
      name            = "api-routes"
      default_service = google_compute_backend_bucket.static_backend.id

      # This would be configured to route to Cloud Functions
      # For now, using the same backend as placeholder
      path_rule {
        paths   = ["/api/*"]
        service = google_compute_backend_bucket.static_backend.id
      }
    }
  }
}

# HTTPS Proxy
resource "google_compute_target_https_proxy" "static_https_proxy" {
  name             = "${var.project_name}-${var.environment}-static-https-proxy"
  url_map          = google_compute_url_map.static_url_map.id
  ssl_certificates = var.domain_name != null ? google_compute_managed_ssl_certificate.static_cert[*].id : google_compute_ssl_certificate.static_self_signed[*].id
}

# HTTP Proxy (redirects to HTTPS)
resource "google_compute_target_http_proxy" "static_http_proxy" {
  name    = "${var.project_name}-${var.environment}-static-http-proxy"
  url_map = google_compute_url_map.static_url_map.id
}

# Global forwarding rule for HTTPS
resource "google_compute_global_forwarding_rule" "static_https" {
  name       = "${var.project_name}-${var.environment}-static-https"
  target     = google_compute_target_https_proxy.static_https_proxy.id
  port_range = "443"
  ip_address = google_compute_global_address.static_ip.address
}

# Global forwarding rule for HTTP (redirects to HTTPS)
resource "google_compute_global_forwarding_rule" "static_http" {
  name       = "${var.project_name}-${var.environment}-static-http"
  target     = google_compute_target_http_proxy.static_http_proxy.id
  port_range = "80"
  ip_address = google_compute_global_address.static_ip.address
}

# Output the index.html file for initial testing
resource "google_storage_bucket_object" "index_html" {
  count = var.deploy_test_index ? 1 : 0

  name   = "index.html"
  bucket = google_storage_bucket.static_site.name

  content = templatefile("${path.module}/templates/index.html.tpl", {
    project_name = var.project_name
    environment  = var.environment
    timestamp    = timestamp()
  })

  content_type = "text/html"

  cache_control = "public, max-age=300"
}
