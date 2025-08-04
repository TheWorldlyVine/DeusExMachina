# SPA Routing Migration Plan

This document outlines the steps to enable SPA routing in production for the DeusExMachina frontend applications.

## Overview

The migration adds URL rewrite rules to the Google Cloud Load Balancer to properly handle Single Page Application (SPA) routing. This ensures that refreshing the page or directly accessing deep routes (e.g., `/novel-creator/editor/123`) works correctly.

## Pre-Migration Checklist

- [ ] Backup current Terraform state
- [ ] Verify all frontend apps have proper base path configuration
- [ ] Ensure CI/CD pipeline is paused during migration
- [ ] Have rollback plan ready

## Migration Steps

### 1. Backup Current State

```bash
cd infrastructure/environments/prod
terraform state pull > terraform.tfstate.backup.$(date +%Y%m%d-%H%M%S)
```

### 2. Plan Changes

```bash
terraform plan -out=spa-routing.tfplan
```

Review the plan carefully. Expected changes:
- Creation of new URL map `deus-ex-machina-prod-spa-url-map`
- Update to HTTPS/HTTP proxy to use new URL map
- No changes to existing bucket or CDN configuration

### 3. Apply Changes

```bash
terraform apply spa-routing.tfplan
```

### 4. Verify Routing

Test each application's routing:

```bash
# Novel Creator
curl -I https://god-in-a-box.com/novel-creator/
curl -I https://god-in-a-box.com/novel-creator/documents
curl -I https://god-in-a-box.com/novel-creator/editor/test-id

# Web App (if deployed)
curl -I https://god-in-a-box.com/web-app/
curl -I https://god-in-a-box.com/web-app/dashboard
```

### 5. Test in Browser

1. Navigate to https://god-in-a-box.com/novel-creator/documents
2. Refresh the page - should not show 404
3. Check browser developer tools for proper index.html loading
4. Verify React Router handles the route correctly

## Rollback Plan

If issues occur, rollback immediately:

```bash
# Disable SPA routing in main.tf
# Change: enable_spa_routing = false

terraform plan -out=rollback.tfplan
terraform apply rollback.tfplan
```

## Post-Migration

1. Update monitoring to track:
   - 404 error rates (should decrease)
   - URL rewrite performance
   - Cache hit rates

2. Notify development team:
   - SPA routing is now active
   - Deep linking is supported
   - No changes needed to frontend code

## Troubleshooting

### Issue: Still getting 404 errors
- Check if the path matches a static file pattern
- Verify the app's base_path is correctly configured
- Check Cloud CDN cache - may need invalidation

### Issue: Wrong app's index.html served
- Verify spa_apps configuration in Terraform
- Check URL map rules in GCP Console
- Ensure no overlapping base paths

### Issue: Slow response times
- Check if URL rewrites are adding latency
- Verify CDN is caching index.html appropriately
- Monitor load balancer metrics

## Monitoring Commands

```bash
# View current URL map configuration
gcloud compute url-maps describe deus-ex-machina-prod-spa-url-map

# Monitor 404 errors
gcloud logging read 'resource.type="http_load_balancer" AND httpRequest.status=404' --limit=50

# Check rewrite rules
gcloud compute url-maps list-path-matchers deus-ex-machina-prod-spa-url-map
```

## Success Criteria

- [ ] All SPA routes return 200 status
- [ ] Page refresh maintains current route
- [ ] No increase in error rates
- [ ] Performance metrics remain stable
- [ ] All existing functionality continues to work