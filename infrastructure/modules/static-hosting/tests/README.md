# Static Hosting Module Tests

This directory contains Terratest tests for the static hosting module, with a focus on validating SPA (Single Page Application) routing functionality.

## Prerequisites

1. Go 1.21 or later
2. Google Cloud SDK (`gcloud`) configured
3. Valid GCP project with necessary permissions
4. Environment variable `GOOGLE_PROJECT` set to your GCP project ID

## Test Structure

### Unit Tests
- `terraform_validation_test.go` - Validates Terraform configuration syntax and variables
- Fast, no infrastructure created
- Run frequently during development

### Integration Tests
- `spa_routing_test.go` - Tests actual SPA routing behavior with real infrastructure
- Creates and destroys real GCP resources
- Run before merging changes

## Running Tests

### Quick Validation (Recommended for Development)
```bash
make test-validation
```

### Full Test Suite (Without Real Infrastructure)
```bash
make test
```

### Integration Tests (Creates Real Infrastructure)
```bash
export GOOGLE_PROJECT=your-project-id
make test-integration
```

### Run Specific Test
```bash
make test-specific TEST_NAME=TestSPARoutingConfiguration
```

## Test Cases Covered

### SPA Routing Tests
1. **Root Path Access** - Verifies landing page serves correctly
2. **App Root Access** - Tests `/app-name` serves app's index.html
3. **Deep Route Access** - Tests `/app-name/route` serves app's index.html
4. **Nested Route Access** - Tests `/app-name/route/subroute` routing
5. **Static Asset Access** - Verifies JS/CSS files serve directly
6. **404 Handling** - Ensures missing assets return proper 404
7. **Refresh Behavior** - Tests that page refresh maintains routing

### Configuration Tests
1. **Variable Validation** - Ensures all variables work correctly
2. **URL Map Configuration** - Validates load balancer rules
3. **Required Variables** - Tests that missing variables fail appropriately

## Important Notes

1. **Costs**: Integration tests create real GCP resources. While minimal, there may be small charges.
2. **Cleanup**: Tests automatically clean up resources, but verify in GCP console if tests fail.
3. **Timing**: Allow 30-60 seconds for load balancer propagation in tests.
4. **Permissions**: Ensure service account has necessary permissions:
   - `compute.urlMaps.*`
   - `compute.backendBuckets.*`
   - `storage.buckets.*`
   - `compute.globalAddresses.*`
   - `compute.sslCertificates.*`

## Debugging Failed Tests

1. Check test output for specific error messages
2. Verify GCP permissions and project configuration
3. Check if resources were created in GCP console
4. Review Terraform state if tests fail during cleanup
5. Use `terraform destroy` manually if automatic cleanup fails

## Adding New Tests

1. Follow existing test patterns
2. Use descriptive test names
3. Clean up all resources in defer blocks
4. Add appropriate timeouts for GCP operations
5. Document any new test scenarios in this README