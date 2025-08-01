# Technical Specification: Authentication & Authorization System

## Overview
This specification defines the implementation of a secure, scalable authentication and authorization system for the DeusExMachina platform. The system will provide multiple authentication methods (email/password, Google OAuth, and future social providers), granular permission controls, and team collaboration features. The solution leverages Google Cloud Platform services with Java-based Cloud Functions for backend logic, JWT-based session management, and integration with Google Identity Platform for OAuth providers.

## Problem Statement
DeusExMachina requires a robust authentication and authorization system that:
- Provides frictionless signup/login experience while maintaining high security standards
- Supports multiple authentication methods with seamless account linking
- Enables fine-grained access control for resource sharing and collaboration
- Scales to support 1M+ users with 99.9% availability
- Maintains GDPR compliance and user privacy standards
- Integrates seamlessly with our existing GCP infrastructure and Java Cloud Functions architecture
- Supports future expansion for team workspaces and enterprise SSO

## Technical Requirements
### Functional Requirements
- Support email/password authentication with secure password storage (Argon2id hashing)
- Implement Google OAuth 2.0 integration with automatic account linking
- Provide JWT-based session management with refresh token rotation
- Enable email verification workflow with 24-hour token expiration
- Support password reset flow with secure token generation
- Implement role-based access control (RBAC) with Owner/Editor/Viewer roles
- Enable multi-factor authentication (TOTP-based 2FA)
- Provide account management dashboard with security activity logging
- Support permission inheritance and time-limited access grants
- Enable API key generation for third-party integrations
- Implement session management across multiple devices

### Non-Functional Requirements
- Performance: Authentication requests complete in < 200ms (p95)
- Scalability: Support 10K authentication requests/second at peak
- Availability: 99.9% uptime for authentication services
- Security: Zero-knowledge architecture for passwords, encrypted JWT tokens
- Compliance: GDPR-compliant with data portability and deletion
- Cost: < $500/month for 100K MAU (excluding egress)
- Session persistence: 30-day refresh token validity with sliding window
- Email delivery: 99% delivery rate within 30 seconds

