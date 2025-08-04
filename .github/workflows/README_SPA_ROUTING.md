# CI/CD Pipeline - SPA Routing Support

This document describes how the CI/CD pipeline handles SPA routing for frontend applications.

## Overview

The existing CI/CD pipeline already includes support for SPA routing through several mechanisms:

1. **Subdirectory Deployment**: Each frontend app is deployed to its own subdirectory (e.g., `/novel-creator/`)
2. **Fallback Routes**: Common SPA routes get index.html copies for direct access
3. **404 Handling**: Creates 404.html as a fallback for client-side routing
4. **Cache Configuration**: HTML files have short TTL for updates

## Current Implementation

### Frontend Deployment (Lines 426-519)

The pipeline deploys frontend apps with SPA support:

```yaml
# Deploy app to its own subdirectory
gsutil -m rsync -r -d "${app_dir}dist/" "gs://${BUCKET_NAME}/${app_name}/"

# Copy index.html to 404.html for general fallback
gsutil cp "gs://${BUCKET_NAME}/${app_name}/index.html" "gs://${BUCKET_NAME}/${app_name}/404.html"

# Create common route directories with index.html
for route in "login" "documents" "editor" "settings" "signup" "dashboard"; do
  gsutil cp "gs://${BUCKET_NAME}/${app_name}/index.html" "gs://${BUCKET_NAME}/${app_name}/${route}/index.html"
done
```

### Cache Headers (Lines 511-517)

Appropriate cache headers are set:
- **Static assets** (JS/CSS): 1 year cache
- **Images**: 30 days cache
- **HTML files**: 5 minutes cache
- **index.html/404.html**: No cache

## Infrastructure Integration

With the new SPA routing infrastructure:

1. **URL Map Configuration**: The infrastructure now handles URL rewrites at the load balancer level
2. **Fallback Strategy**: The CI/CD fallbacks provide additional safety for common routes
3. **Performance**: Load balancer rewrites are faster than bucket-level fallbacks

## CDN Cache Invalidation

The pipeline invalidates CDN cache after deployment:

```yaml
gcloud compute url-maps invalidate-cdn-cache ${URL_MAP_NAME} --path "/*"
```

**Note**: When SPA routing is enabled, ensure the URL_MAP_NAME variable reflects the correct URL map (spa-url-map vs static-url-map).

## Recommendations

### 1. Update URL Map Name Logic

Consider updating the CDN invalidation to use the correct URL map based on infrastructure:

```yaml
- name: Get URL Map Name
  if: github.ref == 'refs/heads/main'
  run: |
    cd infrastructure/environments/prod
    URL_MAP_NAME=$(terraform output -raw static_hosting_url_map_name)
    echo "URL_MAP_NAME=${URL_MAP_NAME}" >> $GITHUB_ENV
```

### 2. Remove Redundant Route Creation

With proper URL rewriting at the load balancer level, the manual route creation (lines 503-509) becomes optional. However, keeping it provides:
- Backwards compatibility
- Fallback if URL rewriting fails
- Faster response for common routes

### 3. Add E2E Test Step

Add a step to run SPA routing tests after deployment:

```yaml
- name: Test SPA Routing
  if: github.ref == 'refs/heads/main'
  run: |
    # Wait for CDN propagation
    sleep 30
    
    # Run the quick routing test
    cd apps/frontend/novel-creator
    ./test-spa-routing.sh
```

## Environment Variables

The pipeline already sets the correct API URLs for production:

```yaml
export VITE_API_URL=https://auth-function-tbmcifixdq-uc.a.run.app
export VITE_GRAPHQL_URL=https://novel-graphql-gateway-97677897945.us-central1.run.app/graphql
# ... other API URLs
```

## Summary

The current CI/CD pipeline is well-equipped to handle SPA deployments. The infrastructure-level URL rewriting enhances the existing bucket-level fallbacks, providing a robust solution for SPA routing.

No major changes are required to the pipeline, though the recommended updates would improve integration with the new infrastructure configuration.