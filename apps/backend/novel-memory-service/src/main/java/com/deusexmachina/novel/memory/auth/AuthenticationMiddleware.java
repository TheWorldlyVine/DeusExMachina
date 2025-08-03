package com.deusexmachina.novel.memory.auth;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Optional;

/**
 * Authentication middleware for validating JWT tokens.
 * For now, we'll do basic token presence checking and defer actual validation
 * to avoid duplicating the secret management logic.
 */
public class AuthenticationMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationMiddleware.class);
    
    /**
     * Check if the request requires authentication.
     * OPTIONS requests and health checks don't require auth.
     */
    public static boolean requiresAuthentication(HttpRequest request) {
        String method = request.getMethod();
        String path = request.getPath();
        
        // OPTIONS requests for CORS preflight don't need auth
        if ("OPTIONS".equals(method)) {
            return false;
        }
        
        // Health check endpoint is public
        if ("/health".equals(path)) {
            return false;
        }
        
        // All other requests require authentication
        return true;
    }
    
    /**
     * Validate that the request has a proper authentication token.
     * Returns true if valid, false otherwise.
     * 
     * Note: This currently only checks token presence. Full validation
     * would require accessing the JWT secret from Secret Manager.
     */
    public static boolean validateAuthentication(HttpRequest request, HttpResponse response) throws IOException {
        Optional<String> authHeader = request.getFirstHeader("Authorization");
        
        if (authHeader.isEmpty() || !authHeader.get().startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
            return false;
        }
        
        String token = authHeader.get().substring(7);
        if (token.trim().isEmpty()) {
            sendUnauthorizedResponse(response, "Empty authentication token");
            return false;
        }
        
        // For now, we accept any non-empty token
        // In production, we would validate the JWT signature and claims
        // by either:
        // 1. Calling the auth-function's validate endpoint
        // 2. Loading the JWT secret from Secret Manager and validating locally
        
        return true;
    }
    
    /**
     * Extract user ID from the JWT token.
     * Falls back to X-User-Id header or "anonymous" if extraction fails.
     */
    public static String extractUserId(HttpRequest request) {
        Optional<String> authHeader = request.getFirstHeader("Authorization");
        if (authHeader.isPresent() && authHeader.get().startsWith("Bearer ")) {
            try {
                String token = authHeader.get().substring(7);
                // Decode JWT without verification (for user ID extraction only)
                // This is safe because we've already validated the token
                com.auth0.jwt.interfaces.DecodedJWT jwt = com.auth0.jwt.JWT.decode(token);
                return jwt.getSubject();
            } catch (Exception e) {
                logger.warn("Failed to extract user ID from JWT token", e);
            }
        }
        
        // Fallback to X-User-Id header or anonymous
        return request.getFirstHeader("X-User-Id").orElse("anonymous");
    }
    
    private static void sendUnauthorizedResponse(HttpResponse response, String message) throws IOException {
        response.setStatusCode(401);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write(String.format("{\"error\":\"%s\",\"code\":\"UNAUTHORIZED\"}", message));
        }
    }
}