# E2E Testing for SPA Routing

This document describes the end-to-end tests for verifying SPA routing functionality in the Novel Creator application.

## Overview

The E2E tests ensure that Single Page Application routing works correctly in production, specifically testing:
- Direct URL access to deep routes
- Page refresh behavior
- Browser navigation (back/forward)
- Static asset handling
- Cross-app isolation

## Test Setup

### Install Dependencies

```bash
pnpm install
```

### Running Tests

#### Local Development
```bash
# Run all E2E tests against local dev server
pnpm test:e2e

# Run with UI mode for debugging
pnpm test:e2e:ui
```

#### Production Testing
```bash
# Test against production URL
pnpm test:e2e:prod

# Quick routing verification
pnpm test:spa-routing
```

#### Custom Environment
```bash
# Test against staging or other environments
E2E_BASE_URL=https://staging.god-in-a-box.com pnpm test:e2e
```

## Test Structure

### 1. SPA Routing E2E Tests (`spa-routing.e2e.test.ts`)

Full Playwright-based tests that use a real browser to verify:

- **Direct Navigation**: Tests accessing routes directly via URL
- **Page Refresh**: Verifies routes are maintained after refresh
- **Browser Navigation**: Tests back/forward button behavior
- **Static Assets**: Ensures JS/CSS files are served correctly
- **Performance**: Checks cache headers and response times

### 2. Quick Routing Test Script (`test-spa-routing.sh`)

Bash script for rapid verification using curl:

```bash
# Run quick tests
./test-spa-routing.sh

# Test different environment
BASE_URL=https://staging.god-in-a-box.com ./test-spa-routing.sh
```

## Test Cases

### Critical Path Tests

1. **Root Access**
   - URL: `/novel-creator/`
   - Expected: 200 status, React app loads

2. **Documents Page**
   - URL: `/novel-creator/documents`
   - Expected: 200 status, documents component renders

3. **Editor with ID**
   - URL: `/novel-creator/editor/[document-id]`
   - Expected: 200 status, editor loads with document

4. **Settings Page**
   - URL: `/novel-creator/settings`
   - Expected: 200 status, settings component renders

### Edge Cases

1. **Nested Routes**
   - URL: `/novel-creator/settings/profile`
   - Expected: Proper routing to nested component

2. **Non-existent Routes**
   - URL: `/novel-creator/non-existent-page`
   - Expected: 200 status (SPA handles 404 client-side)

3. **Static File 404**
   - URL: `/novel-creator/assets/missing.js`
   - Expected: 404 status (not rewritten to index.html)

## Adding Test IDs

To support E2E tests, add data-testid attributes to key components:

```tsx
// DocumentsPage.tsx
<div data-testid="documents-page">
  {/* content */}
</div>

// EditorPage.tsx
<div data-testid="editor-page">
  {/* content */}
</div>

// SettingsPage.tsx
<div data-testid="settings-page">
  {/* content */}
</div>
```

## CI/CD Integration

Add to your GitHub Actions workflow:

```yaml
- name: Run E2E Tests
  run: |
    pnpm install
    pnpm test:e2e:prod
  env:
    E2E_BASE_URL: ${{ secrets.PRODUCTION_URL }}
```

## Troubleshooting

### Tests Fail Locally
1. Ensure dev server is running: `pnpm dev`
2. Check if port 5173 is available
3. Verify no CORS issues

### Tests Fail in Production
1. Check if the infrastructure changes are deployed
2. Verify URL map configuration in GCP
3. Check Cloud CDN cache - may need invalidation
4. Review browser console for JavaScript errors

### Debugging Tips
1. Run tests with UI mode: `pnpm test:e2e:ui`
2. Enable trace on failure for detailed debugging
3. Check screenshots in `test-results/` directory
4. Use `--debug` flag for verbose output

## Performance Benchmarks

Expected performance metrics:
- Initial page load: < 3s
- Route navigation: < 100ms
- Page refresh: < 2s
- Static asset loading: < 500ms

## Maintenance

### Monthly Tasks
1. Run full E2E suite against production
2. Update test cases for new routes
3. Review and update performance benchmarks
4. Check for flaky tests and fix

### When Adding New Routes
1. Add test case to `spa-routing.e2e.test.ts`
2. Update `test-spa-routing.sh` if needed
3. Add data-testid to new components
4. Run tests locally before deploying