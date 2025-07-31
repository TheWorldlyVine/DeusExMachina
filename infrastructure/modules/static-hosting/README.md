# Static Hosting Module

This Terraform module creates a complete static website hosting infrastructure on Google Cloud Platform, including Cloud Storage, Cloud CDN, Load Balancer, and optional security features.

## Features

- **Cloud Storage** bucket for static file hosting
- **Cloud CDN** for global content distribution
- **HTTPS Load Balancer** with managed SSL certificates
- **Cloud Armor** security policies (optional)
- **Custom domain** support
- **Automatic cache invalidation** (optional)
- **Security headers** implementation
- **CORS** configuration

## Usage

### Basic Usage

```hcl
module "static_hosting" {
  source = "../../modules/static-hosting"
  
  project_id   = "my-project-id"
  project_name = "deus-ex-machina"
  environment  = "prod"
  region       = "us-central1"
  
  # Deploy a test index.html
  deploy_test_index = true
  
  labels = {
    team = "frontend"
    cost-center = "engineering"
  }
}
```

### Advanced Usage with Custom Domain

```hcl
module "static_hosting" {
  source = "../../modules/static-hosting"
  
  project_id   = "my-project-id"
  project_name = "deus-ex-machina"
  environment  = "prod"
  region       = "us-central1"
  
  # Custom domain
  domain_name = "app.example.com"
  
  # Enable advanced features
  enable_advanced_cdn = true
  enable_cloud_armor  = true
  
  # CORS configuration
  cors_origins = ["https://api.example.com"]
  
  # Cache policies
  cache_policies = {
    html_ttl   = 300      # 5 minutes
    static_ttl = 31536000 # 1 year
    image_ttl  = 2592000  # 30 days
  }
  
  # Security headers
  security_headers = {
    enable_hsts = true
    csp_policy  = "default-src 'self'; script-src 'self' 'unsafe-inline';"
  }
  
  # Geo-blocking
  blocked_countries = ["CN", "RU", "KP"]
}
```

### With API Routing

```hcl
module "static_hosting" {
  source = "../../modules/static-hosting"
  
  project_id   = "my-project-id"
  project_name = "deus-ex-machina"
  environment  = "prod"
  
  # Enable API routing to Cloud Functions
  enable_api_routing = true
  cloud_functions_backend_url = "https://api-function-xyz.cloudfunctions.net"
}
```

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| project_id | GCP project ID | `string` | n/a | yes |
| project_name | Name of the project | `string` | n/a | yes |
| environment | Environment (dev, staging, prod) | `string` | n/a | yes |
| region | GCP region for the bucket | `string` | `"us-central1"` | no |
| domain_name | Custom domain name | `string` | `null` | no |
| cors_origins | List of CORS origins | `list(string)` | `["*"]` | no |
| enable_api_routing | Enable /api/* routing | `bool` | `false` | no |
| deploy_test_index | Deploy test index.html | `bool` | `false` | no |
| enable_advanced_cdn | Enable advanced CDN config | `bool` | `false` | no |
| enable_cloud_armor | Enable Cloud Armor | `bool` | `false` | no |
| blocked_countries | Countries to block | `list(string)` | `[]` | no |

## Outputs

| Name | Description |
|------|-------------|
| bucket_name | Name of the Cloud Storage bucket |
| static_ip_address | Static IP address of the load balancer |
| load_balancer_url | HTTPS URL of the load balancer |
| domain_url | URL with custom domain (if configured) |
| deployment_instructions | Instructions for deploying files |

## Deployment Instructions

### 1. Deploy Static Files

```bash
# Build your frontend
pnpm run build

# Deploy to bucket
gsutil -m rsync -r -d dist/ gs://BUCKET_NAME/

# Or use gcloud
gcloud storage rsync dist/ gs://BUCKET_NAME/ --recursive --delete-unmatched-destination-objects
```

### 2. Invalidate CDN Cache

```bash
# Invalidate all paths
gcloud compute url-maps invalidate-cdn-cache URL_MAP_NAME --path "/*"

# Invalidate specific path
gcloud compute url-maps invalidate-cdn-cache URL_MAP_NAME --path "/index.html"
```

### 3. Set Up Custom Domain

1. Add the static IP to your DNS as an A record
2. Wait for DNS propagation (can take up to 24 hours)
3. The SSL certificate will be automatically provisioned

## Security Considerations

- Enable Cloud Armor for DDoS protection
- Configure appropriate CORS policies
- Use security headers for XSS protection
- Implement CSP (Content Security Policy)
- Enable HTTPS-only access

## Cost Optimization

- Use appropriate cache TTLs to minimize origin requests
- Enable gzip compression
- Use WebP/AVIF for images
- Implement lazy loading for assets
- Monitor bandwidth usage

## Monitoring

Set up the following alerts:
- Uptime checks on the load balancer
- 4xx/5xx error rate monitoring
- Bandwidth usage alerts
- CDN cache hit ratio monitoring