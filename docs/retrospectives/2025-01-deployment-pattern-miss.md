# Retrospective: Web App Deployment Pattern Miss

**Date**: January 2025  
**Issue**: Attempted to create duplicate infrastructure for web-app deployment instead of using existing patterns  
**Impact**: Wasted effort, potential infrastructure duplication, confusion  

## What Happened

When implementing the signup page for the web-app, I initially attempted to:
1. Create a new `web_app_hosting` Terraform module in the prod environment
2. Set up separate static hosting infrastructure for the web-app
3. Configure independent deployment outputs

This was unnecessary because the existing infrastructure already handles multiple frontend apps through:
- A single static hosting bucket with subdirectory deployment
- CI/CD pipeline that deploys each app to `/{app-name}/` paths
- Shared CDN and load balancer configuration

## Root Cause Analysis

### Primary Causes
1. **Documentation First Violation**: Failed to follow the #1 guideline in CLAUDE.md - "Always Consult Documentation First"
2. **Pattern Blindness**: Didn't recognize the existing deployment pattern in the CI/CD workflow
3. **Assumption Making**: Assumed each app needed separate infrastructure without verifying

### Contributing Factors
- Focused too narrowly on the auth implementation without considering the full deployment context
- Didn't cross-reference the frontend-static-hosting-and-deployment.md spec
- Made infrastructure decisions without understanding the monorepo's established patterns

## What Should Have Happened

The correct approach would have been:
1. **Read** frontend-static-hosting-and-deployment.md first
2. **Examine** the existing CI/CD pipeline in `.github/workflows/main.yml`
3. **Understand** that apps are deployed to subdirectories automatically
4. **Configure** only the app-specific settings (base path, routing)
5. **Document** the deployment path in the auth tech spec

## Lessons Learned

### Technical Lessons
- The monorepo uses a single static hosting bucket with path-based app deployment
- Frontend apps must configure their base path to match their deployment directory
- Client-side routing requires 404.html configuration for static hosting
- The CI/CD pipeline handles deployment automatically based on directory structure

### Process Lessons
- Always check for existing patterns before creating new infrastructure
- Read ALL relevant technical specs before implementation
- Understand the full deployment lifecycle before writing code
- Cross-reference documentation to ensure consistency

## Prevention Measures

### Immediate Actions
1. Update the auth tech spec to include deployment configuration
2. Add a "Deployment Considerations" section to all frontend specs
3. Create a checklist for new frontend app setup

### Long-term Improvements
1. **Pre-Implementation Checklist**:
   - [ ] Read all related technical specs
   - [ ] Check existing CI/CD configuration
   - [ ] Identify deployment patterns
   - [ ] Verify infrastructure requirements
   - [ ] Document deployment path

2. **Documentation Standards**:
   - Every feature spec must reference deployment specs
   - Include "Production Deployment" sections in all specs
   - Cross-reference related documentation

3. **Code Review Focus**:
   - Check for infrastructure duplication
   - Verify alignment with existing patterns
   - Ensure documentation completeness

## Impact Assessment

### What Went Well
- The existing CI/CD pipeline was robust enough to handle the new app
- No actual infrastructure was duplicated (caught during implementation)
- The mistake was identified and corrected before causing issues

### What Could Have Been Better
- Should have discovered the pattern during planning, not implementation
- The auth tech spec should have included deployment details from the start
- Time was wasted on unnecessary infrastructure configuration

## Action Items

1. ✅ Remove unnecessary infrastructure code
2. ✅ Configure web-app for subdirectory deployment
3. 🔄 Update auth tech spec with deployment details
4. 📝 Create frontend app setup checklist
5. 📝 Add deployment sections to all frontend specs

## Key Takeaway

**Always understand the existing system before adding to it.** The monorepo already had elegant solutions for the problems I was trying to solve. By not following the "Documentation First" principle, I created unnecessary work and complexity.

This retrospective serves as a reminder that in a well-architected system, the patterns are usually already there - we just need to look for them.