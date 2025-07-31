# DeusExMachina

A production-ready monorepo containing Java-based GCP Cloud Functions, React frontend applications, and infrastructure as code using Terraform.

## Project Structure

```
DeusExMachina/
├── apps/                      # Deployable applications
│   ├── backend/              # Java GCP Cloud Functions
│   └── frontend/             # React applications
├── packages/                 # Shared libraries
│   ├── ui-components/       # React component library
│   ├── java-common/         # Shared Java utilities
│   └── utils/               # Cross-platform utilities
├── infrastructure/          # Terraform IaC
│   ├── environments/        # Environment-specific configs
│   └── modules/             # Reusable Terraform modules
├── docs/                    # Documentation
└── tools/                   # Build tools and scripts
```

## Prerequisites

- Node.js >= 20.0.0
- pnpm >= 8.0.0
- Java 21
- Gradle 8.x
- Terraform >= 1.6.0
- Google Cloud SDK
- GCP Project with billing enabled
- GitHub repository with Actions enabled

## GCP Setup Guide

### 1. Create a GCP Project

```bash
# Set your project ID (must be globally unique)
export PROJECT_ID="deus-ex-machina-prod"
export PROJECT_NAME="DeusExMachina"

# Create the project
gcloud projects create $PROJECT_ID --name="$PROJECT_NAME"

# Set the project as default
gcloud config set project $PROJECT_ID

# Link a billing account (replace with your billing account ID)
gcloud beta billing projects link $PROJECT_ID --billing-account=XXXXXX-XXXXXX-XXXXXX
```

### 2. Enable Required APIs

```bash
# Enable all required Google Cloud APIs
gcloud services enable \
  compute.googleapis.com \
  cloudfunctions.googleapis.com \
  cloudbuild.googleapis.com \
  storage.googleapis.com \
  secretmanager.googleapis.com \
  cloudresourcemanager.googleapis.com \
  iam.googleapis.com \
  logging.googleapis.com \
  monitoring.googleapis.com \
  dns.googleapis.com \
  certificatemanager.googleapis.com
```

### 3. Create Service Account for Terraform

```bash
# Create service account for Terraform
gcloud iam service-accounts create terraform-sa \
  --display-name="Terraform Service Account" \
  --description="Service account for Terraform infrastructure management"

# Grant necessary roles
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:terraform-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/editor"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:terraform-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/resourcemanager.projectIamAdmin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:terraform-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.admin"

# Create and download service account key
gcloud iam service-accounts keys create terraform-key.json \
  --iam-account=terraform-sa@$PROJECT_ID.iam.gserviceaccount.com

# IMPORTANT: Keep this key secure and never commit it to Git!
```

### 4. Create Terraform State Bucket

```bash
# Create bucket for Terraform state
gsutil mb -p $PROJECT_ID -c STANDARD -l us-central1 gs://$PROJECT_ID-terraform-state/

# Enable versioning
gsutil versioning set on gs://$PROJECT_ID-terraform-state/

# Enable bucket-level uniform access
gsutil uniformbucketlevelaccess set on gs://$PROJECT_ID-terraform-state/
```

### 5. Configure GitHub Secrets

Add the following secrets to your GitHub repository (Settings → Secrets and variables → Actions):

- `GCP_SA_KEY`: Contents of the service account key JSON file
- `GCP_PROJECT_ID`: Your GCP project ID

### 6. Deploy Infrastructure via GitHub Actions

The infrastructure is deployed automatically through GitHub Actions:

1. **Push to main branch** with changes in `infrastructure/` folder
2. **Or manually trigger** the deployment:
   - Go to Actions → Infrastructure Deployment → Run workflow
   - Select the environment (prod/staging/dev)
   - Click "Run workflow"

The workflow will:
- Create the Terraform state bucket automatically
- Deploy all infrastructure (VPC, Static Hosting, CDN, Load Balancer)
- Output the static hosting URL and bucket name
- Configure everything for the frontend deployment

**No local Terraform required!**

### 7. Deploy Initial Test Page

```bash
# The test index.html is automatically deployed by Terraform
# You can access it at the static IP address:
echo "Visit: https://$(terraform output -raw static_hosting_ip)"

# Or manually deploy files:
gsutil -m cp index.html gs://$(terraform output -raw static_hosting_bucket)/
```

