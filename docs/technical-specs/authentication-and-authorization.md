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

## Frontend Implementation (TDD Approach)

### Overview
This section defines the frontend implementation for the authentication system using Test-Driven Development (TDD) principles. We'll build React components with TypeScript, following the Red-Green-Refactor cycle and maintaining >80% test coverage.

### Authentication Pages Architecture

#### Route Structure
```typescript
// apps/frontend/web-app/src/routes/auth.routes.tsx
const authRoutes = [
  { path: '/signup', element: <SignupPage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/verify-email', element: <VerifyEmailPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  { path: '/account', element: <AccountPage />, protected: true },
];
```

### Signup Implementation (TDD)

#### Step 1: Test Specifications (Red Phase)

##### SignupPage Component Tests
```typescript
// apps/frontend/web-app/src/pages/auth/SignupPage.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SignupPage } from './SignupPage';
import { AuthProvider } from '../../contexts/AuthContext';
import { BrowserRouter } from 'react-router-dom';

const renderWithProviders = (component: React.ReactNode) => {
  return render(
    <BrowserRouter>
      <AuthProvider>
        {component}
      </AuthProvider>
    </BrowserRouter>
  );
};

describe('SignupPage', () => {
  it('should render signup form with all required fields', () => {
    renderWithProviders(<SignupPage />);
    
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/display name/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign up/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /continue with google/i })).toBeInTheDocument();
  });

  it('should show real-time email validation', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignupPage />);
    
    const emailInput = screen.getByLabelText(/email address/i);
    
    // Invalid email
    await user.type(emailInput, 'invalid-email');
    await user.tab();
    expect(screen.getByText(/please enter a valid email/i)).toBeInTheDocument();
    
    // Valid email
    await user.clear(emailInput);
    await user.type(emailInput, 'user@example.com');
    await user.tab();
    expect(screen.queryByText(/please enter a valid email/i)).not.toBeInTheDocument();
  });

  it('should show password strength meter', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignupPage />);
    
    const passwordInput = screen.getByLabelText(/^password$/i);
    
    // Weak password
    await user.type(passwordInput, 'weak');
    expect(screen.getByText(/weak password/i)).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '25');
    
    // Strong password
    await user.clear(passwordInput);
    await user.type(passwordInput, 'StrongP@ssw0rd123!');
    expect(screen.getByText(/strong password/i)).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  });

  it('should validate password confirmation matches', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignupPage />);
    
    const passwordInput = screen.getByLabelText(/^password$/i);
    const confirmInput = screen.getByLabelText(/confirm password/i);
    
    await user.type(passwordInput, 'StrongP@ssw0rd123!');
    await user.type(confirmInput, 'DifferentPassword');
    await user.tab();
    
    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
  });

  it('should handle successful signup', async () => {
    const mockSignup = vi.fn().mockResolvedValue({ success: true });
    vi.mock('../../services/authService', () => ({
      signup: mockSignup
    }));
    
    const user = userEvent.setup();
    renderWithProviders(<SignupPage />);
    
    await user.type(screen.getByLabelText(/email address/i), 'user@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'StrongP@ssw0rd123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'StrongP@ssw0rd123!');
    await user.type(screen.getByLabelText(/display name/i), 'Test User');
    await user.click(screen.getByRole('checkbox', { name: /accept terms/i }));
    
    await user.click(screen.getByRole('button', { name: /sign up/i }));
    
    await waitFor(() => {
      expect(mockSignup).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: 'StrongP@ssw0rd123!',
        displayName: 'Test User',
        acceptedTerms: true
      });
    });
    
    expect(screen.getByText(/verification email sent/i)).toBeInTheDocument();
  });

  it('should handle signup errors', async () => {
    const mockSignup = vi.fn().mockRejectedValue(new Error('Email already exists'));
    vi.mock('../../services/authService', () => ({
      signup: mockSignup
    }));
    
    const user = userEvent.setup();
    renderWithProviders(<SignupPage />);
    
    // Fill form
    await user.type(screen.getByLabelText(/email address/i), 'existing@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'StrongP@ssw0rd123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'StrongP@ssw0rd123!');
    await user.type(screen.getByLabelText(/display name/i), 'Test User');
    await user.click(screen.getByRole('checkbox', { name: /accept terms/i }));
    
    await user.click(screen.getByRole('button', { name: /sign up/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/email already exists/i)).toBeInTheDocument();
    });
  });

  it('should disable submit button during submission', async () => {
    const mockSignup = vi.fn().mockImplementation(() => 
      new Promise(resolve => setTimeout(resolve, 1000))
    );
    vi.mock('../../services/authService', () => ({
      signup: mockSignup
    }));
    
    const user = userEvent.setup();
    renderWithProviders(<SignupPage />);
    
    const submitButton = screen.getByRole('button', { name: /sign up/i });
    
    // Fill form
    await user.type(screen.getByLabelText(/email address/i), 'user@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'StrongP@ssw0rd123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'StrongP@ssw0rd123!');
    await user.type(screen.getByLabelText(/display name/i), 'Test User');
    await user.click(screen.getByRole('checkbox', { name: /accept terms/i }));
    
    await user.click(submitButton);
    
    expect(submitButton).toBeDisabled();
    expect(submitButton).toHaveTextContent(/signing up/i);
  });

  it('should be accessible', async () => {
    const { container } = renderWithProviders(<SignupPage />);
    
    // Check for proper heading hierarchy
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/sign up/i);
    
    // Check for form landmarks
    expect(screen.getByRole('form')).toHaveAccessibleName(/sign up form/i);
    
    // Check for proper labeling
    const inputs = screen.getAllByRole('textbox');
    inputs.forEach(input => {
      expect(input).toHaveAccessibleName();
    });
    
    // Run axe accessibility tests
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
```

