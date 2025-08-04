# Makefile for running Terratest tests

.PHONY: all test test-unit test-integration test-validation deps clean

# Default target
all: test

# Install dependencies
deps:
	go mod download
	go mod tidy

# Run all tests
test: test-validation test-unit

# Run only validation tests (fast, no infrastructure)
test-validation:
	go test -v -run TestTerraformValidation -timeout 5m
	go test -v -run TestRequiredVariables -timeout 5m
	go test -v -run TestSPARoutingVariables -timeout 5m

# Run unit tests (no real infrastructure)
test-unit: test-validation

# Run integration tests (creates real infrastructure)
test-integration:
	@echo "Running integration tests (this will create real GCP resources)..."
	@echo "Make sure GOOGLE_PROJECT is set"
	go test -v -run TestSPARoutingConfiguration -timeout 30m
	go test -v -run TestSPARoutingWithRefresh -timeout 30m
	go test -v -run TestURLMapConfiguration -timeout 30m

# Run a specific test
test-specific:
	go test -v -run $(TEST_NAME) -timeout 30m

# Clean up
clean:
	rm -rf .terraform/
	rm -rf terraform.tfstate*
	rm -rf /tmp/terraform-plan-*

# Format Go code
fmt:
	go fmt ./...

# Lint Go code
lint:
	golangci-lint run

# Help
help:
	@echo "Available targets:"
	@echo "  deps              - Install Go dependencies"
	@echo "  test              - Run all tests"
	@echo "  test-validation   - Run validation tests only (fast)"
	@echo "  test-unit         - Run unit tests"
	@echo "  test-integration  - Run integration tests (creates GCP resources)"
	@echo "  test-specific     - Run specific test (use TEST_NAME=TestName)"
	@echo "  clean             - Clean up temporary files"
	@echo "  fmt               - Format Go code"
	@echo "  lint              - Lint Go code"