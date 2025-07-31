# Technical Specification: Frontend Static Hosting and Deployment

## Overview
This specification defines the infrastructure and deployment pipeline for hosting static frontend files (starting with index.html) on Google Cloud Platform. The solution uses Cloud Storage for static file hosting, Cloud CDN for global distribution, and Cloud Load Balancer for HTTPS termination, with Terraform managing all infrastructure as code.

## Problem Statement
The DeusExMachina monorepo requires a scalable, cost-effective solution for hosting frontend static files that:
- Integrates with our existing GCP infrastructure and Java Cloud Functions backend
- Supports future migration to SSR without major architectural changes
- Provides global CDN distribution for optimal performance
- Maintains infrastructure as code using Terraform
- Enables automated deployments through our CI/CD pipeline

## Technical Requirements
### Functional Requirements
- Host static files (HTML, CSS, JS, images) with 99.9% availability
- Serve files over HTTPS with automatic SSL certificate management
- Support custom domain mapping (e.g., app.myorg.com)
- Enable versioned deployments with instant rollback capability
- Provide path-based routing to backend Cloud Functions
- Support index.html as default document for directory requests

### Non-Functional Requirements
- Performance: First byte response time < 100ms from CDN edge
- Scalability: Handle 100K+ concurrent users without degradation
- Security: HTTPS-only with modern TLS protocols (1.2+)
- Cost: < $50/month for typical usage (10GB storage, 1TB transfer)
- Deployment: Zero-downtime deployments with < 5 minute propagation

## Architecture Design
### System Architecture
```
┌─────────────────┐     ┌──────────────────┐     ┌────────────────┐
│   GitHub Repo   │────▶│  GitHub Actions  │────▶│ Cloud Storage  │
└─────────────────┘     └──────────────────┘     └────────────────┘
                                                           │
┌─────────────────┐     ┌──────────────────┐             ▼
│     Users       │────▶│  Cloud CDN       │◀────┌────────────────┐
└─────────────────┘     └──────────────────┘     │ Load Balancer  │
                                                  └────────────────┘
                                                           │
                                                           ▼
                                                  ┌────────────────┐
                                                  │ Cloud Functions│
                                                  │   (Backend)    │
                                                  └────────────────┘
```

### Data Flow
1. **Build Phase**: GitHub Actions builds React app → produces static files
2. **Deploy Phase**: Static files uploaded to Cloud Storage bucket with versioning
3. **Request Flow**:
    - User requests https://app.myorg.com
    - Cloud Load Balancer receives request
    - Static paths (/, /assets/*) → Cloud CDN → Cloud Storage
    - API paths (/api/*) → Cloud Functions backend
4. **Cache Flow**: CDN caches static assets at edge locations globally

### API Design
No API endpoints for static hosting. Load balancer URL mapping rules:
- `/*` → Cloud Storage bucket (default)
- `/api/*` → Cloud Functions backend
- `/health` → Custom health check endpoint

## Implementation Plan
### Phase 1: Foundation (Week 1)
- Create Terraform module for static hosting infrastructure
    - Cloud Storage bucket with website configuration
    - Cloud Load Balancer with SSL certificate
    - Basic CDN configuration
- Configure bucket permissions and CORS
- Set up staging environment
- Create initial index.html test page

### Phase 2: CDN and Security (Week 2)
- Configure Cloud CDN caching policies
    - HTML files: 5 minute cache
    - CSS/JS files: 1 year cache with content hashing
    - Images: 30 day cache
- Implement Cloud Armor security policies
- Set up custom domain and SSL certificates
- Configure proper security headers

### Phase 3: CI/CD Integration (Week 3)
- Create GitHub Actions workflow for deployment
- Implement blue-green deployment strategy
- Add deployment notifications
- Configure environment-specific deployments

### Phase 4: Monitoring and Testing (Week 4)
- Set up Cloud Monitoring dashboards
- Configure uptime checks and alerts
- Implement performance budget testing
- Create rollback procedures

## Testing Strategy
### Infrastructure Tests
- Terraform validation and planning
- Infrastructure compliance testing with Terratest
- SSL certificate validation
- CDN cache header verification

### Performance Tests
- Lighthouse CI integration (target score > 90)
- Load testing with 10K concurrent users
- Global latency testing from multiple regions
- Cache hit ratio validation (target > 95%)

### Integration Tests
- Verify static file serving
- Test custom domain resolution
- Validate CORS headers
- Test error page handling (404, 500)

### Security Tests
- SSL/TLS configuration scanning
- Security header validation
- Cloud Armor rule testing
- Penetration testing for common web vulnerabilities

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Cache invalidation delays | Medium | Implement cache busting via file versioning; maintain low TTL for HTML |
| Accidental file deletion | High | Enable object versioning; implement 7-day soft delete policy |
| SSL certificate expiration | High | Use Google-managed certificates with auto-renewal |
| CDN outage | Medium | Cloud Storage serves as origin; implement multi-region bucket |
| Deployment failures | Medium | Blue-green deployment with automated rollback |
| Cost overruns | Low | Set up budget alerts; implement bandwidth monitoring |
| CORS misconfiguration | Medium | Thoroughly test in staging; use restrictive defaults |

## Timeline
- **Week 1**: Foundation
    - Days 1-2: Terraform module development
    - Days 3-4: Staging environment setup
    - Day 5: Initial testing and documentation

- **Week 2**: CDN and Security
    - Days 1-2: CDN configuration and optimization
    - Days 3-4: Security policies and SSL setup
    - Day 5: Performance testing

- **Week 3**: CI/CD Integration
    - Days 1-2: GitHub Actions workflow
    - Days 3-4: Deployment strategies
    - Day 5: Integration testing

- **Week 4**: Monitoring and Production
    - Days 1-2: Monitoring setup
    - Days 3-4: Production deployment
    - Day 5: Documentation and handover

## Appendix: Terraform Module Structure
```
infrastructure/modules/static-hosting/
├── main.tf           # Core resources
├── variables.tf      # Input variables
├── outputs.tf        # Output values
├── cdn.tf           # CDN configuration
├── security.tf      # Cloud Armor rules
└── README.md        # Module documentation
```