##### PasswordStrengthMeter Component Tests
```typescript
// packages/ui-components/src/components/PasswordStrengthMeter/PasswordStrengthMeter.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PasswordStrengthMeter } from './PasswordStrengthMeter';

describe('PasswordStrengthMeter', () => {
  it('should show weak strength for simple passwords', () => {
    render(<PasswordStrengthMeter password="123456" />);
    
    expect(screen.getByText(/weak/i)).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '25');
    expect(screen.getByRole('progressbar')).toHaveClass('bg-red-500');
  });

  it('should show medium strength for moderate passwords', () => {
    render(<PasswordStrengthMeter password="Password123" />);
    
    expect(screen.getByText(/medium/i)).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '50');
    expect(screen.getByRole('progressbar')).toHaveClass('bg-yellow-500');
  });

  it('should show strong strength for complex passwords', () => {
    render(<PasswordStrengthMeter password="MyStr0ng!P@ssw0rd" />);
    
    expect(screen.getByText(/strong/i)).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
    expect(screen.getByRole('progressbar')).toHaveClass('bg-green-500');
  });

  it('should list missing requirements', () => {
    render(<PasswordStrengthMeter password="weak" showRequirements />);
    
    expect(screen.getByText(/at least 8 characters/i)).toHaveClass('text-red-600');
    expect(screen.getByText(/uppercase letter/i)).toHaveClass('text-red-600');
    expect(screen.getByText(/number/i)).toHaveClass('text-red-600');
    expect(screen.getByText(/special character/i)).toHaveClass('text-red-600');
  });

  it('should check off met requirements', () => {
    render(<PasswordStrengthMeter password="MyStr0ng!P@ssw0rd123" showRequirements />);
    
    expect(screen.getByText(/at least 8 characters/i)).toHaveClass('text-green-600');
    expect(screen.getByText(/uppercase letter/i)).toHaveClass('text-green-600');
    expect(screen.getByText(/lowercase letter/i)).toHaveClass('text-green-600');
    expect(screen.getByText(/number/i)).toHaveClass('text-green-600');
    expect(screen.getByText(/special character/i)).toHaveClass('text-green-600');
  });
});
```