### 8. Configure Custom Domain (Optional)

1. Add an A record in your DNS pointing to the static IP:
   ```
   Type: A
   Name: @ (or subdomain)
   Value: <static_ip_from_terraform>
   ```

2. Update the Terraform configuration with your domain:
   ```hcl
   domain_name = "your-domain.com"
   ```

3. Re-apply Terraform to provision SSL certificate:
   ```bash
   terraform apply
   ```

### 9. Local Development Authentication

For local development, authenticate with GCP:

```bash
# Login with your Google account
gcloud auth login

# Set application default credentials
gcloud auth application-default login

# Set the default project
gcloud config set project $PROJECT_ID
```

## Getting Started

### Initial Setup

1. Clone the repository:
```bash
git clone https://github.com/your-org/DeusExMachina.git
cd DeusExMachina
```

2. Install dependencies:
```bash
pnpm install
```

3. Set up environment variables:
```bash
cp .env.example .env
# Edit .env with your configuration
```

### Development

#### Frontend Development
```bash
# Start the web application
pnpm --filter web-app dev

# Run all frontend tests
pnpm run test

# Build all frontend applications
pnpm run build
```

#### Backend Development
```bash
# Run a specific function locally
./gradlew :apps:backend:auth-function:runFunction

# Run all backend tests
./gradlew test

# Build all functions
./gradlew build
```

#### Infrastructure
```bash
# Initialize Terraform
cd infrastructure/environments/dev
terraform init

# Plan changes
terraform plan

# Apply changes
terraform apply
```

## Architecture

### Backend Services
- **auth-function**: Authentication service with JWT token generation
- **api-function**: Main API service for business logic
- **processor-function**: Async processing service

### Frontend Applications
- **web-app**: Main user-facing application
- **admin-panel**: Administrative interface

### Shared Packages
- **ui-components**: Reusable React components with Storybook
- **java-common**: Shared Java utilities and base classes
- **utils**: Cross-platform utility functions

## Testing

### Unit Tests
```bash
# Frontend
pnpm test

# Backend
./gradlew test
```

### Integration Tests
```bash
# Run integration test suite
pnpm run test:integration
```

### E2E Tests
```bash
# Run Cypress tests
pnpm run test:e2e
```

## CI/CD

The project uses GitHub Actions for continuous integration and deployment:

- **Main Pipeline**: Runs on every push and PR
- **Security Scanning**: Daily vulnerability scans
- **Dependency Updates**: Weekly dependency updates

## Documentation

- [Technical Specifications](./docs/technical-specs/)
- [Product Requirements](./docs/prds/)
- [Architecture Decision Records](./docs/adrs/)
- [API Documentation](./docs/api/)

## Troubleshooting

### Common Issues

1. **Terraform state lock error**
   ```bash
   # Force unlock if needed (use with caution)
   terraform force-unlock <LOCK_ID>
   ```

2. **Permission denied errors**
   - Ensure your service account has all required roles
   - Check if APIs are enabled: `gcloud services list --enabled`

3. **Static site not accessible**
   - Check if the bucket is public: `gsutil iam get gs://BUCKET_NAME`
   - Verify load balancer is healthy: Check GCP Console → Network Services → Load Balancing
   - SSL certificate provisioning can take up to 24 hours for custom domains

4. **CDN not caching properly**
   - Check cache headers: `curl -I https://your-site.com/file.js`
   - Verify CDN is enabled on backend bucket in GCP Console
   - Clear CDN cache: `gcloud compute url-maps invalidate-cdn-cache URL_MAP_NAME --path "/*"`

5. **GitHub Actions deployment failing**
   - Verify GCP_SA_KEY secret is set correctly
   - Check if service account has storage.admin role
   - Ensure bucket name and URL map name secrets are correct

## Static Hosting Features

The static hosting infrastructure includes:

- **Global CDN**: Content served from edge locations worldwide
- **Auto-SSL**: Managed SSL certificates with automatic renewal  
- **Security Headers**: HSTS, XSS Protection, Content-Type Options
- **Cloud Armor**: DDoS protection and security policies
- **Cache Control**: Optimized caching for different file types
- **CORS Support**: Configurable cross-origin resource sharing
- **404 Handling**: Custom error pages
- **Compression**: Automatic gzip/brotli compression

## Contributing

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.