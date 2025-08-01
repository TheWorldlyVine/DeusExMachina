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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cloud Function for user authentication.
 * Handles login, token generation, and token verification.
 */
public class AuthFunction implements HttpFunction {
    private static final Logger logger = Logger.getLogger(AuthFunction.class.getName());
    private static final Gson gson = new Gson();
    private static final String JWT_SECRET = System.getenv("JWT_SECRET");
    private static final String JWT_ISSUER = "deusexmachina-auth";
    private static final Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET != null ? JWT_SECRET : "default-secret");
    
    /**
     * @requires request != null
     * @requires request.getMethod() != null
     * @ensures response.getStatusCode() >= 200 && response.getStatusCode() < 600
     */
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        // Set CORS headers
        response.appendHeader("Access-Control-Allow-Origin", "*");
        response.appendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.appendHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Handle preflight requests
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatusCode(204);
            return;
        }
        
        String path = request.getPath();
        String method = request.getMethod();
        
        try {
            switch (path) {
                case "/auth/login":
                    if ("POST".equals(method)) {
                        handleLogin(request, response);
                    } else {
                        sendError(response, 405, "Method not allowed");
                    }
                    break;
                    
                case "/auth/verify":
                    if ("GET".equals(method) || "POST".equals(method)) {
                        handleVerify(request, response);
                    } else {
                        sendError(response, 405, "Method not allowed");
                    }
                    break;
                    
                case "/auth/refresh":
                    if ("POST".equals(method)) {
                        handleRefresh(request, response);
                    } else {
                        sendError(response, 405, "Method not allowed");
                    }
                    break;
                    
                default:
                    sendError(response, 404, "Endpoint not found");
                    break;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing request", e);
            sendError(response, 500, "Internal server error");
        }
    }
    
    /**
     * @requires request.getMethod().equals("POST")
     * @requires request body contains valid JSON with username and password
     * @ensures response contains JWT token on success or error on failure
     */
    private void handleLogin(HttpRequest request, HttpResponse response) throws IOException {
        BufferedReader reader = request.getReader();
        LoginRequest loginRequest = gson.fromJson(reader, LoginRequest.class);
        
        // Validate input
        if (loginRequest == null || loginRequest.username == null || loginRequest.password == null) {
            sendError(response, 400, "Invalid request body");
            return;
        }
        
        // TODO: Implement actual user authentication against database
        // This is a mock implementation for demonstration
        if (isValidUser(loginRequest.username, loginRequest.password)) {
            String token = generateToken(loginRequest.username);
            String refreshToken = generateRefreshToken(loginRequest.username);
            
            JsonObject responseBody = new JsonObject();
            responseBody.addProperty("token", token);
            responseBody.addProperty("refreshToken", refreshToken);
            responseBody.addProperty("expiresIn", 3600); // 1 hour
            
            sendSuccess(response, responseBody);
        } else {
            sendError(response, 401, "Invalid credentials");
        }
    }
    
    /**
     * @requires request contains Authorization header with Bearer token
     * @ensures response indicates whether token is valid
     */
    private void handleVerify(HttpRequest request, HttpResponse response) throws IOException {
        String authHeader = request.getFirstHeader("Authorization").orElse("");
        
        if (!authHeader.startsWith("Bearer ")) {
            sendError(response, 401, "Missing or invalid authorization header");
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            DecodedJWT decodedJWT = verifyToken(token);
            
            JsonObject responseBody = new JsonObject();
            responseBody.addProperty("valid", true);
            responseBody.addProperty("username", decodedJWT.getSubject());
            responseBody.addProperty("expiresAt", decodedJWT.getExpiresAt().getTime());
            
            sendSuccess(response, responseBody);
        } catch (JWTVerificationException e) {
            logger.log(Level.WARNING, "Token verification failed", e);
            sendError(response, 401, "Invalid token");
        }
    }
    
    /**
     * @requires request contains valid refresh token
     * @ensures response contains new JWT token
     */
    private void handleRefresh(HttpRequest request, HttpResponse response) throws IOException {
        BufferedReader reader = request.getReader();
        RefreshRequest refreshRequest = gson.fromJson(reader, RefreshRequest.class);
        
        if (refreshRequest == null || refreshRequest.refreshToken == null) {
            sendError(response, 400, "Invalid request body");
            return;
        }
        
        try {
            DecodedJWT decodedJWT = verifyRefreshToken(refreshRequest.refreshToken);
            String username = decodedJWT.getSubject();
            
            String newToken = generateToken(username);
            
            JsonObject responseBody = new JsonObject();
            responseBody.addProperty("token", newToken);
            responseBody.addProperty("expiresIn", 3600); // 1 hour
            
            sendSuccess(response, responseBody);
        } catch (JWTVerificationException e) {
            logger.log(Level.WARNING, "Refresh token verification failed", e);
            sendError(response, 401, "Invalid refresh token");
        }
    }
    
    private boolean isValidUser(String username, String password) {
        // Mock implementation - replace with actual authentication logic
        return "admin".equals(username) && "password".equals(password);
    }
    
    private String generateToken(String username) {
        Instant now = Instant.now();
        return JWT.create()
            .withIssuer(JWT_ISSUER)
            .withSubject(username)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(1, ChronoUnit.HOURS)))
            .sign(algorithm);
    }
    
    private String generateRefreshToken(String username) {
        Instant now = Instant.now();
        return JWT.create()
            .withIssuer(JWT_ISSUER)
            .withSubject(username)
            .withClaim("type", "refresh")
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(7, ChronoUnit.DAYS)))
            .sign(algorithm);
    }
    
    private DecodedJWT verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm)
            .withIssuer(JWT_ISSUER)
            .build();
        return verifier.verify(token);
    }
    
    private DecodedJWT verifyRefreshToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm)
            .withIssuer(JWT_ISSUER)
            .withClaim("type", "refresh")
            .build();
        return verifier.verify(token);
    }
    
    private void sendSuccess(HttpResponse response, JsonObject body) throws IOException {
        response.setContentType("application/json");
        response.setStatusCode(200);
        PrintWriter writer = new PrintWriter(response.getWriter());
        writer.print(gson.toJson(body));
        writer.flush();
    }
    
    private void sendError(HttpResponse response, int statusCode, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatusCode(statusCode);
        
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("statusCode", statusCode);
        
        PrintWriter writer = new PrintWriter(response.getWriter());
        writer.print(gson.toJson(error));
        writer.flush();
    }
    
    // Request/Response DTOs
    static class LoginRequest {
        String username;
        String password;
    }
    
    static class RefreshRequest {
        String refreshToken;
    }
}