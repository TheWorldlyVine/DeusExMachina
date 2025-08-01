package com.deusexmachina.auth.service.impl;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.deusexmachina.auth.domain.AuthProvider;
import com.deusexmachina.auth.domain.Session;
import com.deusexmachina.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenServiceImpl.
 * Tests JWT generation and validation.
 */
class TokenServiceImplTest {
    
    private TokenServiceImpl tokenService;
    private User testUser;
    private Session testSession;
    
    @BeforeEach
    void setUp() {
        // Initialize with test secret
        tokenService = new TokenServiceImpl("test-secret-key-for-testing-only");
        
        // Create test user
        testUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .email("test@example.com")
                .displayName("Test User")
                .authProvider(AuthProvider.EMAIL)
                .emailVerified(true)
                .build();
        
        // Create test session
        testSession = Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(testUser.getUserId())
                .refreshTokenHash("test-hash")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
    }
    
    @Test
    void testGenerateAccessToken() {
        String token = tokenService.generateAccessToken(testUser, null);
        
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }
    
    @Test
    void testGenerateAccessToken_WithAdditionalClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "admin");
        claims.put("tenant", "test-tenant");
        
        String token = tokenService.generateAccessToken(testUser, claims);
        DecodedJWT decoded = tokenService.verifyAccessToken(token);
        
        assertEquals("admin", decoded.getClaim("role").asString());
        assertEquals("test-tenant", decoded.getClaim("tenant").asString());
    }
    
    @Test
    void testVerifyAccessToken_ValidToken() {
        String token = tokenService.generateAccessToken(testUser, null);
        DecodedJWT decoded = tokenService.verifyAccessToken(token);
        
        assertEquals(testUser.getUserId(), decoded.getSubject());
        assertEquals(testUser.getEmail(), decoded.getClaim("email").asString());
        assertEquals(testUser.isEmailVerified(), decoded.getClaim("email_verified").asBoolean());
        assertEquals("deusexmachina-auth", decoded.getIssuer());
        assertEquals("deusexmachina-client", decoded.getAudience().get(0));
    }
    
    @Test
    void testVerifyAccessToken_ExpiredToken() throws InterruptedException {
        // Create token service with very short expiry for testing
        TokenServiceImpl shortExpiryService = new TokenServiceImpl("test-secret") {
            @Override
            public String generateAccessToken(User user, Map<String, Object> additionalClaims) {
                // Override to create already expired token
                return com.auth0.jwt.JWT.create()
                        .withIssuer("deusexmachina-auth")
                        .withSubject(user.getUserId())
                        .withExpiresAt(java.util.Date.from(Instant.now().minusSeconds(60)))
                        .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("test-secret"));
            }
        };
        
        String token = shortExpiryService.generateAccessToken(testUser, null);
        
        assertThrows(JWTVerificationException.class, () -> 
                shortExpiryService.verifyAccessToken(token));
    }
    
    @Test
    void testVerifyAccessToken_InvalidSignature() {
        String token = tokenService.generateAccessToken(testUser, null);
        
        // Tamper with token
        String[] parts = token.split("\\.");
        parts[2] = "tampered-signature";
        String tamperedToken = String.join(".", parts);
        
        assertThrows(JWTVerificationException.class, () -> 
                tokenService.verifyAccessToken(tamperedToken));
    }
    
    @Test
    void testGenerateRefreshToken() {
        String token = tokenService.generateRefreshToken(testSession);
        
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }
    
    @Test
    void testVerifyRefreshToken_ValidToken() {
        String token = tokenService.generateRefreshToken(testSession);
        DecodedJWT decoded = tokenService.verifyRefreshToken(token);
        
        assertEquals(testSession.getUserId(), decoded.getSubject());
        assertEquals(testSession.getSessionId(), decoded.getClaim("session_id").asString());
        assertEquals("refresh", decoded.getClaim("type").asString());
    }
    
    @Test
    void testVerifyRefreshToken_RejectsAccessToken() {
        String accessToken = tokenService.generateAccessToken(testUser, null);
        
        assertThrows(JWTVerificationException.class, () -> 
                tokenService.verifyRefreshToken(accessToken));
    }
    
    @Test
    void testHashToken() {
        String token = "test-token";
        String hash1 = tokenService.hashToken(token);
        String hash2 = tokenService.hashToken(token);
        
        assertNotNull(hash1);
        assertEquals(hash1, hash2); // Same token should produce same hash
        assertNotEquals(token, hash1); // Hash should be different from original
        assertTrue(hash1.length() > 20); // SHA-256 produces long hash
    }
    
    @Test
    void testHashToken_DifferentTokensDifferentHashes() {
        String hash1 = tokenService.hashToken("token1");
        String hash2 = tokenService.hashToken("token2");
        
        assertNotEquals(hash1, hash2);
    }
    
    @Test
    void testExtractUserId() {
        String token = tokenService.generateAccessToken(testUser, null);
        String userId = tokenService.extractUserId(token);
        
        assertEquals(testUser.getUserId(), userId);
    }
    
    @Test
    void testExtractUserId_InvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> 
                tokenService.extractUserId("invalid-token"));
    }
}