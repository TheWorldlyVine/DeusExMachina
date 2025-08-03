# Static Hosting Configuration

module "static_hosting" {
  source = "../../modules/static-hosting"

  project_id   = local.project_id
  project_name = local.project_name
  environment  = local.environment
  region       = local.region

  # Domain configuration
  domain_name = "god-in-a-box.com"

  # CORS configuration
  cors_origins = ["https://god-in-a-box.com", "https://www.god-in-a-box.com"]

  # Enable CDN and security features
  enable_cdn          = true
  enable_cloud_armor  = false # Enable this later if you need DDoS protection
  enable_logging      = false # Enable if you have a logging bucket

  # Cache policies
  cache_policies = {
    static_ttl = 31536000 # 1 year for JS/CSS
    html_ttl   = 300      # 5 minutes for HTML
  }

  # Security headers
  security_headers = {
    enable_hsts                 = true
    enable_content_type_options = true
    enable_xss_protection       = true
    enable_frame_options        = true
    csp_policy                  = null # Add Content Security Policy if needed
  }

  # Labels
  labels = local.common_labels
}

# Import existing resources
# Run these commands after applying:
# terraform import module.static_hosting.google_storage_bucket.static_site deus-ex-machina-prod-static
# terraform import module.static_hosting.google_compute_global_address.static_ip deus-ex-machina-prod-static-ip
# terraform import module.static_hosting.google_compute_backend_bucket.static_backend deus-ex-machina-prod-backend-bucket
# terraform import module.static_hosting.google_compute_url_map.static_url_map deus-ex-machina-prod-static-url-map
# terraform import module.static_hosting.google_compute_target_https_proxy.static_https_proxy deus-ex-machina-prod-static-https-proxy
# terraform import module.static_hosting.google_compute_target_http_proxy.static_http_proxy deus-ex-machina-prod-static-http-proxy
# terraform import module.static_hosting.google_compute_global_forwarding_rule.static_https deus-ex-machina-prod-static-https
# terraform import module.static_hosting.google_compute_global_forwarding_rule.static_http deus-ex-machina-prod-static-http
# terraform import module.static_hosting.google_compute_managed_ssl_certificate.static_cert[0] god-in-a-box-ssl-cert