##### API Integration Tests
```typescript
// apps/frontend/web-app/src/services/authService.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { signup } from './authService';
import { apiClient } from '../utils/apiClient';

vi.mock('../utils/apiClient');

describe('authService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('signup', () => {
    it('should call API with correct payload', async () => {
      const mockResponse = {
        userId: '123',
        accessToken: 'token',
        refreshToken: 'refresh',
        expiresIn: 900
      };
      
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockResponse });
      
      const result = await signup({
        email: 'user@example.com',
        password: 'password123',
        displayName: 'Test User',
        acceptedTerms: true
      });
      
      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/signup', {
        email: 'user@example.com',
        password: 'password123',
        displayName: 'Test User',
        acceptedTerms: true
      });
      
      expect(result).toEqual(mockResponse);
    });

    it('should handle validation errors', async () => {
      const mockError = {
        response: {
          status: 400,
          data: {
            error: 'Validation failed',
            details: {
              email: 'Email already exists'
            }
          }
        }
      };
      
      vi.mocked(apiClient.post).mockRejectedValue(mockError);
      
      await expect(signup({
        email: 'existing@example.com',
        password: 'password123',
        displayName: 'Test User',
        acceptedTerms: true
      })).rejects.toThrow('Email already exists');
    });

    it('should handle network errors', async () => {
      vi.mocked(apiClient.post).mockRejectedValue(new Error('Network error'));
      
      await expect(signup({
        email: 'user@example.com',
        password: 'password123',
        displayName: 'Test User',
        acceptedTerms: true
      })).rejects.toThrow('Network error');
    });
  });
});
```

#### Step 2: Implementation (Green Phase)

