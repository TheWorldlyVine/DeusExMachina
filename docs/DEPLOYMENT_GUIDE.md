# DeusExMachina Deployment Guide

## Quick Deploy Test Index.html

### Prerequisites
1. GCP project created and configured
2. Terraform infrastructure deployed
3. `gcloud` CLI authenticated

### Manual Deployment Steps

1. **Get the bucket name from Terraform outputs:**
   ```bash
   cd infrastructure/environments/prod
   BUCKET_NAME=$(terraform output -raw static_hosting_bucket)
   echo "Bucket: $BUCKET_NAME"
   ```

2. **Deploy the test index.html:**
   ```bash
   # Navigate to the frontend directory
   cd ../../../apps/frontend/web-app
   
   # Deploy the test file
   gsutil cp dist/index.html gs://$BUCKET_NAME/
   
   # Set proper cache headers
   gsutil setmeta -h "Cache-Control:no-cache" gs://$BUCKET_NAME/index.html
   ```

3. **Get the access URL:**
   ```bash
   cd ../../../infrastructure/environments/prod
   echo "Access your site at: $(terraform output -raw static_hosting_url)"
   ```

4. **Verify deployment:**
   - Open the URL in your browser
   - Check that all health checks pass
   - Verify HTTPS is working

### Full Frontend Deployment

1. **Build the React app:**
   ```bash
   cd apps/frontend/web-app
   pnpm install
   pnpm run build
   ```

2. **Deploy all files:**
   ```bash
   # Get bucket name
   BUCKET_NAME=$(cd infrastructure/environments/prod && terraform output -raw static_hosting_bucket)
   
   # Sync all files
   gsutil -m rsync -r -d dist/ gs://$BUCKET_NAME/
   
   # Set cache headers
   gsutil -m setmeta -h "Cache-Control:public, max-age=31536000" gs://$BUCKET_NAME/**/*.js
   gsutil -m setmeta -h "Cache-Control:public, max-age=31536000" gs://$BUCKET_NAME/**/*.css
   gsutil -m setmeta -h "Cache-Control:public, max-age=300" gs://$BUCKET_NAME/**/*.html
   gsutil -m setmeta -h "Cache-Control:no-cache" gs://$BUCKET_NAME/index.html
   ```

3. **Invalidate CDN cache (if needed):**
   ```bash
   URL_MAP_NAME="deus-ex-machina-prod-static-url-map"
   gcloud compute url-maps invalidate-cdn-cache $URL_MAP_NAME --path "/*"
   ```

## Automated Deployment via GitHub Actions

Once configured, deployments happen automatically:

1. **On push to main branch:**
   - Frontend is built
   - Files are deployed to Cloud Storage
   - CDN cache is invalidated

2. **Required GitHub Secrets:**
   - `GCP_SA_KEY`: Service account JSON key
   - `STATIC_HOSTING_BUCKET`: Your bucket name
   - `STATIC_URL_MAP_NAME`: Your URL map name

## Troubleshooting Deployment

### Files not updating
1. Check CDN cache headers: `curl -I <your-url>/file.js`
2. Force cache invalidation: `gcloud compute url-maps invalidate-cdn-cache <url-map> --path "/*"`
3. Wait 5-10 minutes for global propagation

### Permission errors
1. Check bucket IAM: `gsutil iam get gs://<bucket-name>`
2. Ensure `allUsers` has `storage.objectViewer` role
3. Verify service account has `storage.admin` role

### SSL certificate issues
1. Check certificate status in GCP Console
2. Verify DNS is pointing to the correct IP
3. Wait up to 24 hours for initial provisioning

### Load balancer unhealthy
1. Check backend bucket configuration
2. Verify at least one file exists in the bucket
3. Check health check configuration

## Monitoring Deployment

### View deployment logs
```bash
# GitHub Actions logs
# Go to GitHub → Actions → Select workflow run

# GCP Load Balancer logs
gcloud logging read "resource.type=http_load_balancer" --limit 50

# CDN cache logs
gcloud logging read "resource.type=cloud_cdn_bucket" --limit 50
```

### Check metrics
- **Uptime**: GCP Console → Monitoring → Uptime Checks
- **Performance**: GCP Console → Network Services → Load Balancing → Your LB → Monitoring
- **CDN Hit Rate**: GCP Console → Network Services → Cloud CDN → Your Backend → Monitoring

## Cost Optimization

1. **Monitor bandwidth usage:**
   ```bash
   # Check current month's CDN usage
   gcloud compute project-info describe --format="value(quotas[name=CPUS_ALL_REGIONS].usage)"
   ```

2. **Optimize assets:**
   - Use WebP/AVIF for images
   - Enable gzip/brotli compression
   - Implement proper cache headers
   - Use CDN for all static assets

3. **Set up budget alerts:**
   - Go to GCP Console → Billing → Budgets & Alerts
   - Create alert for static hosting resources