# Edge Function for SPA Routing
# This Cloud Function handles SPA routing at the edge

resource "google_storage_bucket_object" "spa_router_code" {
  count = var.enable_spa_routing ? 1 : 0

  name   = "cloud-functions/spa-router/index.js"
  bucket = google_storage_bucket.static_site.name

  content = <<-EOT
const functions = require('@google-cloud/functions-framework');
const { Storage } = require('@google-cloud/storage');

const storage = new Storage();
const BUCKET_NAME = '${google_storage_bucket.static_site.name}';

functions.http('spaRouter', async (req, res) => {
  const path = req.path || '/';
  
  try {
    // Try to get the exact file
    const bucket = storage.bucket(BUCKET_NAME);
    const file = bucket.file(path.substring(1)); // Remove leading slash
    
    const [exists] = await file.exists();
    
    if (exists) {
      // File exists, serve it
      const stream = file.createReadStream();
      const [metadata] = await file.getMetadata();
      
      res.set('Content-Type', metadata.contentType || 'application/octet-stream');
      res.set('Cache-Control', metadata.cacheControl || 'public, max-age=3600');
      
      stream.pipe(res);
    } else {
      // File doesn't exist, check if it's a SPA route
      const appMatch = path.match(/^\/(novel-creator|web-app)(\/.*)?$/);
      
      if (appMatch) {
        const appName = appMatch[1];
        const indexFile = bucket.file(`$${appName}/index.html`);
        
        const [indexExists] = await indexFile.exists();
        
        if (indexExists) {
          // Serve the app's index.html
          const stream = indexFile.createReadStream();
          
          res.set('Content-Type', 'text/html');
          res.set('Cache-Control', 'no-cache, no-store, must-revalidate');
          res.set('X-Original-Path', path);
          
          stream.pipe(res);
        } else {
          res.status(404).send('Not Found');
        }
      } else {
        // Try to serve root index.html
        const indexFile = bucket.file('index.html');
        const [indexExists] = await indexFile.exists();
        
        if (indexExists) {
          const stream = indexFile.createReadStream();
          res.set('Content-Type', 'text/html');
          stream.pipe(res);
        } else {
          res.status(404).send('Not Found');
        }
      }
    }
  } catch (error) {
    console.error('Error:', error);
    res.status(500).send('Internal Server Error');
  }
});
EOT

  content_type = "application/javascript"
}

resource "google_storage_bucket_object" "spa_router_package" {
  count = var.enable_spa_routing ? 1 : 0

  name   = "cloud-functions/spa-router/package.json"
  bucket = google_storage_bucket.static_site.name

  content = jsonencode({
    name    = "spa-router"
    version = "1.0.0"
    main    = "index.js"
    dependencies = {
      "@google-cloud/functions-framework" = "^3.0.0"
      "@google-cloud/storage"             = "^6.0.0"
    }
  })

  content_type = "application/json"
}

# Cloud Function for SPA routing
resource "google_cloudfunctions2_function" "spa_router" {
  count = var.enable_spa_routing && var.enable_edge_function ? 1 : 0

  name        = "${var.project_name}-${var.environment}-spa-router"
  location    = var.region
  description = "Edge function for SPA routing"

  build_config {
    runtime     = "nodejs20"
    entry_point = "spaRouter"

    source {
      storage_source {
        bucket = google_storage_bucket.static_site.name
        object = "cloud-functions/spa-router/"
      }
    }
  }

  service_config {
    max_instance_count = 100
    min_instance_count = 0
    timeout_seconds    = 60

    environment_variables = {
      BUCKET_NAME = google_storage_bucket.static_site.name
    }

    ingress_settings               = "ALLOW_ALL"
    all_traffic_on_latest_revision = true
  }

  depends_on = [
    google_storage_bucket_object.spa_router_code,
    google_storage_bucket_object.spa_router_package
  ]
}

# Output the function URL
output "spa_router_url" {
  value       = var.enable_spa_routing && var.enable_edge_function ? google_cloudfunctions2_function.spa_router[0].url : null
  description = "URL of the SPA router Cloud Function"
}