##### SignupPage Component
```typescript
// apps/frontend/web-app/src/pages/auth/SignupPage.tsx
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button, Input, Alert } from '@deusexmachina/ui-components';
import { PasswordStrengthMeter } from '@deusexmachina/ui-components';
import { GoogleOAuthButton } from '../../components/auth/GoogleOAuthButton';
import { useAuth } from '../../contexts/AuthContext';
import { signup } from '../../services/authService';

const signupSchema = z.object({
  email: z.string().email('Please enter a valid email'),
  password: z.string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain an uppercase letter')
    .regex(/[0-9]/, 'Password must contain a number')
    .regex(/[!@#$%^&*]/, 'Password must contain a special character'),
  confirmPassword: z.string(),
  displayName: z.string().min(2, 'Display name must be at least 2 characters'),
  acceptedTerms: z.boolean().refine(val => val === true, {
    message: 'You must accept the terms and conditions'
  })
}).refine(data => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword']
});

type SignupFormData = z.infer<typeof signupSchema>;

export const SignupPage: React.FC = () => {
  const navigate = useNavigate();
  const { setAuth } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [showSuccess, setShowSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors }
  } = useForm<SignupFormData>({
    resolver: zodResolver(signupSchema)
  });

  const password = watch('password', '');

  const onSubmit = async (data: SignupFormData) => {
    try {
      setIsSubmitting(true);
      setApiError(null);

      const response = await signup({
        email: data.email,
        password: data.password,
        displayName: data.displayName,
        acceptedTerms: data.acceptedTerms
      });

      // Store auth tokens
      setAuth({
        user: {
          id: response.userId,
          email: data.email,
          displayName: data.displayName,
          emailVerified: false
        },
        accessToken: response.accessToken,
        refreshToken: response.refreshToken
      });

      setShowSuccess(true);
      
      // Redirect to dashboard after 3 seconds
      setTimeout(() => {
        navigate('/dashboard');
      }, 3000);
    } catch (error: any) {
      setApiError(error.message || 'An error occurred during signup');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (showSuccess) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full space-y-8">
          <Alert
            variant="success"
            title="Account created successfully!"
            description="We've sent a verification email to your inbox. Please check your email to verify your account."
          />
          <p className="text-center text-sm text-gray-600">
            Redirecting to dashboard...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h1 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Create your account
          </h1>
          <p className="mt-2 text-center text-sm text-gray-600">
            Or{' '}
            <Link to="/login" className="font-medium text-blue-600 hover:text-blue-500">
              sign in to your existing account
            </Link>
          </p>
        </div>

        <form
          className="mt-8 space-y-6"
          onSubmit={handleSubmit(onSubmit)}
          aria-label="Sign up form"
        >
          {apiError && (
            <Alert variant="error" title="Signup failed" description={apiError} />
          )}

          <div className="space-y-4">
            <Input
              {...register('email')}
              type="email"
              label="Email address"
              autoComplete="email"
              error={errors.email?.message}
              disabled={isSubmitting}
            />

            <Input
              {...register('displayName')}
              type="text"
              label="Display name"
              autoComplete="name"
              error={errors.displayName?.message}
              disabled={isSubmitting}
            />

            <div>
              <Input
                {...register('password')}
                type="password"
                label="Password"
                autoComplete="new-password"
                error={errors.password?.message}
                disabled={isSubmitting}
              />
              {password && (
                <div className="mt-2">
                  <PasswordStrengthMeter password={password} showRequirements />
                </div>
              )}
            </div>

            <Input
              {...register('confirmPassword')}
              type="password"
              label="Confirm password"
              autoComplete="new-password"
              error={errors.confirmPassword?.message}
              disabled={isSubmitting}
            />

            <div className="flex items-start">
              <input
                {...register('acceptedTerms')}
                id="acceptedTerms"
                type="checkbox"
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                disabled={isSubmitting}
              />
              <label htmlFor="acceptedTerms" className="ml-2 block text-sm text-gray-900">
                I accept the{' '}
                <Link to="/terms" className="text-blue-600 hover:text-blue-500">
                  Terms and Conditions
                </Link>
                {' '}and{' '}
                <Link to="/privacy" className="text-blue-600 hover:text-blue-500">
                  Privacy Policy
                </Link>
              </label>
            </div>
            {errors.acceptedTerms && (
              <p className="text-sm text-red-600">{errors.acceptedTerms.message}</p>
            )}
          </div>

          <div className="space-y-4">
            <Button
              type="submit"
              fullWidth
              loading={isSubmitting}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Signing up...' : 'Sign up'}
            </Button>

            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-300" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-gray-50 text-gray-500">Or continue with</span>
              </div>
            </div>

            <GoogleOAuthButton
              disabled={isSubmitting}
              onSuccess={(response) => {
                setAuth(response);
                navigate('/dashboard');
              }}
              onError={(error) => {
                setApiError(error.message);
              }}
            />
          </div>
        </form>
      </div>
    </div>
  );
};
```

