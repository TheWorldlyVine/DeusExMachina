package test

import (
	"testing"

	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
)

// TestTerraformValidation ensures the Terraform configuration is valid
func TestTerraformValidation(t *testing.T) {
	t.Parallel()

	terraformOptions := &terraform.Options{
		TerraformDir: "../",
		NoColor:      true,
	}

	// Run terraform init and validate
	terraform.Init(t, terraformOptions)
	terraform.Validate(t, terraformOptions)
}

// TestRequiredVariables ensures all required variables are defined
func TestRequiredVariables(t *testing.T) {
	t.Parallel()

	// Test that missing required variables cause plan to fail
	terraformOptions := &terraform.Options{
		TerraformDir: "../",
		NoColor:      true,
		PlanFilePath: "/tmp/terraform-plan-test",
	}

	// This should fail due to missing required variables
	_, err := terraform.InitAndPlanE(t, terraformOptions)
	assert.Error(t, err, "Plan should fail without required variables")
}

// TestSPARoutingVariables tests the new SPA routing variables
func TestSPARoutingVariables(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		name      string
		vars      map[string]interface{}
		expectErr bool
	}{
		{
			name: "Valid SPA configuration",
			vars: map[string]interface{}{
				"project_id":         "test-project",
				"project_name":       "test",
				"environment":        "dev",
				"enable_spa_routing": true,
				"spa_apps": map[string]interface{}{
					"app1": map[string]interface{}{
						"base_path": "/app1",
						"routes":    []string{"/route1", "/route2"},
					},
				},
			},
			expectErr: false,
		},
		{
			name: "SPA routing disabled",
			vars: map[string]interface{}{
				"project_id":         "test-project",
				"project_name":       "test",
				"environment":        "dev",
				"enable_spa_routing": false,
			},
			expectErr: false,
		},
		{
			name: "Invalid environment",
			vars: map[string]interface{}{
				"project_id":   "test-project",
				"project_name": "test",
				"environment":  "invalid",
			},
			expectErr: true,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			terraformOptions := &terraform.Options{
				TerraformDir: "../",
				Vars:         tc.vars,
				NoColor:      true,
				PlanFilePath: "/tmp/terraform-plan-" + tc.name,
			}

			_, err := terraform.InitAndPlanE(t, terraformOptions)
			if tc.expectErr {
				assert.Error(t, err, "Expected error for test case: %s", tc.name)
			} else {
				assert.NoError(t, err, "Unexpected error for test case: %s", tc.name)
			}
		})
	}
}