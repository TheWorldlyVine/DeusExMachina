#!/bin/bash
# Script to import existing static hosting resources into Terraform state

set -e

echo "Importing existing static hosting resources into Terraform..."
echo "Make sure you've run 'terraform init' first!"
echo ""

# Import storage bucket
echo "Importing storage bucket..."
terraform import module.static_hosting.google_storage_bucket.static_site deus-ex-machina-prod-static || echo "Already imported or doesn't exist"

# Import static IP
echo "Importing static IP..."
terraform import module.static_hosting.google_compute_global_address.static_ip deus-ex-machina-prod-static-ip || echo "Already imported or doesn't exist"

# Import backend bucket
echo "Importing backend bucket..."
terraform import module.static_hosting.google_compute_backend_bucket.static_backend deus-ex-machina-prod-backend-bucket || echo "Already imported or doesn't exist"

# Import URL map
echo "Importing URL map..."
terraform import module.static_hosting.google_compute_url_map.static_url_map deus-ex-machina-prod-static-url-map || echo "Already imported or doesn't exist"

# Import HTTPS proxy
echo "Importing HTTPS proxy..."
terraform import module.static_hosting.google_compute_target_https_proxy.static_https_proxy deus-ex-machina-prod-static-https-proxy || echo "Already imported or doesn't exist"

# Import HTTP proxy
echo "Importing HTTP proxy..."
terraform import module.static_hosting.google_compute_target_http_proxy.static_http_proxy deus-ex-machina-prod-static-http-proxy || echo "Already imported or doesn't exist"

# Import HTTPS forwarding rule
echo "Importing HTTPS forwarding rule..."
terraform import module.static_hosting.google_compute_global_forwarding_rule.static_https deus-ex-machina-prod-static-https || echo "Already imported or doesn't exist"

# Import HTTP forwarding rule
echo "Importing HTTP forwarding rule..."
terraform import module.static_hosting.google_compute_global_forwarding_rule.static_http deus-ex-machina-prod-static-http || echo "Already imported or doesn't exist"

# Import SSL certificate
echo "Importing SSL certificate..."
terraform import 'module.static_hosting.google_compute_managed_ssl_certificate.static_cert[0]' god-in-a-box-ssl-cert || echo "Already imported or doesn't exist"

echo ""
echo "Import complete! Now you can run:"
echo "  terraform plan"
echo "  terraform apply"
echo ""
echo "This will ensure Terraform manages these resources going forward."