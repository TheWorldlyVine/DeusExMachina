# Terraform Error Handling in CI/CD

This document explains how we handle different types of Terraform errors in our CI/CD pipeline.

## Error Types and Handling

### 409 Errors (Resource Already Exists)
These errors occur when Terraform tries to create a resource that already exists in GCP. This is expected in certain scenarios:
- Manual resource creation for testing
- Resources created outside of Terraform
- State file inconsistencies

**Handling:** We continue with the deployment as these don't indicate actual problems.

### 403 Errors (Permission Denied)
These errors indicate that the service account lacks necessary permissions. Common causes:
- Missing IAM roles
- Insufficient permissions for resource creation
- API not enabled

**Handling:** We fail the deployment immediately. These must be fixed by granting proper permissions.

### Other Errors
Any other errors (500, 400, etc.) indicate actual problems that need to be addressed.

**Handling:** We fail the deployment immediately.

## Implementation

The `scripts/terraform-apply-smart.sh` script implements this logic:

1. Runs `terraform apply`
2. If it fails, analyzes the error types
3. Only continues if ALL errors are 409 (already exists)
4. Fails if ANY errors are 403 (permission denied) or other types

## Granting Permissions

If you encounter permission errors, run:
```bash
./scripts/grant-github-actions-permissions.sh
```

This script must be run by someone with IAM admin permissions in the project.

## Importing Existing Resources

For resources that already exist, you can import them into Terraform state:
```bash
./scripts/import-terraform-resources.sh
```

This prevents 409 errors by bringing existing resources under Terraform management.