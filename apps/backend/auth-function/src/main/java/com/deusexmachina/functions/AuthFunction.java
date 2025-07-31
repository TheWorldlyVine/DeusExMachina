package com.deusexmachina.functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Logger;

public class AuthFunction implements HttpFunction {
    private static final Logger logger = Logger.getLogger(AuthFunction.class.getName());
    private static final Gson gson = new Gson();
    private static final String JWT_SECRET = System.getenv("JWT_SECRET");
    private static final long TOKEN_EXPIRY_MS = 3600000; // 1 hour
    
    /**
     * @pre request != null && request.getMethod().equals("POST")
     * @post response.getStatusCode() >= 200 && response.getStatusCode() < 300
     */
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        // JML precondition check
        assert request != null : "Request cannot be null";
        
        // Set CORS headers
        response.appendHeader("Access-Control-Allow-Origin", "*");
        response.appendHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.appendHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Handle preflight requests
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatusCode(204);
            return;
        }
        
        if (!"POST".equals(request.getMethod())) {
            response.setStatusCode(405);
            response.getWriter().write("{\"error\":\"Method not allowed\"}");
            return;
        }
        
        logger.info("Processing authentication request");
        
        try {
            AuthRequest authRequest = parseRequest(request);
            AuthResponse authResponse = authenticateUser(authRequest);
            
            response.setStatusCode(200);
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(authResponse));
            
            // JML postcondition
            assert response.getStatusCode() >= 200 && response.getStatusCode() < 300 
                : "Response must be successful";
        } catch (AuthenticationException e) {
            logger.warning("Authentication failed: " + e.getMessage());
            response.setStatusCode(401);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.severe("Internal error: " + e.getMessage());
            response.setStatusCode(500);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
        }
    }
    
    private AuthRequest parseRequest(HttpRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, AuthRequest.class);
        }
    }
    
    /**
     * @requires authRequest != null && authRequest.username != null && authRequest.password != null
     * @ensures \result != null && \result.token != null
     * @signals (AuthenticationException e) invalid credentials
     */
    private AuthResponse authenticateUser(AuthRequest authRequest) throws AuthenticationException {
        // Validate input
        if (authRequest == null || authRequest.username == null || authRequest.password == null) {
            throw new AuthenticationException("Invalid request format");
        }
        
        // TODO: Implement actual authentication logic (e.g., check against database)
        // This is a placeholder implementation
        if (!"admin".equals(authRequest.username) || !"password".equals(authRequest.password)) {
            throw new AuthenticationException("Invalid credentials");
        }
        
        // Generate JWT token
        String token = generateToken(authRequest.username);
        
        return new AuthResponse(token, authRequest.username, TOKEN_EXPIRY_MS / 1000);
    }
    
    private String generateToken(String username) {
        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET != null ? JWT_SECRET : "default-secret");
        
        return JWT.create()
            .withIssuer("deus-ex-machina")
            .withSubject(username)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MS))
            .sign(algorithm);
    }
    
    // Request/Response classes
    static class AuthRequest {
        public String username;
        public String password;
    }
    
    static class AuthResponse {
        public String token;
        public String username;
        public long expiresIn;
        
        public AuthResponse(String token, String username, long expiresIn) {
            this.token = token;
            this.username = username;
            this.expiresIn = expiresIn;
        }
    }
    
    static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}