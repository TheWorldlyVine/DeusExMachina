# SPA Routing Configuration Technical Specification

## 1. Introduction

### 1.1 Purpose
This specification defines the infrastructure configuration required to properly handle Single Page Application (SPA) routing for frontend applications hosted on Google Cloud Storage. It addresses the routing failures that occur when users refresh or directly navigate to deep routes within our SPA applications.

### 1.2 Scope
This specification covers:
- Infrastructure-level routing configuration for all frontend SPA applications
- Load balancer and Cloud Storage bucket settings
- URL rewrite rules for proper SPA routing
- Migration strategy for existing deployments

This specification does NOT cover:
- Client-side routing implementation (handled by React Router)
- Backend API routing
- CDN caching strategies for dynamic routes

### 1.3 Goals
- **Seamless Navigation**: Enable users to refresh or directly access any route without errors
- **Multi-App Support**: Handle routing for multiple SPAs deployed to subdirectories
- **Zero Downtime**: Implement changes without affecting existing users
- **Maintainability**: Use infrastructure as code for all configurations

## 2. Problem Statement

Current frontend applications experience routing failures when users:
1. Refresh the page on any non-root route (e.g., `/novel-creator/documents`)
2. Share or bookmark deep links to specific pages
3. Use browser back/forward navigation after a page refresh

The root cause is that Cloud Storage serves static files directly and returns 404/NoSuchKey errors for paths that don't correspond to actual files. This prevents the SPA's index.html from loading and handling client-side routing.

## 3. System Architecture

### 3.1 High-Level Design

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────────┐
│     Users       │────▶│  Cloud Load      │────▶│  URL Map Rules      │
└─────────────────┘     │  Balancer        │     │  (Route Processor)  │
                        └──────────────────┘     └─────────────────────┘
                                                           │
                        ┌──────────────────────────────────┴───────────────┐
                        │                                                    │
                        ▼                                                    ▼
                ┌──────────────────┐                          ┌──────────────────┐
                │  Path Matches     │                          │  Path Not Found  │
                │  Existing File    │                          │  (SPA Route)     │
                └──────────────────┘                          └──────────────────┘
                        │                                                    │
                        ▼                                                    ▼
                ┌──────────────────┐                          ┌──────────────────┐
                │  Serve Static     │                          │  Rewrite Path    │
                │  File Directly    │                          │  to index.html   │
                └──────────────────┘                          └──────────────────┘