##### PasswordStrengthMeter Component
```typescript
// packages/ui-components/src/components/PasswordStrengthMeter/PasswordStrengthMeter.tsx
import React from 'react';
import { cn } from '../../utils/cn';

interface PasswordStrengthMeterProps {
  password: string;
  showRequirements?: boolean;
  className?: string;
}

interface PasswordRequirement {
  label: string;
  test: (password: string) => boolean;
}

const requirements: PasswordRequirement[] = [
  { label: 'At least 8 characters', test: (p) => p.length >= 8 },
  { label: 'Uppercase letter', test: (p) => /[A-Z]/.test(p) },
  { label: 'Lowercase letter', test: (p) => /[a-z]/.test(p) },
  { label: 'Number', test: (p) => /[0-9]/.test(p) },
  { label: 'Special character', test: (p) => /[!@#$%^&*]/.test(p) },
];

export const PasswordStrengthMeter: React.FC<PasswordStrengthMeterProps> = ({
  password,
  showRequirements = false,
  className
}) => {
  const calculateStrength = (): { score: number; label: string; color: string } => {
    if (!password) return { score: 0, label: 'No password', color: 'bg-gray-300' };
    
    const passedRequirements = requirements.filter(req => req.test(password)).length;
    
    if (passedRequirements <= 2) return { score: 25, label: 'Weak', color: 'bg-red-500' };
    if (passedRequirements <= 3) return { score: 50, label: 'Medium', color: 'bg-yellow-500' };
    if (passedRequirements <= 4) return { score: 75, label: 'Good', color: 'bg-blue-500' };
    return { score: 100, label: 'Strong', color: 'bg-green-500' };
  };

  const { score, label, color } = calculateStrength();

  return (
    <div className={cn('space-y-2', className)}>
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-700">Password strength</span>
        <span className={cn(
          'text-sm font-medium',
          score <= 25 && 'text-red-600',
          score > 25 && score <= 50 && 'text-yellow-600',
          score > 50 && score <= 75 && 'text-blue-600',
          score > 75 && 'text-green-600'
        )}>
          {label}
        </span>
      </div>
      
      <div className="w-full bg-gray-200 rounded-full h-2">
        <div
          className={cn('h-2 rounded-full transition-all duration-300', color)}
          style={{ width: `${score}%` }}
          role="progressbar"
          aria-valuenow={score}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`Password strength: ${label}`}
        />
      </div>

      {showRequirements && (
        <ul className="mt-2 space-y-1">
          {requirements.map((req, index) => {
            const passed = password ? req.test(password) : false;
            return (
              <li
                key={index}
                className={cn(
                  'flex items-center text-sm',
                  passed ? 'text-green-600' : 'text-red-600'
                )}
              >
                <svg
                  className="w-4 h-4 mr-2"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  {passed ? (
                    <path
                      fillRule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clipRule="evenodd"
                    />
                  ) : (
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                      clipRule="evenodd"
                    />
                  )}
                </svg>
                {req.label}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};
```

##### Auth Service Implementation
```typescript
// apps/frontend/web-app/src/services/authService.ts
import { apiClient } from '../utils/apiClient';
import { AuthResponse, SignupRequest } from '../types/auth';

export const signup = async (data: SignupRequest): Promise<AuthResponse> => {
  try {
    const response = await apiClient.post<AuthResponse>('/api/auth/signup', data);
    return response.data;
  } catch (error: any) {
    if (error.response?.status === 400 && error.response?.data?.details?.email) {
      throw new Error(error.response.data.details.email);
    }
    throw error;
  }
};
```

#### Step 3: Refactor (Refactor Phase)

##### Extracting Form Field Component
```typescript
// packages/ui-components/src/components/FormField/FormField.tsx
import React from 'react';
import { UseFormRegisterReturn } from 'react-hook-form';
import { Input, InputProps } from '../Input/Input';

interface FormFieldProps extends Omit<InputProps, 'name'> {
  registration: UseFormRegisterReturn;
  error?: string;
}

export const FormField: React.FC<FormFieldProps> = ({
  registration,
  error,
  ...inputProps
}) => {
  return (
    <Input
      {...registration}
      {...inputProps}
      error={error}
      aria-invalid={!!error}
      aria-describedby={error ? `${registration.name}-error` : undefined}
    />
  );
};
```

##### Extracting Validation Rules
```typescript
// apps/frontend/web-app/src/utils/validation.ts
import { z } from 'zod';

export const emailSchema = z.string().email('Please enter a valid email');

export const passwordSchema = z.string()
  .min(8, 'Password must be at least 8 characters')
  .regex(/[A-Z]/, 'Password must contain an uppercase letter')
  .regex(/[0-9]/, 'Password must contain a number')
  .regex(/[!@#$%^&*]/, 'Password must contain a special character');

export const displayNameSchema = z.string()
  .min(2, 'Display name must be at least 2 characters')
  .max(50, 'Display name must be less than 50 characters');
```

### Shared Auth Components

