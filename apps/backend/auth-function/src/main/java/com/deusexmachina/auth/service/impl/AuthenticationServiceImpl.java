package com.deusexmachina.auth.service.impl;

import com.deusexmachina.auth.domain.AuthProvider;
import com.deusexmachina.auth.domain.Session;
import com.deusexmachina.auth.domain.User;
import com.deusexmachina.auth.domain.UserSecuritySettings;
import com.deusexmachina.auth.dto.*;
import com.deusexmachina.auth.exception.AuthException;
import com.deusexmachina.auth.repository.SessionRepository;
import com.deusexmachina.auth.repository.UserRepository;
import com.deusexmachina.auth.service.*;
import com.deusexmachina.auth.util.TokenGenerator;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.cloud.Timestamp;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of AuthenticationService.
 * Orchestrates authentication flows using other services.
 * Follows Open/Closed Principle - open for extension via new auth providers.
 */
@Singleton
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final GoogleIdTokenVerifier googleVerifier;
    
    // Token storage for email verification and password reset
    private final Map<String, TokenInfo> verificationTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenInfo> resetTokens = new ConcurrentHashMap<>();
    
    @Inject
    public AuthenticationServiceImpl(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordService passwordService,
            TokenService tokenService,
            EmailService emailService,
            @Named("google.client.id") String googleClientId) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        
        // Initialize Google verifier
        this.googleVerifier = new GoogleIdTokenVerifier.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                new com.google.api.client.json.gson.GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }
    
    @Override
    public CompletableFuture<AuthResponse> register(RegisterRequest request) {
        return userRepository.existsByEmail(request.email())
                .thenCompose(exists -> {
                    if (exists) {
                        throw new AuthException("Email already registered");
                    }
                    
                    // Validate password strength
                    if (!passwordService.validatePasswordStrength(request.password())) {
                        throw new AuthException("Password does not meet strength requirements");
                    }
                    
                    // Check if password is breached
                    if (passwordService.isPasswordBreached(request.password())) {
                        throw new AuthException("Password has been found in data breaches");
                    }
                    
                    // Hash password
                    String passwordHash = passwordService.hashPassword(request.password());
                    
                    // Create user
                    User user = User.builder()
                            .userId(UUID.randomUUID().toString())
                            .email(request.email())
                            .passwordHash(passwordHash)
                            .displayName(request.displayName())
                            .authProvider(AuthProvider.EMAIL)
                            .emailVerified(false)
                            .securitySettings(new UserSecuritySettings())
                            .build();
                    
                    return userRepository.save(user);
                })
                .thenCompose(user -> {
                    // Generate verification token
                    String verificationToken = TokenGenerator.generateSecureToken();
                    verificationTokens.put(verificationToken, new TokenInfo(user.getUserId(), 
                            Instant.now().plus(24, ChronoUnit.HOURS)));
                    
                    // Send verification email
                    emailService.sendVerificationEmail(user.getEmail(), verificationToken)
                            .exceptionally(e -> {
                                logger.error("Failed to send verification email", e);
                                return null;
                            });
                    
                    // Create session and generate tokens
                    return createSessionAndGenerateTokens(user, null, false);
                });
    }
    
    @Override
    public CompletableFuture<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .thenCompose(optionalUser -> {
                    User user = optionalUser.orElseThrow(() -> 
                            new AuthException("Invalid credentials"));
                    
                    // Check if account is locked
                    if (user.getSecuritySettings().isLockedOut()) {
                        throw new AuthException("Account is temporarily locked");
                    }
                    
                    // Verify password
                    if (!user.hasPassword() || 
                        !passwordService.verifyPassword(request.password(), user.getPasswordHash())) {
                        
                        // Increment failed login attempts
                        handleFailedLogin(user);
                        throw new AuthException("Invalid credentials");
                    }
                    
                    // Reset failed login attempts on successful login
                    if (user.getSecuritySettings().getFailedLoginAttempts() > 0) {
                        UserSecuritySettings updatedSettings = user.getSecuritySettings().toBuilder()
                                .failedLoginAttempts(0)
                                .lockoutUntil(null)
                                .build();
                        
                        return userRepository.updateSecuritySettings(user.getUserId(), updatedSettings)
                                .thenCompose(updatedUser -> 
                                        createSessionAndGenerateTokens(updatedUser, null, 
                                                request.rememberMe()));
                    }
                    
                    return createSessionAndGenerateTokens(user, null, request.rememberMe());
                });
    }
    
    @Override
    public CompletableFuture<AuthResponse> googleLogin(GoogleLoginRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verify Google ID token
                GoogleIdToken idToken = googleVerifier.verify(request.idToken());
                if (idToken == null) {
                    throw new AuthException("Invalid Google ID token");
                }
                
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String googleId = payload.getSubject();
                
                return userRepository.findByEmail(email)
                        .thenCompose(optionalUser -> {
                            if (optionalUser.isPresent()) {
                                // Existing user - link Google account if not already linked
                                User user = optionalUser.get();
                                if (!user.isProviderLinked("google")) {
                                    List<String> linkedProviders = new ArrayList<>(user.getLinkedProviders());
                                    linkedProviders.add("google");
                                    
                                    User updatedUser = user.toBuilder()
                                            .linkedProviders(linkedProviders)
                                            .emailVerified(true) // Google emails are pre-verified
                                            .build();
                                    
                                    return userRepository.save(updatedUser);
                                }
                                return CompletableFuture.completedFuture(user);
                            } else {
                                // New user from Google
                                User newUser = User.builder()
                                        .userId(UUID.randomUUID().toString())
                                        .email(email)
                                        .displayName((String) payload.get("name"))
                                        .authProvider(AuthProvider.GOOGLE)
                                        .emailVerified(true)
                                        .linkedProviders(List.of("google"))
                                        .securitySettings(new UserSecuritySettings())
                                        .build();
                                
                                return userRepository.save(newUser);
                            }
                        })
                        .thenCompose(user -> createSessionAndGenerateTokens(user, googleId, true));
                
            } catch (Exception e) {
                throw new AuthException("Google authentication failed", e);
            }
        }).thenCompose(future -> future);
    }
    
    @Override
    public CompletableFuture<TokenResponse> refreshToken(String refreshToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verify refresh token
                var decodedToken = tokenService.verifyRefreshToken(refreshToken);
                String sessionId = decodedToken.getClaim("session_id").asString();
                String tokenHash = tokenService.hashToken(refreshToken);
                
                return sessionRepository.findByRefreshTokenHash(tokenHash)
                        .thenCompose(optionalSession -> {
                            Session session = optionalSession.orElseThrow(() ->
                                    new AuthException("Invalid refresh token"));
                            
                            if (!session.isActive()) {
                                throw new AuthException("Session is no longer active");
                            }
                            
                            // Update session last accessed time
                            return sessionRepository.updateLastAccessed(sessionId)
                                    .thenCompose(updatedSession -> 
                                            userRepository.findById(session.getUserId()))
                                    .thenCompose(optionalUser -> {
                                        User user = optionalUser.orElseThrow(() ->
                                                new AuthException("User not found"));
                                        
                                        // Generate new tokens
                                        String newAccessToken = tokenService.generateAccessToken(user, null);
                                        String newRefreshToken = tokenService.generateRefreshToken(session);
                                        
                                        // Update session with new refresh token hash
                                        Session updatedSession = session.toBuilder()
                                                .refreshTokenHash(tokenService.hashToken(newRefreshToken))
                                                .lastAccessedAt(Instant.now())
                                                .build();
                                        
                                        return sessionRepository.create(updatedSession)
                                                .thenApply(s -> new TokenResponse(
                                                        newAccessToken,
                                                        newRefreshToken,
                                                        900, // 15 minutes
                                                        "Bearer"
                                                ));
                                    });
                        });
            } catch (Exception e) {
                throw new AuthException("Token refresh failed", e);
            }
        }).thenCompose(future -> future);
    }
    
    @Override
    public CompletableFuture<Void> logout(String refreshToken, boolean logoutAll) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var decodedToken = tokenService.verifyRefreshToken(refreshToken);
                String userId = decodedToken.getSubject();
                String sessionId = decodedToken.getClaim("session_id").asString();
                
                if (logoutAll) {
                    return sessionRepository.revokeAllForUser(userId)
                            .thenApply(count -> null);
                } else {
                    return sessionRepository.revoke(sessionId)
                            .thenApply(success -> null);
                }
            } catch (Exception e) {
                // Log error but don't throw - logout should always succeed
                logger.error("Error during logout", e);
                return CompletableFuture.<Void>completedFuture(null);
            }
        }).thenCompose(future -> future);
    }
    
    @Override
    public CompletableFuture<Void> verifyEmail(String token) {
        TokenInfo tokenInfo = verificationTokens.get(token);
        if (tokenInfo == null || tokenInfo.isExpired()) {
            throw new AuthException("Invalid or expired verification token");
        }
        
        return userRepository.findById(tokenInfo.userId())
                .thenCompose(optionalUser -> {
                    User user = optionalUser.orElseThrow(() -> 
                            new AuthException("User not found"));
                    
                    if (user.isEmailVerified()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    return userRepository.update(user.getUserId(), 
                            new UserRepository.UserUpdateRequest(null, true, null, null))
                            .thenApply(u -> null);
                })
                .whenComplete((result, error) -> {
                    // Clean up token
                    verificationTokens.remove(token);
                });
    }
    
    @Override
    public CompletableFuture<Void> initiatePasswordReset(String email) {
        return userRepository.findByEmail(email)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        // Don't reveal if email exists
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    User user = optionalUser.get();
                    
                    // Generate reset token
                    String resetToken = TokenGenerator.generateSecureToken();
                    resetTokens.put(resetToken, new TokenInfo(user.getUserId(), 
                            Instant.now().plus(1, ChronoUnit.HOURS)));
                    
                    // Send reset email
                    return emailService.sendPasswordResetEmail(email, resetToken);
                });
    }
    
    @Override
    public CompletableFuture<Void> completePasswordReset(String token, String newPassword) {
        TokenInfo tokenInfo = resetTokens.get(token);
        if (tokenInfo == null || tokenInfo.isExpired()) {
            throw new AuthException("Invalid or expired reset token");
        }
        
        // Validate new password
        if (!passwordService.validatePasswordStrength(newPassword)) {
            throw new AuthException("Password does not meet strength requirements");
        }
        
        if (passwordService.isPasswordBreached(newPassword)) {
            throw new AuthException("Password has been found in data breaches");
        }
        
        String newPasswordHash = passwordService.hashPassword(newPassword);
        
        return userRepository.update(tokenInfo.userId(), 
                new UserRepository.UserUpdateRequest(null, null, newPasswordHash, null))
                .thenCompose(user -> {
                    // Revoke all sessions for security
                    return sessionRepository.revokeAllForUser(user.getUserId());
                })
                .thenApply(count -> null)
                .whenComplete((result, error) -> {
                    // Clean up token
                    resetTokens.remove(token);
                });
    }
    
    private CompletableFuture<AuthResponse> createSessionAndGenerateTokens(
            User user, String providerId, boolean rememberMe) {
        
        // Create session
        Instant now = Instant.now();
        Instant expiresAt = rememberMe ? 
                now.plus(30, ChronoUnit.DAYS) : 
                now.plus(1, ChronoUnit.DAYS);
        
        Session session = Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(user.getUserId())
                .refreshTokenHash("") // Will be set after token generation
                .createdAt(now)
                .expiresAt(expiresAt)
                .lastAccessedAt(now)
                .build();
        
        // Generate tokens
        Map<String, Object> additionalClaims = new HashMap<>();
        if (providerId != null) {
            additionalClaims.put("provider_id", providerId);
        }
        
        String accessToken = tokenService.generateAccessToken(user, additionalClaims);
        String refreshToken = tokenService.generateRefreshToken(session);
        
        // Update session with refresh token hash
        Session sessionWithToken = session.toBuilder()
                .refreshTokenHash(tokenService.hashToken(refreshToken))
                .build();
        
        return sessionRepository.create(sessionWithToken)
                .thenApply(savedSession -> new AuthResponse(
                        user.getUserId(),
                        accessToken,
                        refreshToken,
                        900, // 15 minutes
                        "Bearer",
                        new AuthResponse.UserInfo(
                                user.getEmail(),
                                user.getDisplayName(),
                                user.isEmailVerified(),
                                user.getAuthProvider().getValue()
                        )
                ));
    }
    
    private void handleFailedLogin(User user) {
        int attempts = user.getSecuritySettings().getFailedLoginAttempts() + 1;
        UserSecuritySettings.Builder settingsBuilder = user.getSecuritySettings().toBuilder()
                .failedLoginAttempts(attempts);
        
        // Lock account after 5 failed attempts
        if (attempts >= 5) {
            settingsBuilder.lockoutUntil(Timestamp.of(java.sql.Timestamp.from(
                    Instant.now().plus(15, ChronoUnit.MINUTES))));
            
            // Send account locked email
            emailService.sendAccountLockedNotification(user.getEmail(), attempts)
                    .exceptionally(e -> {
                        logger.error("Failed to send account locked email", e);
                        return null;
                    });
        }
        
        userRepository.updateSecuritySettings(user.getUserId(), settingsBuilder.build())
                .exceptionally(e -> {
                    logger.error("Failed to update security settings", e);
                    return null;
                });
    }
    
    private static class TokenInfo {
        private final String userId;
        private final Instant expiresAt;
        
        TokenInfo(String userId, Instant expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
        
        String userId() { return userId; }
        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }
}