## Architecture Design
### System Architecture
```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client Applications                            │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐            │
│  │  React Web   │    │ Mobile Apps  │    │  Admin Panel │            │
│  └──────────────┘    └──────────────┘    └──────────────┘            │
└─────────────────────────┬─────────────────────────────────────────────┘
                          │ HTTPS
┌─────────────────────────┴─────────────────────────────────────────────┐
│                     Cloud Load Balancer                                │
│                  (SSL Termination, WAF Rules)                         │
└─────────────────────────┬─────────────────────────────────────────────┘
                          │
┌─────────────────────────┴─────────────────────────────────────────────┐
│                    API Gateway (Cloud Endpoints)                       │
│              (Rate Limiting, API Key Validation)                      │
└──────┬──────────────────┬─────────────────┬───────────────────────────┘
       │                  │                   │
┌──────┴───────┐  ┌──────┴───────┐  ┌───────┴──────┐
│Auth Function │  │Session Func  │  │Permission    │
│  - Signup    │  │  - Validate  │  │Function      │
│  - Login     │  │  - Refresh   │  │ - Check      │
│  - OAuth     │  │  - Revoke    │  │ - Grant      │
│  - MFA       │  │              │  │ - Revoke     │
└──────┬───────┘  └──────┬───────┘  └───────┬──────┘
       │                  │                   │
┌──────┴──────────────────┴─────────────────┴───────────────────────────┐
│                        Data Layer                                      │
│  ┌────────────┐  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │  Firestore │  │  Cloud SQL  │  │ Cloud Memory │  │ Secret Mgr  │ │
│  │   (Users)  │  │ (Sessions)  │  │    (Cache)   │  │   (Keys)    │ │
│  └────────────┘  └─────────────┘  └──────────────┘  └─────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
       │                                                          │
┌──────┴──────────────────────────────────────────────────────────┴─────┐
│                    External Services                                   │
│  ┌────────────┐  ┌─────────────┐  ┌──────────────┐                  │
│  │  SendGrid  │  │Google OAuth │  │ Cloud Tasks  │                  │
│  │   (Email)  │  │  Provider   │  │ (Async Jobs) │                  │
│  └────────────┘  └─────────────┘  └──────────────┘                  │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models

#### User Entity (Firestore)
```java
@Document(collection = "users")
public class User {
    @Id
    private String userId;           // UUID v4
    private String email;           // Encrypted at rest
    private String passwordHash;    // Argon2id hash
    private String displayName;
    private AuthProvider authProvider; // EMAIL, GOOGLE, etc.
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
    private UserProfile profile;    // Nested document
    private SecuritySettings security; // MFA settings, etc.
    private List<String> linkedProviders; // For account linking
}
```

#### Session Entity (Cloud SQL)
```sql
CREATE TABLE sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    refresh_token_hash VARCHAR(256) NOT NULL,
    device_info JSONB,
    ip_address INET,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_accessed TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_refresh_token ON sessions(refresh_token_hash);
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at) WHERE revoked_at IS NULL;
```

#### Permissions Entity (Firestore)
```java
@Document(collection = "permissions")
public class Permission {
    @Id
    private String permissionId;
    private String resourceId;      // ID of the resource
    private String resourceType;    // world, character, etc.
    private String grantedTo;       // User ID
    private String grantedBy;       // User ID who granted
    private PermissionLevel level;  // OWNER, EDITOR, VIEWER
    private Instant grantedAt;
    private Instant expiresAt;      // Optional expiration
    private Map<String, Boolean> customPermissions; // Future expansion
}
```

### API Design

#### Authentication Endpoints
```yaml
POST /api/auth/signup
  Request:
    email: string
    password: string
    displayName: string
    acceptedTerms: boolean
  Response:
    userId: string
    accessToken: string
    refreshToken: string
    expiresIn: number

POST /api/auth/login
  Request:
    email: string
    password: string
    rememberMe: boolean
  Response:
    userId: string
    accessToken: string
    refreshToken: string
    expiresIn: number

POST /api/auth/google
  Request:
    idToken: string
  Response:
    userId: string
    accessToken: string
    refreshToken: string
    isNewUser: boolean

POST /api/auth/refresh
  Request:
    refreshToken: string
  Response:
    accessToken: string
    refreshToken: string
    expiresIn: number

POST /api/auth/logout
  Request:
    refreshToken: string
    logoutAll: boolean
  Response:
    success: boolean

POST /api/auth/verify-email
  Request:
    token: string
  Response:
    success: boolean
    email: string

POST /api/auth/reset-password
  Request:
    email: string
  Response:
    success: boolean

POST /api/auth/confirm-reset
  Request:
    token: string
    newPassword: string
  Response:
    success: boolean
```

#### Permission Endpoints
```yaml
GET /api/permissions/check
  Query:
    resourceId: string
    action: string
  Response:
    allowed: boolean
    level: string

POST /api/permissions/grant
  Request:
    resourceId: string
    userId: string
    level: string
    expiresAt?: string
  Response:
    permissionId: string
    success: boolean

DELETE /api/permissions/{permissionId}
  Response:
    success: boolean

GET /api/resources/{resourceId}/permissions
  Response:
    permissions: Permission[]
