# Cloud Run Services Module

This module manages Cloud Run services that are deployed via CI/CD. It handles:
- IAM policies for public/private access
- Environment variable documentation
- Service discovery

## Important Notes

1. **Services must exist**: This module uses data sources to reference existing Cloud Run services. The services must be deployed before running Terraform.

2. **CORS and Authentication**: Services are configured with `allow_unauthenticated = true` to handle CORS preflight requests. Authentication is implemented at the application level.

3. **Environment Variables**: While this module documents required environment variables, they are actually set during deployment via GitHub Actions.

## Usage

```hcl
module "novel_services" {
  source = "../../modules/cloud-run-services"
  
  project_id = var.project_id
  region     = var.region
  
  services = {
    document_service = {
      name                  = "novel-document-service"
      allow_unauthenticated = true
      environment_variables = {
        GCP_PROJECT_ID = var.project_id
      }
    }
  }
}
```

## Variables

| Name | Description | Type | Default |
|------|-------------|------|---------|
| project_id | GCP project ID | string | - |
| region | GCP region | string | us-central1 |
| services | Map of service configurations | map(object) | {} |

## Outputs

| Name | Description |
|------|-------------|
| service_urls | URLs of the Cloud Run services |
| service_names | Names of the Cloud Run services |
| service_regions | Regions where services are deployed |