/**
 * Cloud Function to invalidate CDN cache
 * Triggered when objects are created/updated in the storage bucket
 */

exports.invalidateCache = async (event, context) => {
  const { Compute } = require('@google-cloud/compute');
  const compute = new Compute();
  
  const file = event.name;
  const bucket = event.bucket;
  
  console.log(`File ${file} was updated in bucket ${bucket}`);
  
  // Get the URL map name from environment variable
  const urlMapName = process.env.URL_MAP_NAME;
  
  if (!urlMapName) {
    console.error('URL_MAP_NAME environment variable not set');
    return;
  }
  
  try {
    // Invalidate the specific path in the CDN
    const path = `/${file}`;
    
    const [operation] = await compute.urlMaps.invalidateCache(urlMapName, {
      path: path,
    });
    
    console.log(`Cache invalidation started for path: ${path}`);
    
    // Wait for the operation to complete
    await operation.promise();
    
    console.log(`Cache invalidation completed for path: ${path}`);
  } catch (error) {
    console.error('Error invalidating cache:', error);
    throw error;
  }
};