```

### Security Architecture

#### JWT Token Structure
```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "key-id"
  },
  "payload": {
    "sub": "user-id",
    "email": "user@example.com",
    "email_verified": true,
    "roles": ["user"],
    "permissions": ["read:own", "write:own"],
    "iat": 1234567890,
    "exp": 1234567890,
    "iss": "https://api.deusexmachina.com",
    "aud": "deusexmachina-client"
  }
}
```

#### Security Measures
1. **Password Security**
   - Argon2id hashing with salt (memory: 64MB, iterations: 3, parallelism: 4)
   - Password strength validation (zxcvbn library)
   - Breached password checking (HaveIBeenPwned API)
   - Rate limiting on password attempts (5 attempts per 15 minutes)

2. **Session Security**
   - Secure, httpOnly, sameSite cookies for web
   - Refresh token rotation on each use
   - Device fingerprinting for anomaly detection
   - Automatic session expiration after 30 days of inactivity

3. **OAuth Security**
   - PKCE flow for public clients
   - State parameter validation
   - Nonce validation for ID tokens
   - Strict redirect URI validation

4. **API Security**
   - Rate limiting per IP and per user
   - CORS configuration with strict origins
   - Request signing for sensitive operations
   - Input validation and sanitization

## Implementation Plan

### Phase 1: Core Authentication (Week 1-2)
1. **Setup Infrastructure**
   - Create Firestore collections and Cloud SQL schema
   - Configure Secret Manager for JWT keys
   - Set up Cloud Memorystore for session caching
   - Deploy base Cloud Functions

2. **Implement Basic Auth Flow**
   - Email/password signup with Argon2id
   - Login with JWT generation
   - Email verification system
   - Password reset flow

3. **Session Management**
   - JWT token generation and validation
   - Refresh token implementation
   - Session storage in Cloud SQL
   - Cache layer with Memorystore

### Phase 2: OAuth Integration (Week 3)
1. **Google OAuth Setup**
   - Configure Google Cloud Identity Platform
   - Implement OAuth 2.0 flow with PKCE
   - Account linking for existing users
   - Profile data synchronization

2. **Security Enhancements**
   - Implement rate limiting with Cloud Armor
   - Add request validation middleware
   - Set up anomaly detection rules
   - Configure security headers

### Phase 3: Authorization System (Week 4)
1. **RBAC Implementation**
   - Define role hierarchy (Owner > Editor > Viewer)
   - Implement permission checking middleware
   - Create permission granting/revoking APIs
   - Add permission inheritance logic

2. **Advanced Features**
   - Time-limited permissions
   - Custom permission sets
   - Bulk permission management
   - Permission templates

### Phase 4: MFA & Account Management (Week 5)
1. **Multi-Factor Authentication**
   - TOTP implementation with QR codes
   - Backup codes generation
   - Device trust management
   - Recovery flow

2. **Account Dashboard**
   - Active sessions management
   - Security activity log
   - Privacy settings
   - Data export (GDPR)

### Phase 5: Testing & Monitoring (Week 6)
1. **Comprehensive Testing**
   - Unit tests with 90% coverage
   - Integration tests for all flows
   - Load testing (10K RPS target)
   - Security penetration testing

2. **Monitoring Setup**
   - Cloud Monitoring dashboards
   - Custom metrics for auth performance
   - Alert policies for failures
   - Audit logging with Cloud Logging

## Testing Strategy

### Unit Testing
```java
@Test
public void testPasswordHashing() {
    String password = "SecurePassword123!";
    String hash = authService.hashPassword(password);
    
    assertTrue(authService.verifyPassword(password, hash));
    assertFalse(authService.verifyPassword("wrongpassword", hash));
    assertTrue(hash.startsWith("$argon2id$"));
}

@Test
public void testJWTGeneration() {
    User user = createTestUser();
    String token = jwtService.generateAccessToken(user);
    
    Claims claims = jwtService.validateToken(token);
    assertEquals(user.getUserId(), claims.getSubject());
    assertEquals(user.getEmail(), claims.get("email"));
}
```

### Integration Testing
```java
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {
    @Test
    public void testCompleteSignupFlow() throws Exception {
        // 1. Signup
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@example.com",
                        "password": "SecurePass123!",
                        "displayName": "Test User"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists());
        
        // 2. Verify email verification was sent
        verify(emailService).sendVerificationEmail(any());
        
        // 3. Complete verification
        mockMvc.perform(post("/api/auth/verify-email")
                .param("token", "verification-token"))
                .andExpect(status().isOk());
    }
}
```

### Load Testing
```javascript
// k6 load test script
import http from 'k6/http';
import { check } from 'k6';