#### AuthForm Wrapper
```typescript
// apps/frontend/web-app/src/components/auth/AuthForm.tsx
import React from 'react';

interface AuthFormProps {
  title: string;
  subtitle?: React.ReactNode;
  onSubmit: (e: React.FormEvent) => void;
  children: React.ReactNode;
  error?: string | null;
}

export const AuthForm: React.FC<AuthFormProps> = ({
  title,
  subtitle,
  onSubmit,
  children,
  error
}) => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h1 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            {title}
          </h1>
          {subtitle && (
            <p className="mt-2 text-center text-sm text-gray-600">
              {subtitle}
            </p>
          )}
        </div>

        <form
          className="mt-8 space-y-6"
          onSubmit={onSubmit}
          aria-label={`${title} form`}
        >
          {error && (
            <Alert variant="error" title="Error" description={error} />
          )}
          {children}
        </form>
      </div>
    </div>
  );
};
```

#### AuthGuard Route Protection
```typescript
// apps/frontend/web-app/src/components/auth/AuthGuard.tsx
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Spinner } from '@deusexmachina/ui-components';

interface AuthGuardProps {
  children: React.ReactNode;
  requireEmailVerification?: boolean;
  requiredRole?: string;
}

export const AuthGuard: React.FC<AuthGuardProps> = ({
  children,
  requireEmailVerification = false,
  requiredRole
}) => {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requireEmailVerification && !user.emailVerified) {
    return <Navigate to="/verify-email" replace />;
  }

  if (requiredRole && !user.roles?.includes(requiredRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};
```

### E2E Testing Strategy

#### Cypress E2E Tests
```typescript
// apps/frontend/web-app/cypress/e2e/auth/signup.cy.ts
describe('Signup Flow', () => {
  beforeEach(() => {
    cy.visit('/signup');
  });

  it('should complete signup flow successfully', () => {
    // Generate unique email
    const email = `test${Date.now()}@example.com`;
    
    // Fill out form
    cy.findByLabelText(/email address/i).type(email);
    cy.findByLabelText(/display name/i).type('Test User');
    cy.findByLabelText(/^password$/i).type('TestP@ssw0rd123!');
    cy.findByLabelText(/confirm password/i).type('TestP@ssw0rd123!');
    
    // Check password strength meter
    cy.findByText(/strong password/i).should('be.visible');
    cy.findByRole('progressbar').should('have.attr', 'aria-valuenow', '100');
    
    // Accept terms
    cy.findByRole('checkbox', { name: /accept terms/i }).check();
    
    // Submit form
    cy.findByRole('button', { name: /sign up/i }).click();
    
    // Verify success message
    cy.findByText(/verification email sent/i).should('be.visible');
    
    // Should redirect to dashboard
    cy.url().should('include', '/dashboard', { timeout: 4000 });
  });

  it('should handle existing email error', () => {
    cy.intercept('POST', '/api/auth/signup', {
      statusCode: 400,
      body: {
        error: 'Validation failed',
        details: {
          email: 'Email already exists'
        }
      }
    });
    
    cy.findByLabelText(/email address/i).type('existing@example.com');
    cy.findByLabelText(/display name/i).type('Test User');
    cy.findByLabelText(/^password$/i).type('TestP@ssw0rd123!');
    cy.findByLabelText(/confirm password/i).type('TestP@ssw0rd123!');
    cy.findByRole('checkbox', { name: /accept terms/i }).check();
    cy.findByRole('button', { name: /sign up/i }).click();
    
    cy.findByText(/email already exists/i).should('be.visible');
  });

  it('should validate form fields', () => {
    // Try to submit empty form
    cy.findByRole('button', { name: /sign up/i }).click();
    
    // Check for validation errors
    cy.findByText(/please enter a valid email/i).should('be.visible');
    cy.findByText(/password must be at least 8 characters/i).should('be.visible');
    cy.findByText(/display name must be at least 2 characters/i).should('be.visible');
    cy.findByText(/you must accept the terms/i).should('be.visible');
  });

  it('should handle Google OAuth', () => {
    cy.findByRole('button', { name: /continue with google/i }).click();
    
    // Would redirect to Google OAuth page
    cy.origin('https://accounts.google.com', () => {
      // Mock Google OAuth flow
    });
  });
});
```

### Performance Optimization

