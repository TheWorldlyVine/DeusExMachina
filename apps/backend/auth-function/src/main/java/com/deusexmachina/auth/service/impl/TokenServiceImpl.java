package com.deusexmachina.auth.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.deusexmachina.auth.domain.Session;
import com.deusexmachina.auth.domain.User;
import com.deusexmachina.auth.service.TokenService;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of TokenService for JWT operations.
 * Follows Single Responsibility Principle - only handles JWT operations.
 */
@Singleton
public class TokenServiceImpl implements TokenService {
    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);
    
    private static final String ISSUER = "deusexmachina-auth";
    private static final String AUDIENCE = "deusexmachina-client";
    private static final long ACCESS_TOKEN_EXPIRY_MINUTES = 15;
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 30;
    
    private final Algorithm algorithm;
    
    @Inject
    public TokenServiceImpl(SecretManagerServiceClient secretManager) {
        // Load JWT secret from Secret Manager
        try {
            String jwtSecret = loadJwtSecret(secretManager);
            this.algorithm = Algorithm.HMAC256(jwtSecret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TokenService", e);
        }
    }
    
    // Constructor for testing
    TokenServiceImpl(String jwtSecret) {
        try {
            this.algorithm = Algorithm.HMAC256(jwtSecret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TokenService with provided secret", e);
        }
    }
    
    @Override
    public String generateAccessToken(User user, Map<String, Object> additionalClaims) {
        Instant now = Instant.now();
        Instant expiry = now.plus(ACCESS_TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES);
        
        var jwtBuilder = JWT.create()
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withSubject(user.getUserId())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .withClaim("email", user.getEmail())
                .withClaim("email_verified", user.isEmailVerified())
                .withClaim("auth_provider", user.getAuthProvider().getValue())
                .withClaim("roles", java.util.List.of("user")); // Default role
        
        // Add additional claims if provided
        if (additionalClaims != null) {
            additionalClaims.forEach((key, value) -> {
                if (value instanceof String) {
                    jwtBuilder.withClaim(key, (String) value);
                } else if (value instanceof Integer) {
                    jwtBuilder.withClaim(key, (Integer) value);
                } else if (value instanceof Boolean) {
                    jwtBuilder.withClaim(key, (Boolean) value);
                } else if (value instanceof Date) {
                    jwtBuilder.withClaim(key, (Date) value);
                } else if (value instanceof List) {
                    jwtBuilder.withClaim(key, (List<?>) value);
                }
            });
        }
        
        return jwtBuilder.sign(algorithm);
    }
    
    @Override
    public String generateRefreshToken(Session session) {
        Instant now = Instant.now();
        Instant expiry = now.plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS);
        
        return JWT.create()
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withSubject(session.getUserId())
                .withClaim("session_id", session.getSessionId())
                .withClaim("type", "refresh")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .sign(algorithm);
    }
    
    @Override
    public DecodedJWT verifyAccessToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .build();
            
            DecodedJWT jwt = verifier.verify(token);
            
            // Ensure it's not a refresh token
            if ("refresh".equals(jwt.getClaim("type").asString())) {
                throw new JWTVerificationException("Invalid token type");
            }
            
            return jwt;
        } catch (JWTVerificationException e) {
            logger.warn("Access token verification failed", e);
            throw e;
        }
    }
    
    @Override
    public DecodedJWT verifyRefreshToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .withClaim("type", "refresh")
                    .build();
            
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            logger.warn("Refresh token verification failed", e);
            throw e;
        }
    }
    
    @Override
    public String extractUserId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getSubject();
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token", e);
            throw new IllegalArgumentException("Invalid token");
        }
    }
    
    @Override
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    private String loadJwtSecret(SecretManagerServiceClient secretManager) {
        // Try to load from Secret Manager first (for production)
        try {
            String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
            if (projectId == null || projectId.isEmpty()) {
                projectId = "deus-ex-machina-prod";
            }
            
            String secretName = String.format("projects/%s/secrets/jwt-secret/versions/latest", projectId);
            logger.info("Loading JWT secret from Secret Manager: {}", secretName);
            
            var response = secretManager.accessSecretVersion(secretName);
            return response.getPayload().getData().toStringUtf8();
        } catch (Exception e) {
            logger.warn("Failed to load JWT secret from Secret Manager, falling back to environment variable", e);
            
            // Fallback to environment variable for local development
            String secret = System.getenv("JWT_SECRET");
            if (secret == null || secret.isEmpty()) {
                logger.error("JWT_SECRET not found in environment and Secret Manager failed");
                throw new RuntimeException("JWT secret configuration error");
            }
            return secret;
        }
    }
}