export let options = {
    stages: [
        { duration: '2m', target: 100 },
        { duration: '5m', target: 1000 },
        { duration: '2m', target: 10000 },
        { duration: '5m', target: 10000 },
        { duration: '2m', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'],
        http_req_failed: ['rate<0.1'],
    },
};

export default function() {
    let response = http.post('https://api.deusexmachina.com/api/auth/login', {
        email: 'test@example.com',
        password: 'password123',
    });
    
    check(response, {
        'status is 200': (r) => r.status === 200,
        'has access token': (r) => r.json('accessToken') !== '',
    });
}
```

### Security Testing
- OWASP ZAP automated scanning
- Manual penetration testing for:
  - SQL injection
  - JWT token manipulation
  - Session fixation
  - CSRF attacks
  - Timing attacks on login
- Dependency vulnerability scanning with Snyk

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Credential stuffing attacks | High | High | Implement rate limiting, CAPTCHA after failures, breach detection |
| JWT token theft | High | Medium | Short expiration (15min), refresh rotation, device binding |
| Database breach | Critical | Low | Encryption at rest, field-level encryption for PII, access controls |
| OAuth provider outage | Medium | Low | Graceful degradation, cached provider configs, email fallback |
| Email delivery failures | Medium | Medium | Multiple email providers, retry with backoff, SMS fallback |
| Session hijacking | High | Medium | Device fingerprinting, IP validation, anomaly detection |
| Brute force attacks | Medium | High | Account lockout, progressive delays, IP blocking |
| GDPR non-compliance | High | Low | Data minimization, clear consent, automated deletion |
| Performance degradation | High | Medium | Caching strategy, database indexing, horizontal scaling |
| Key rotation complexity | Medium | Low | Automated rotation, grace period, key versioning |

## Performance Optimization

### Caching Strategy
1. **User Data Cache** (Memorystore)
   - Cache user profile data for 5 minutes
   - Invalidate on user update
   - Key pattern: `user:{userId}`

2. **Permission Cache**
   - Cache permission checks for 1 minute
   - Key pattern: `perm:{userId}:{resourceId}:{action}`
   - Invalidate on permission change

3. **Session Cache**
   - Cache active sessions for duration
   - Update on each request
   - Key pattern: `session:{sessionId}`

### Database Optimization
```sql
-- Optimized indexes for common queries
CREATE INDEX idx_users_email_provider ON users(email, auth_provider);
CREATE INDEX idx_permissions_user_resource ON permissions(granted_to, resource_id);
CREATE INDEX idx_sessions_user_active ON sessions(user_id, expires_at) 
    WHERE revoked_at IS NULL;

-- Partitioning for sessions table
CREATE TABLE sessions_2024_q1 PARTITION OF sessions
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
```

### Cloud Function Optimization
- Minimum instances: 1 for auth functions (reduce cold starts)
- Maximum instances: 100 (auto-scale based on load)
- Memory allocation: 512MB for auth, 256MB for permission checks
- Concurrent requests: 80 per instance

## Monitoring and Observability

### Key Metrics
1. **Authentication Metrics**
   - Login success rate (target: >99%)
   - Signup completion rate (target: >85%)
   - Average authentication time (target: <200ms)
   - Failed login attempts rate

2. **Session Metrics**
   - Active sessions count
   - Session duration distribution
   - Refresh token usage rate
   - Session timeout rate

3. **Security Metrics**
   - Blocked requests by WAF
   - Suspicious login attempts
   - MFA adoption rate
   - Password reset requests

### Logging Strategy
```java
// Structured logging for auth events
logger.info("auth.login", Map.of(
    "userId", user.getUserId(),
    "method", "password",
    "ip", request.getRemoteAddr(),
    "userAgent", request.getHeader("User-Agent"),
    "success", true,
    "duration", duration
));

// Security event logging
logger.warn("auth.suspicious_activity", Map.of(
    "type", "multiple_failed_logins",
    "email", email,
    "ip", request.getRemoteAddr(),
    "attempts", attemptCount
));
```

### Alerting Rules
- Login failure rate > 20% for 5 minutes
- Average auth response time > 500ms
- Session creation failures > 1%
- Email delivery failures > 5%
- Any critical security events

## Cost Analysis

### Monthly Cost Estimate (100K MAU)
| Service | Usage | Cost |
|---------|-------|------|
| Cloud Functions | 10M invocations | $40 |
| Firestore | 5M reads, 1M writes | $35 |
| Cloud SQL | db-f1-micro, 10GB | $50 |
| Memorystore | 1GB Redis | $35 |
| Secret Manager | 10K operations | $3 |
| Cloud Logging | 50GB ingestion | $25 |
| Load Balancer | 1 forwarding rule | $18 |
| Cloud Armor | 10M requests | $70 |
| SendGrid | 100K emails | $50 |
| **Total** | | **$326** |

### Cost Optimization Strategies
1. Use Cloud Function connection pooling
2. Implement aggressive caching
3. Archive old session data to Cloud Storage
4. Use committed use discounts for stable workloads
5. Optimize Firestore queries to reduce reads

## Migration Strategy

### Data Migration
1. Export existing user data
2. Transform to new schema
3. Bulk import with validation
4. Verify data integrity
5. Run parallel systems for validation

### Zero-Downtime Deployment
1. Deploy new auth system alongside existing
2. Implement feature flags for gradual rollout
3. Route percentage of traffic to new system
4. Monitor metrics and error rates
5. Complete migration after validation period

## Future Enhancements

### Phase 2 Features (Q2)
- Apple Sign In integration
- Discord OAuth for gaming community
- WebAuthn/Passkeys support
- Risk-based authentication
- Geographic access restrictions

### Phase 3 Features (Q3)
- Enterprise SSO (SAML 2.0)
- Team workspaces
- Advanced audit logs
- Compliance certifications (SOC 2)
- Custom authentication providers

## Appendix: Configuration Examples

### Terraform Module Usage
```hcl
module "auth_system" {
  source = "../../modules/auth"
  
  project_id = var.project_id
  region     = var.region
  
  # Firestore configuration
  firestore_database = "deusexmachina-auth"
  
  # Cloud SQL configuration
  sql_tier = "db-f1-micro"
  sql_database_name = "auth_db"
  
  # Redis configuration
  redis_tier = "BASIC"
  redis_memory_size_gb = 1
  
  # Email provider
  sendgrid_api_key = var.sendgrid_api_key
  
  # OAuth providers
  google_client_id = var.google_oauth_client_id
  google_client_secret = var.google_oauth_client_secret
}
```

### Environment Variables
```bash
# JWT Configuration
JWT_PRIVATE_KEY_PATH=/secrets/jwt-private.pem
JWT_PUBLIC_KEY_PATH=/secrets/jwt-public.pem
JWT_ISSUER=https://api.deusexmachina.com
JWT_AUDIENCE=deusexmachina-client
JWT_ACCESS_TOKEN_EXPIRY=15m
JWT_REFRESH_TOKEN_EXPIRY=30d

# Database Configuration
FIRESTORE_PROJECT_ID=deusexmachina-prod
CLOUD_SQL_CONNECTION_NAME=project:region:instance
REDIS_HOST=10.0.0.5
REDIS_PORT=6379

# Security Configuration
ARGON2_MEMORY_COST=65536
ARGON2_TIME_COST=3
ARGON2_PARALLELISM=4
RATE_LIMIT_LOGIN_ATTEMPTS=5
RATE_LIMIT_WINDOW_MINUTES=15

# OAuth Configuration
GOOGLE_OAUTH_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=xxx
OAUTH_REDIRECT_URL=https://app.deusexmachina.com/auth/callback
```

This technical specification provides a comprehensive implementation plan for the authentication and authorization system that meets all requirements while leveraging the existing GCP infrastructure and maintaining high security standards.