#### Code Splitting
```typescript
// apps/frontend/web-app/src/routes/index.tsx
import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Spinner } from '@deusexmachina/ui-components';

// Lazy load auth pages
const SignupPage = lazy(() => import('../pages/auth/SignupPage'));
const LoginPage = lazy(() => import('../pages/auth/LoginPage'));
const VerifyEmailPage = lazy(() => import('../pages/auth/VerifyEmailPage'));
const ResetPasswordPage = lazy(() => import('../pages/auth/ResetPasswordPage'));
const AccountPage = lazy(() => import('../pages/auth/AccountPage'));

export const AppRoutes = () => {
  return (
    <Suspense fallback={<Spinner fullScreen />}>
      <Routes>
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route
          path="/account"
          element={
            <AuthGuard requireEmailVerification>
              <AccountPage />
            </AuthGuard>
          }
        />
      </Routes>
    </Suspense>
  );
};
```

### Monitoring & Analytics

#### Error Tracking
```typescript
// apps/frontend/web-app/src/utils/errorTracking.ts
import * as Sentry from '@sentry/react';

export const trackAuthError = (error: Error, context: Record<string, any>) => {
  Sentry.captureException(error, {
    tags: {
      component: 'auth',
      ...context
    }
  });
};

// Usage in signup
catch (error: any) {
  trackAuthError(error, {
    action: 'signup',
    email: data.email // Hashed for privacy
  });
  setApiError(error.message);
}
```

#### Analytics Events
```typescript
// apps/frontend/web-app/src/utils/analytics.ts
import { analytics } from './analytics-client';

export const trackAuthEvent = (event: string, properties?: Record<string, any>) => {
  analytics.track(event, {
    ...properties,
    timestamp: new Date().toISOString()
  });
};

// Usage
trackAuthEvent('signup_started');
trackAuthEvent('signup_completed', { method: 'email' });
trackAuthEvent('signup_failed', { error: error.message });
```

## Production Deployment Configuration

### Overview
The authentication frontend is deployed as part of the web-app to the existing static hosting infrastructure. This section details the deployment configuration required for production.

### Reference Documentation
- **Frontend Static Hosting Spec**: `/docs/technical-specs/frontend-static-hosting-and-deployment.md`
- **CI/CD Pipeline**: `.github/workflows/main.yml`

### Deployment Architecture
```
┌─────────────────────┐
│   Static Bucket     │
├─────────────────────┤
│ /                   │ ← Landing Page (root)
│ /web-app/           │ ← Web Application
│ /web-app/signup     │ ← Signup Page (client-side route)
│ /web-app/login      │ ← Login Page (client-side route)
│ /web-app/verify     │ ← Email Verification (client-side route)
└─────────────────────┘
```

### Required Configuration

#### 1. Vite Configuration
```typescript
// apps/frontend/web-app/vite.config.ts
export default defineConfig({
  plugins: [react()],
  base: '/web-app/',  // REQUIRED: Set base path for subdirectory deployment
  // ... rest of config
});
```

#### 2. Router Configuration
```typescript
// apps/frontend/web-app/src/router.tsx
export const router = createBrowserRouter([
  // routes...
], {
  basename: '/web-app'  // REQUIRED: Match the deployment path
});
```

#### 3. 404 Handling for Client-Side Routing
```html
<!-- apps/frontend/web-app/public/404.html -->
<!DOCTYPE html>
<html lang="en">
  <head>
    <script>
      // Redirect 404s to index.html for client-side routing
      const path = window.location.pathname;
      const base = '/web-app';
      
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

#### 4. API Endpoint Configuration
```typescript
// apps/frontend/web-app/src/config/api.ts
const API_BASE_URL = import.meta.env.PROD 
  ? 'https://<region>-<project-id>.cloudfunctions.net'
  : 'http://localhost:8080';

