# Infrastructure as Code

This directory contains Terraform configurations for provisioning DeusExMachina infrastructure on Google Cloud Platform.

## Structure

```
terraform/
├── modules/
│   └── static-hosting/    # GCS bucket for frontend static hosting
└── environments/
    └── production/        # Production environment configuration
```

## Setup

1. Install Terraform (>= 1.0)
2. Authenticate with Google Cloud:
   ```bash
   gcloud auth application-default login
   ```

3. Navigate to the environment directory:
   ```bash
   cd environments/production
   ```

4. Create a `terraform.tfvars` file:
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   # Edit terraform.tfvars with your project ID
   ```

5. Initialize Terraform:
   ```bash
   terraform init
   ```

6. Plan the deployment:
   ```bash
   terraform plan
   ```

7. Apply the configuration:
   ```bash
   terraform apply
   ```

## Resources Created

- **Google Cloud Storage Bucket**: For hosting static frontend applications
  - Public read access enabled
  - Website configuration for serving index.html
  - CORS enabled for API access

## Outputs

After applying the Terraform configuration, you'll get:
- `frontend_bucket_name`: The name of the created GCS bucket
- `frontend_website_url`: The public URL for accessing the static site