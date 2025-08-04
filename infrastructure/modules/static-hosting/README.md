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

### With SPA Routing (Single Page Applications)

```hcl
module "static_hosting" {
  source = "../../modules/static-hosting"
  
  project_id   = "my-project-id"
  project_name = "deus-ex-machina"
  environment  = "prod"
  
  # Enable SPA routing for React/Vue/Angular apps
  enable_spa_routing = true
  
  # Configure SPA applications
  spa_apps = {
    "novel-creator" = {
      base_path = "/novel-creator"
      routes    = ["/documents", "/editor/*", "/settings"]
    }
    "web-app" = {
      base_path = "/web-app"
      routes    = ["/dashboard", "/projects/*"]
    }
  }
  
  # Optional: domain configuration
  domain_name = "god-in-a-box.com"
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
| enable_spa_routing | Enable SPA routing support | `bool` | `false` | no |
| spa_apps | SPA app configurations | `map(object)` | `{}` | no |

## Outputs

| Name | Description |
|------|-------------|
| bucket_name | Name of the Cloud Storage bucket |
| static_ip_address | Static IP address of the load balancer |
| load_balancer_url | HTTPS URL of the load balancer |
| domain_url | URL with custom domain (if configured) |
| url_map_name | Name of the URL map resource |
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

## SPA Routing Configuration

When `enable_spa_routing` is set to `true`, the module configures the load balancer to properly handle Single Page Application routing:

### How It Works

1. **Static Files**: Files with extensions (`.js`, `.css`, `.png`, etc.) are served directly
2. **API Routes**: `/api/*` paths bypass SPA routing and are forwarded as configured
3. **SPA Routes**: All other paths within an app's base path serve the app's `index.html`
4. **URL Preservation**: The original URL path is preserved for client-side routing

### Example Routing Behavior

For an app configured with `base_path = "/novel-creator"`:
- `/novel-creator` → serves `/novel-creator/index.html`
- `/novel-creator/documents` → serves `/novel-creator/index.html`
- `/novel-creator/editor/123` → serves `/novel-creator/index.html`
- `/novel-creator/assets/app.js` → serves the actual JS file
- `/novel-creator/missing.js` → returns 404

### Directory Structure

```
bucket/
├── index.html                    # Landing page
├── novel-creator/
│   ├── index.html               # Novel Creator app
│   └── assets/
│       ├── app.js
│       └── style.css
└── web-app/
    ├── index.html               # Web app
    └── assets/
        └── bundle.js
```

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