export const API_ENDPOINTS = {
  auth: {
    signup: `${API_BASE_URL}/auth-function/signup`,
    login: `${API_BASE_URL}/auth-function/login`,
    verify: `${API_BASE_URL}/auth-function/verify-email`,
    refresh: `${API_BASE_URL}/auth-function/refresh`,
  }
};
```

### Deployment Process

#### Automatic Deployment
The CI/CD pipeline automatically deploys the web-app when changes are pushed to main:

1. **Build Phase**: 
   - Runs `pnpm build` in the web-app directory
   - Outputs to `apps/frontend/web-app/dist/`

2. **Deploy Phase**:
   - Uploads dist contents to `gs://<bucket>/web-app/`
   - Sets appropriate cache headers:
     - HTML: 5 minutes
     - JS/CSS: 1 year (with hash)
     - Images: 30 days

3. **CDN Invalidation**:
   - Automatically invalidates CDN cache after deployment

#### Manual Deployment (for testing)
```bash
# Build the app
cd apps/frontend/web-app
pnpm build

# Deploy to bucket (requires gcloud auth)
gsutil -m rsync -r -d dist/ gs://<bucket-name>/web-app/

# Invalidate CDN cache
gcloud compute url-maps invalidate-cdn-cache <url-map-name> --path "/web-app/*"
```

### Production URLs

After deployment, the auth pages are accessible at:
- **Base URL**: `https://<static-hosting-url>/web-app/`
- **Signup**: `https://<static-hosting-url>/web-app/signup`
- **Login**: `https://<static-hosting-url>/web-app/login`
- **Email Verification**: `https://<static-hosting-url>/web-app/verify-email`
- **Password Reset**: `https://<static-hosting-url>/web-app/reset-password`

### Security Headers
The static hosting module automatically applies security headers including:
- Strict-Transport-Security (HSTS)
- X-Content-Type-Options
- X-Frame-Options
- Content-Security-Policy (configured to allow Google OAuth)

### Performance Considerations

1. **Code Splitting**: Auth pages are lazy-loaded to reduce initial bundle size
2. **Asset Optimization**: All assets are hashed for long-term caching
3. **CDN Distribution**: Global edge caching for low latency
4. **Compression**: Automatic gzip/brotli compression

### Monitoring

1. **Deployment Status**: Check GitHub Actions for build/deploy status
2. **Uptime Monitoring**: Cloud Monitoring checks `/web-app/` availability
3. **Performance**: Lighthouse CI runs on deployment
4. **Error Tracking**: Client-side errors sent to error tracking service

### Rollback Procedure

If issues occur after deployment:
1. **Immediate**: Revert the commit and push to trigger redeploy
2. **Manual**: Use gsutil to sync previous build from backup
3. **CDN**: Invalidate cache after rollback

### Known Limitations

#### SPA Routing with Cloud Load Balancer
Google Cloud Load Balancer doesn't support automatic fallback routing for SPAs. Direct URLs like `/web-app/signup` will return 404 errors.

**Current Workarounds**:
1. Access the base URL (`/web-app/`) and use client-side navigation
2. Use hash-based routing (`/#/signup`)
3. Implement a Cloud Function for routing
4. Consider Firebase Hosting for better SPA support

**Manual Deployment** (if CI/CD fails):
```bash
# Build locally
cd apps/frontend/web-app
pnpm build

# Deploy to bucket
gsutil -m cp -r dist/* gs://<bucket-name>/web-app/

# Set cache headers
gsutil -m setmeta -h "Cache-Control:public, max-age=31536000" "gs://<bucket>/web-app/assets/*.js"
gsutil -m setmeta -h "Cache-Control:public, max-age=31536000" "gs://<bucket>/web-app/assets/*.css"
gsutil -m setmeta -h "Cache-Control:no-cache" "gs://<bucket>/web-app/*.html"
```

### Environment Variables

Production environment variables are set during build:
```bash
VITE_API_URL=https://<region>-<project-id>.cloudfunctions.net
VITE_GOOGLE_CLIENT_ID=<oauth-client-id>
VITE_SENTRY_DSN=<sentry-dsn>
```

This technical specification provides a comprehensive implementation plan for the authentication and authorization system that meets all requirements while leveraging the existing GCP infrastructure and maintaining high security standards.