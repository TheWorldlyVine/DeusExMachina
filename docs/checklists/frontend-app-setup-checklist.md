# Frontend App Setup Checklist

This checklist ensures proper setup of new frontend applications in the DeusExMachina monorepo, preventing common mistakes and ensuring consistency with established patterns.

## Pre-Implementation Review

### 📚 Documentation Review
- [ ] Read `/docs/technical-specs/frontend-static-hosting-and-deployment.md`
- [ ] Read `.github/workflows/main.yml` to understand CI/CD pipeline
- [ ] Check for existing similar apps in `/apps/frontend/`
- [ ] Review relevant technical specifications for the feature
- [ ] Identify deployment path (typically `/app-name/`)

### 🔍 Pattern Analysis
- [ ] Understand how existing frontend apps are structured
- [ ] Check how static assets are deployed (subdirectory pattern)
- [ ] Review routing patterns in existing apps
- [ ] Identify shared components in `/packages/ui-components/`
- [ ] Check authentication patterns if applicable

## App Creation

### 🏗️ Project Structure
```bash
apps/frontend/<app-name>/
├── public/
│   ├── 404.html          # Required for client-side routing
│   └── [static assets]
├── src/
│   ├── components/
│   ├── pages/
│   ├── services/
│   ├── stores/
│   ├── utils/
│   ├── App.tsx
│   ├── main.tsx
│   └── router.tsx
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── vitest.config.ts
```

### ⚙️ Configuration Files

#### 1. Vite Configuration
```typescript
// vite.config.ts
export default defineConfig({
  plugins: [react()],
  base: '/<app-name>/',  // CRITICAL: Must match deployment path
  // ... rest of config
});
```

#### 2. Router Configuration
```typescript
// src/router.tsx
export const router = createBrowserRouter([
  // routes...
], {
  basename: '/<app-name>'  // CRITICAL: Must match deployment path
});
```

#### 3. 404 Handling
```html
<!-- public/404.html -->
<!DOCTYPE html>
<html lang="en">
  <head>
    <script>
      const path = window.location.pathname;
      const base = '/<app-name>';
      
      if (path.startsWith(base)) {
        window.location.replace(base + '/index.html');
      } else {
        window.location.replace(base + path);
      }
    </script>
  </head>
  <body></body>
</html>
```

#### 4. Package.json Scripts
```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "test": "vitest run",
    "lint": "eslint src --ext ts,tsx",
    "type-check": "tsc --noEmit"
  }
}
```

## Testing Setup

### 🧪 Test Infrastructure
- [ ] Configure Vitest with jsdom environment
- [ ] Set up Testing Library and utilities
- [ ] Create test setup file with common mocks
- [ ] Ensure tests pass before first commit
- [ ] Aim for 80%+ code coverage

### 📋 Test Checklist
- [ ] Unit tests for all components
- [ ] Integration tests for key flows
- [ ] Routing tests for navigation
- [ ] API mocking configured
- [ ] Error boundary testing

## Production Configuration

### 🌍 Environment Variables
- [ ] Create `.env.example` with all required vars
- [ ] Document environment variables in README
- [ ] Configure API endpoints for production
- [ ] Set up error tracking (if applicable)
- [ ] Configure analytics (if applicable)

### 🔐 API Configuration
```typescript
// src/config/api.ts
const API_BASE_URL = import.meta.env.PROD 
  ? 'https://<region>-<project-id>.cloudfunctions.net'
  : 'http://localhost:8080';
```

## Deployment Verification

### 🚀 Pre-Deployment
- [ ] Run `pnpm build` successfully
- [ ] Check `dist/` output structure
- [ ] Verify all assets use relative paths
- [ ] Test 404.html redirect locally
- [ ] Ensure no hardcoded URLs

### 📦 CI/CD Integration
- [ ] Verify app is included in CI/CD pipeline
- [ ] Check that build output goes to `dist/`
- [ ] Confirm deployment path matches configuration
- [ ] Test that GitHub Actions picks up changes
- [ ] Verify cache headers will be set correctly

### 🌐 Post-Deployment
- [ ] Access app at `https://<static-url>/<app-name>/`
- [ ] Test all client-side routes work
- [ ] Verify 404 handling redirects properly
- [ ] Check browser console for errors
- [ ] Test API connections (if applicable)

## Documentation

### 📝 Required Documentation
- [ ] Update app's README with:
  - [ ] Purpose and features
  - [ ] Development setup
  - [ ] Build and deployment info
  - [ ] Environment variables
  - [ ] Testing instructions
- [ ] Add app to main README's project structure
- [ ] Document any new patterns introduced
- [ ] Update technical specs if needed
- [ ] Add to any relevant ADRs

### 🔗 Cross-References
- [ ] Link to related technical specs
- [ ] Reference shared components used
- [ ] Note API endpoints consumed
- [ ] Document deployment URL
- [ ] Link to monitoring dashboards

## Common Pitfalls to Avoid

### ❌ Don't
- Create separate infrastructure for each app
- Hardcode absolute URLs
- Forget the base path configuration
- Skip the 404.html for SPAs
- Assume deployment paths
- Ignore existing patterns

### ✅ Do
- Follow existing app patterns
- Use environment variables for config
- Test locally with base path
- Check CI/CD logs after push
- Document deployment details
- Ask questions when unsure

## Final Verification

### 🎯 Success Criteria
- [ ] App builds without errors
- [ ] All tests pass
- [ ] Linting passes
- [ ] Type checking passes
- [ ] Deploys automatically on push to main
- [ ] Accessible at correct URL
- [ ] All routes work in production
- [ ] No console errors in production
- [ ] Performance budget met
- [ ] Documentation complete

## Quick Commands Reference

```bash
# Create new app
cd apps/frontend/
mkdir <app-name>
cd <app-name>

# Initialize
pnpm init
pnpm add react react-dom react-router-dom
pnpm add -D vite @vitejs/plugin-react typescript vitest

# Test build
pnpm build

# Preview production build
pnpm preview

# Run all checks
pnpm lint && pnpm type-check && pnpm test
```

## Notes

- This checklist is based on lessons learned from the auth implementation
- Always check for updates to deployment patterns
- When in doubt, look at recently created apps for current patterns
- The CI/CD pipeline is the source of truth for deployment behavior

Remember: **The monorepo already has solutions for most problems. Look for existing patterns before creating new ones.**