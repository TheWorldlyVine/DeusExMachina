# Cloud Functions Configuration

This document tracks the resource configurations for our Cloud Functions.

## Memory Configuration

| Function | Memory | Reason |
|----------|--------|---------|
| auth-function | 512Mi | Handles authentication, email sending via Pub/Sub, and Firestore operations. Was hitting 256Mi limit. |
| api-function | 1Gi | Main API with potentially heavy operations, multiple dependencies, and concurrent requests. |
| processor-function | 512Mi | Background processing tasks, similar load to auth-function. |

## Scaling Configuration

| Function | Max Instances | Min Instances | Reason |
|----------|---------------|---------------|---------|
| auth-function | 100 | 0 | Scales based on auth requests, can scale to zero when idle |
| api-function | 200 | 0* | Higher limit for main API traffic |
| processor-function | 50 | 0 | Lower limit as it processes background tasks |

*Consider setting min instances to 1 for api-function to reduce cold starts.

## Timeout Configuration

| Function | Timeout | Reason |
|----------|---------|---------|
| auth-function | 300s (5 min) | Standard timeout for auth operations |
| api-function | 300s (5 min) | Standard timeout for API operations |
| processor-function | 540s (9 min) | Extended timeout for long-running background tasks |

## Deployment

These configurations are set in `.github/workflows/main.yml` during the Cloud Functions deployment step.

## Monitoring

Monitor memory usage with:
```bash
gcloud logging read 'resource.type="cloud_run_revision" 
  resource.labels.service_name="auth-function" 
  textPayload:"Memory limit"' \
  --limit=10 --project=deus-ex-machina-prod
```

## History

- 2025-08-02: Increased auth-function memory from 256Mi to 512Mi due to OOM errors during email sending