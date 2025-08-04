package test

import (
	"fmt"
	"io"
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/gruntwork-io/terratest/modules/gcp"
	"github.com/gruntwork-io/terratest/modules/random"
	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// TestSPARoutingConfiguration tests that the infrastructure properly handles SPA routing
func TestSPARoutingConfiguration(t *testing.T) {
	t.Parallel()

	// Generate unique identifiers
	uniqueID := strings.ToLower(random.UniqueId())
	projectID := gcp.GetGoogleProjectIDFromEnvVar(t)

	// Configure Terraform options
	terraformOptions := &terraform.Options{
		TerraformDir: "../",
		Vars: map[string]interface{}{
			"project_id":         projectID,
			"project_name":       fmt.Sprintf("test-%s", uniqueID),
			"environment":        "dev",
			"enable_spa_routing": true, // New variable to enable SPA routing
			"spa_apps": map[string]interface{}{
				"novel-creator": map[string]interface{}{
					"base_path": "/novel-creator",
					"routes": []string{
						"/documents",
						"/editor/*",
						"/settings",
					},
				},
				"web-app": map[string]interface{}{
					"base_path": "/web-app",
					"routes": []string{
						"/dashboard",
						"/projects/*",
					},
				},
			},
		},
	}

	// Ensure cleanup
	defer terraform.Destroy(t, terraformOptions)

	// Deploy the infrastructure
	terraform.InitAndApply(t, terraformOptions)

	// Get outputs
	bucketName := terraform.Output(t, terraformOptions, "bucket_name")
	loadBalancerIP := terraform.Output(t, terraformOptions, "static_ip_address")

	// Deploy test SPA files
	deployTestSPAFiles(t, bucketName)

	// Wait for load balancer to be ready
	time.Sleep(30 * time.Second)

	// Test cases for SPA routing
	testCases := []struct {
		name           string
		path           string
		expectedStatus int
		expectedBody   string
		description    string
	}{
		{
			name:           "Root path serves landing page",
			path:           "/",
			expectedStatus: 200,
			expectedBody:   "Landing Page",
			description:    "Root should serve landing page index.html",
		},
		{
			name:           "Novel creator root",
			path:           "/novel-creator",
			expectedStatus: 200,
			expectedBody:   "Novel Creator App",
			description:    "App root should serve app's index.html",
		},
		{
			name:           "Novel creator documents route",
			path:           "/novel-creator/documents",
			expectedStatus: 200,
			expectedBody:   "Novel Creator App",
			description:    "SPA route should serve app's index.html",
		},
		{
			name:           "Novel creator editor route",
			path:           "/novel-creator/editor/123",
			expectedStatus: 200,
			expectedBody:   "Novel Creator App",
			description:    "Nested SPA route should serve app's index.html",
		},
		{
			name:           "Web app dashboard route",
			path:           "/web-app/dashboard",
			expectedStatus: 200,
			expectedBody:   "Web App",
			description:    "Different app's route should serve its index.html",
		},
		{
			name:           "Static asset direct access",
			path:           "/novel-creator/assets/app.js",
			expectedStatus: 200,
			expectedBody:   "console.log('app.js')",
			description:    "Static assets should be served directly",
		},
		{
			name:           "Non-existent asset returns 404",
			path:           "/novel-creator/assets/missing.js",
			expectedStatus: 404,
			expectedBody:   "",
			description:    "Missing assets should return 404",
		},
	}

	// Run test cases
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			url := fmt.Sprintf("http://%s%s", loadBalancerIP, tc.path)
			
			// Make request
			resp, err := http.Get(url)
			require.NoError(t, err, "Failed to make request to %s", url)
			defer resp.Body.Close()

			// Check status code
			assert.Equal(t, tc.expectedStatus, resp.StatusCode, 
				"Unexpected status code for %s: %s", tc.path, tc.description)

			// Check body content if expected
			if tc.expectedBody != "" {
				body, err := io.ReadAll(resp.Body)
				require.NoError(t, err)
				assert.Contains(t, string(body), tc.expectedBody,
					"Body doesn't contain expected content for %s", tc.path)
			}

			// Verify URL preservation
			if strings.Contains(tc.path, "/documents") || strings.Contains(tc.path, "/editor") {
				// The original URL should be preserved for client-side routing
				assert.Contains(t, resp.Request.URL.Path, tc.path,
					"Original URL path should be preserved")
			}
		})
	}
}