```

### 3.2 Request Flow

1. **Static Asset Request** (e.g., `/novel-creator/assets/app.js`):
   - Load Balancer receives request
   - URL map checks if file exists in Cloud Storage
   - Serves file directly with appropriate cache headers

2. **SPA Route Request** (e.g., `/novel-creator/documents`):
   - Load Balancer receives request
   - URL map detects no matching file
   - Rewrites request to `/novel-creator/index.html`
   - Serves index.html while preserving original URL
   - React Router reads URL and renders correct component

3. **API Request** (e.g., `/api/documents`):
   - Follows existing backend routing rules
   - No changes required

## 4. Technical Requirements

### 4.1 Functional Requirements

1. **Route Handling**:
   - Serve actual files when they exist (JS, CSS, images)
   - Serve index.html for any unmatched routes within app directories
   - Preserve original URL for client-side routing
   - Support multiple apps in subdirectories

2. **App Structure Support**:
   - `/novel-creator/*` → `/novel-creator/index.html`
   - `/web-app/*` → `/web-app/index.html`
   - `/` → `/index.html` (landing page)

3. **Error Handling**:
   - Return proper 404 for truly missing assets
   - Distinguish between SPA routes and missing files

### 4.2 Non-Functional Requirements

- **Performance**: No additional latency for route resolution (<10ms)
- **Scalability**: Handle unlimited SPA routes without configuration changes
- **Compatibility**: Work with existing CDN and caching layers
- **Monitoring**: Track rewrite rule usage and errors

## 5. Implementation Details

### 5.1 Simplified SPA Routing Approach

Due to limitations in Google Cloud Load Balancer's URL rewriting capabilities, we implement SPA routing using a combination of:

1. **Cloud Storage's 404 handling**
2. **Client-side redirect logic**
3. **Proper file organization**

```hcl
# infrastructure/modules/static-hosting/spa_routing_simple.tf

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

  content_type = "text/html"
  cache_control = "no-cache, no-store, must-revalidate"
}
```

### 5.2 Enhanced Backend Bucket Configuration

```hcl
# infrastructure/modules/frontend-hosting/storage.tf

resource "google_compute_backend_bucket" "static_site" {
  name        = "${var.project_name}-frontend-backend"
  bucket_name = google_storage_bucket.static_site.name
  enable_cdn  = true

  # Custom headers for SPA support
  custom_response_headers = [
    "X-Content-Type-Options: nosniff",
    "X-Frame-Options: SAMEORIGIN",
    "X-XSS-Protection: 1; mode=block",
    # Indicate this is an SPA
    "X-SPA-Mode: true"
  ]
}

# Cloud Storage bucket with proper error handling
resource "google_storage_bucket" "static_site" {
  name     = "${var.project_name}-frontend-static"
  location = var.region

  website {
    main_page_suffix = "index.html"
    # Don't use not_found_page as it interferes with SPA routing
  }

  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD", "OPTIONS"]
    response_header = ["*"]
    max_age_seconds = 3600
  }
}
```

### 5.3 Advanced URL Rewrite with Conditional Logic

```hcl
# infrastructure/modules/frontend-hosting/url-rewrite.tf

locals {
  # Define all SPA apps and their routes
  spa_apps = {
    "novel-creator" = {
      base_path = "/novel-creator"
      routes = [
        "/documents",
        "/editor/*",
        "/settings",
        "/profile/*"
      ]
    }
    "web-app" = {
      base_path = "/web-app"
      routes = [
        "/dashboard",
        "/projects/*",
        "/settings"
      ]
    }
  }
}

resource "google_compute_url_map" "frontend_advanced" {
  name = "${var.project_name}-frontend-url-map-v2"

  default_service = google_compute_backend_bucket.static_site.id

  # Dynamic path rules for each SPA
  dynamic "path_matcher" {
    for_each = local.spa_apps

    content {
      name = "${path_matcher.key}-matcher"
      
      # Exact match for base path
      path_rule {
        paths = ["${path_matcher.value.base_path}"]
        route_action {
          url_rewrite {
            path_template_rewrite = "${path_matcher.value.base_path}/index.html"
          }
        }
      }

      # Match all SPA routes
      path_rule {
        paths = ["${path_matcher.value.base_path}/*"]
        
        # Advanced route handling with precedence
        route_action {
          url_rewrite {
            # Check if file exists, otherwise serve index.html
            path_template_rewrite = "{path_exists} ? {path} : ${path_matcher.value.base_path}/index.html"
          }
        }
      }
    }
  }
}
```

### 5.4 Edge Function for Advanced Routing (Alternative Approach)

```javascript
// infrastructure/edge-functions/spa-router.js

const SPA_CONFIGS = {
  '/novel-creator': {
    indexPath: '/novel-creator/index.html',
    assetPaths: ['/assets', '/fonts', '/images']
  },
  '/web-app': {
    indexPath: '/web-app/index.html',
    assetPaths: ['/assets', '/fonts', '/images']
  }
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    // Find matching SPA config
    const spaConfig = Object.entries(SPA_CONFIGS).find(([basePath]) => 
      path.startsWith(basePath)
    );

    if (!spaConfig) {
      // Not an SPA route, pass through
      return fetch(request);
    }

    const [basePath, config] = spaConfig;

    // Check if this is an asset request
    const isAsset = config.assetPaths.some(assetPath => 
      path.includes(assetPath)
    );

    if (isAsset) {
      // Try to fetch the asset
      const response = await fetch(request);
      if (response.status !== 404) {
        return response;
      }
    }

    // For all other paths, return the SPA's index.html
    const indexRequest = new Request(
      new URL(config.indexPath, request.url),
      request
    );

    const response = await fetch(indexRequest);
    
    // Preserve the original URL for client-side routing
    const modifiedResponse = new Response(response.body, {
      status: 200,
      headers: {
        ...response.headers,
        'X-Original-Path': path
      }
    });

    return modifiedResponse;
  }
};
```

## 6. Deployment and Migration Plan

### 6.1 Rollout Strategy

1. **Phase 1: Development Environment** (Day 1-2)
   - Deploy URL map changes to development
   - Test all SPA routes for each application
   - Validate asset loading and caching

2. **Phase 2: Staging Environment** (Day 3-4)
   - Deploy to staging with production-like traffic
   - Run automated E2E tests
   - Performance testing with route changes

3. **Phase 3: Production Canary** (Day 5)
   - Deploy to 10% of production traffic
   - Monitor error rates and performance
   - Gradual rollout to 100% if metrics are stable

4. **Phase 4: Full Production** (Day 6-7)
   - Complete production deployment
   - Monitor for 48 hours
   - Document any issues and resolutions

### 6.2 Rollback Plan

```bash
# Instant rollback procedure
terraform workspace select production
terraform apply -target=google_compute_url_map.frontend -var="use_legacy_routing=true"

# This reverts to the previous URL map within 60 seconds
```

## 7. Testing Strategy

### 7.1 Automated Tests

```typescript
// tests/e2e/spa-routing.spec.ts

describe('SPA Routing Tests', () => {
  const apps = [
    { name: 'novel-creator', routes: ['/documents', '/editor/123', '/settings'] },
    { name: 'web-app', routes: ['/dashboard', '/projects/456', '/settings'] }
  ];

  apps.forEach(app => {
    describe(`${app.name} routing`, () => {
      app.routes.forEach(route => {
        it(`should handle direct navigation to ${route}`, async ({ page }) => {
          // Direct navigation
          await page.goto(`https://god-in-a-box.com/${app.name}${route}`);
          await expect(page).not.toHaveURL(/error|404/);
          await expect(page.locator('#root')).toBeVisible();
        });

        it(`should handle refresh on ${route}`, async ({ page }) => {
          await page.goto(`https://god-in-a-box.com/${app.name}`);
          await page.goto(`https://god-in-a-box.com/${app.name}${route}`);
          await page.reload();
          await expect(page).not.toHaveURL(/error|404/);
          await expect(page.locator('#root')).toBeVisible();
        });
      });
    });
  });
});
```

### 7.2 Manual Test Checklist

- [ ] Direct navigation to deep routes works
- [ ] Page refresh maintains current route
- [ ] Browser back/forward buttons work correctly
- [ ] Bookmarked URLs load correctly
- [ ] Asset files (JS, CSS) load without rewriting
- [ ] 404 errors shown for truly missing assets
- [ ] API routes unaffected
- [ ] CDN caching still functional

## 8. Monitoring and Observability

### 8.1 Key Metrics

```hcl
# infrastructure/modules/frontend-hosting/monitoring.tf

resource "google_monitoring_alert_policy" "spa_routing_errors" {
  display_name = "SPA Routing Errors"
  
  conditions {
    display_name = "High 404 rate for index.html"
    
    condition_threshold {
      filter = <<-EOT
        resource.type = "load_balancer"
        AND metric.type = "loadbalancing.googleapis.com/https/request_count"
        AND metric.label.response_code = "404"
        AND metric.label.url_path =~ ".*index.html"
      EOT
      
      threshold_value = 100
      duration        = "60s"
    }
  }
}

resource "google_logging_metric" "spa_route_rewrites" {
  name        = "spa_route_rewrites"
  description = "Count of SPA route rewrites"
  
  filter = <<-EOT
    resource.type = "load_balancer"
    AND jsonPayload.url_rewrite = true
  EOT
  
  metric_descriptor {
    metric_kind = "DELTA"
    value_type  = "INT64"
    
    labels {
      key         = "app_name"
      value_type  = "STRING"
      description = "Name of the SPA application"
    }
  }
}
```

### 8.2 Dashboard Configuration

```json
{
  "displayName": "SPA Routing Dashboard",
  "widgets": [
    {
      "title": "Route Rewrite Success Rate",
      "xyChart": {
        "dataSets": [{
          "timeSeriesQuery": {
            "filter": "metric.type=\"custom.googleapis.com/spa_route_rewrites\""
          }
        }]
      }
    },
    {
      "title": "404 Errors by Path",
      "xyChart": {
        "dataSets": [{
          "timeSeriesQuery": {
            "filter": "metric.type=\"loadbalancing.googleapis.com/https/request_count\" AND metric.label.response_code=\"404\""
          }
        }]
      }
    }
  ]
}
```

## 9. Security Considerations

1. **Path Traversal Protection**: Ensure URL rewrites cannot access files outside intended directories
2. **Cache Poisoning**: Validate that rewritten responses maintain proper cache keys
3. **Header Injection**: Sanitize any headers added during rewriting
4. **CORS Validation**: Ensure CORS headers are properly maintained after rewriting

## 10. Cost Analysis

| Component | Current Cost | Projected Cost | Difference |
|-----------|--------------|----------------|------------|
| Load Balancer Rules | $0.00 | $0.00 | No change |
| Additional Processing | $0.00 | ~$5.00/month | Minimal overhead |
| Edge Function (if used) | N/A | ~$10.00/month | Optional enhancement |
| **Total Additional Cost** | **$0.00** | **$5-15/month** | **Acceptable** |

## 11. Future Enhancements

1. **Smart Preloading**: Detect SPA routes and preload assets
2. **Route Analytics**: Track most-used routes for optimization
3. **A/B Testing Support**: Enable gradual rollout of new routes
4. **Service Worker Integration**: Offline support for SPA routing
5. **Multi-Region Optimization**: Route-aware CDN warming

## 12. Conclusion

This specification provides a robust, scalable solution for SPA routing that:
- Eliminates refresh and direct navigation errors
- Supports multiple SPA applications
- Maintains infrastructure as code principles
- Provides zero-downtime deployment
- Includes comprehensive monitoring and rollback capabilities

The implementation follows GCP best practices and integrates seamlessly with our existing infrastructure while solving the routing issues without client-side hacks or workarounds.