// TestSPARoutingWithRefresh tests that refreshing on a SPA route works correctly
func TestSPARoutingWithRefresh(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	projectID := gcp.GetGoogleProjectIDFromEnvVar(t)

	terraformOptions := &terraform.Options{
		TerraformDir: "../",
		Vars: map[string]interface{}{
			"project_id":         projectID,
			"project_name":       fmt.Sprintf("test-refresh-%s", uniqueID),
			"environment":        "dev",
			"enable_spa_routing": true,
			"spa_apps": map[string]interface{}{
				"novel-creator": map[string]interface{}{
					"base_path": "/novel-creator",
				},
			},
		},
	}

	defer terraform.Destroy(t, terraformOptions)
	terraform.InitAndApply(t, terraformOptions)

	bucketName := terraform.Output(t, terraformOptions, "bucket_name")
	loadBalancerIP := terraform.Output(t, terraformOptions, "static_ip_address")

	// Deploy test files
	deployTestSPAFiles(t, bucketName)

	// Wait for deployment
	time.Sleep(30 * time.Second)

	// Simulate a browser refresh scenario
	client := &http.Client{
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			// Don't follow redirects automatically
			return http.ErrUseLastResponse
		},
	}

	// Direct access to deep route (simulating refresh)
	deepRouteURL := fmt.Sprintf("http://%s/novel-creator/editor/document-123", loadBalancerIP)
	resp, err := client.Get(deepRouteURL)
	require.NoError(t, err)
	defer resp.Body.Close()

	// Should return 200 with index.html content
	assert.Equal(t, 200, resp.StatusCode, "Direct access to deep route should return 200")
	
	body, _ := io.ReadAll(resp.Body)
	assert.Contains(t, string(body), "Novel Creator App", 
		"Should serve index.html content for deep routes")
}

// deployTestSPAFiles uploads test SPA files to the bucket
func deployTestSPAFiles(t *testing.T, bucketName string) {
	// Landing page
	gcp.UploadFileToGCSBucket(t, "us-central1", bucketName, "index.html", 
		strings.NewReader("<html><body>Landing Page</body></html>"))

	// Novel Creator app
	gcp.UploadFileToGCSBucket(t, "us-central1", bucketName, "novel-creator/index.html",
		strings.NewReader("<html><body>Novel Creator App</body></html>"))
	gcp.UploadFileToGCSBucket(t, "us-central1", bucketName, "novel-creator/assets/app.js",
		strings.NewReader("console.log('app.js');"))

	// Web App
	gcp.UploadFileToGCSBucket(t, "us-central1", bucketName, "web-app/index.html",
		strings.NewReader("<html><body>Web App</body></html>"))
}

// TestURLMapConfiguration verifies the URL map is correctly configured
func TestURLMapConfiguration(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	projectID := gcp.GetGoogleProjectIDFromEnvVar(t)

	terraformOptions := &terraform.Options{
		TerraformDir: "../",
		Vars: map[string]interface{}{
			"project_id":         projectID,
			"project_name":       fmt.Sprintf("test-urlmap-%s", uniqueID),
			"environment":        "dev",
			"enable_spa_routing": true,
			"spa_apps": map[string]interface{}{
				"test-app": map[string]interface{}{
					"base_path": "/test-app",
				},
			},
		},
	}

	defer terraform.Destroy(t, terraformOptions)
	terraform.InitAndApply(t, terraformOptions)

	// Verify URL map resource exists and has correct configuration
	urlMapName := terraform.Output(t, terraformOptions, "url_map_name")
	assert.NotEmpty(t, urlMapName, "URL map name should not be empty")

	// Additional assertions can be added here to verify URL map rules
	// using GCP SDK to fetch and inspect the actual URL map configuration
}