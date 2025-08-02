package com.deusexmachina.functions;

import com.deusexmachina.auth.config.AuthModule;
import com.deusexmachina.auth.dto.*;
import com.deusexmachina.auth.exception.AuthException;
import com.deusexmachina.auth.service.AuthenticationService;
import com.deusexmachina.auth.service.EmailService;
import com.deusexmachina.shared.utils.JsonUtils;
import com.deusexmachina.shared.utils.ResponseUtils;
import com.deusexmachina.shared.validation.ValidationUtils;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Cloud Function for authentication endpoints.
 * Acts as a controller, delegating business logic to services.
 * Follows Single Responsibility Principle - only handles HTTP request/response.
 */
public class AuthFunction implements HttpFunction {
    private static final Logger logger = LoggerFactory.getLogger(AuthFunction.class);
    private static final Gson gson = JsonUtils.getGson();
    private static final long REQUEST_TIMEOUT_SECONDS = 30;
    
    private final AuthenticationService authService;
    
    public AuthFunction() {
        logger.info("AuthFunction: Starting initialization");
        System.out.println("AuthFunction: Creating Guice injector");
        
        try {
            // Initialize dependency injection
            Injector injector = Guice.createInjector(new AuthModule());
            System.out.println("AuthFunction: Injector created successfully");
            
            this.authService = injector.getInstance(AuthenticationService.class);
            System.out.println("AuthFunction: AuthenticationService retrieved successfully");
            
            // Force email service initialization to test
            try {
                EmailService emailService = injector.getInstance(EmailService.class);
                System.out.println("AuthFunction: EmailService retrieved successfully - " + emailService.getClass().getName());
            } catch (Exception e) {
                System.err.println("AuthFunction: Failed to get EmailService - " + e.getMessage());
                e.printStackTrace();
            }
            
            logger.info("AuthFunction: Initialization complete");
        } catch (Exception e) {
            System.err.println("AuthFunction: Failed to initialize - " + e.getMessage());
            logger.error("AuthFunction: Failed to initialize", e);
            throw e;
        }
    }
    
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        // Set CORS headers
        ResponseUtils.setCorsHeaders(response);
        
        // Handle preflight requests
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatusCode(204);
            return;
        }
        
        String path = request.getPath();
        String method = request.getMethod();
        
        try {
            // Route to appropriate handler
            switch (path) {
                case "/auth/register":
                    handleRegister(request, response, method);
                    break;
                    
                case "/auth/login":
                    handleLogin(request, response, method);
                    break;
                    
                case "/auth/google":
                    handleGoogleLogin(request, response, method);
                    break;
                    
                case "/auth/refresh":
                    handleRefreshToken(request, response, method);
                    break;
                    
                case "/auth/logout":
                    handleLogout(request, response, method);
                    break;
                    
                case "/auth/verify-email":
                    handleVerifyEmail(request, response, method);
                    break;
                    
                case "/auth/reset-password":
                    handlePasswordReset(request, response, method);
                    break;
                    
                case "/auth/confirm-reset":
                    handleConfirmReset(request, response, method);
                    break;
                    
                default:
                    ResponseUtils.sendError(response, 404, "Endpoint not found");
                    break;
            }
        } catch (AuthException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            ResponseUtils.sendError(response, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            ResponseUtils.sendError(response, 500, "Internal server error");
        }
    }
    
    private void handleRegister(HttpRequest request, HttpResponse response, String method) 
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (!"POST".equals(method)) {
            ResponseUtils.sendError(response, 405, "Method not allowed");
            return;
        }
        
        RegisterRequest registerRequest = gson.fromJson(request.getReader(), RegisterRequest.class);
        
        // Validate request
        Set<ConstraintViolation<RegisterRequest>> violations = 
                ValidationUtils.validate(registerRequest);
        if (!violations.isEmpty()) {
            ResponseUtils.sendValidationErrors(response, violations);
            return;
        }
        
        // Process registration
        AuthResponse authResponse = authService.register(registerRequest)
                .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ResponseUtils.sendSuccess(response, authResponse, 201);
    }
    
    private void handleLogin(HttpRequest request, HttpResponse response, String method) 
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (!"POST".equals(method)) {
            ResponseUtils.sendError(response, 405, "Method not allowed");
            return;
        }
        
        LoginRequest loginRequest = gson.fromJson(request.getReader(), LoginRequest.class);
        
        // Validate request
        Set<ConstraintViolation<LoginRequest>> violations = 
                ValidationUtils.validate(loginRequest);
        if (!violations.isEmpty()) {
            ResponseUtils.sendValidationErrors(response, violations);
            return;
        }
        
        // Process login
        AuthResponse authResponse = authService.login(loginRequest)
                .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ResponseUtils.sendSuccess(response, authResponse);
    }
    
    private void handleGoogleLogin(HttpRequest request, HttpResponse response, String method) 
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (!"POST".equals(method)) {
            ResponseUtils.sendError(response, 405, "Method not allowed");
            return;
        }
        
        GoogleLoginRequest googleRequest = gson.fromJson(request.getReader(), GoogleLoginRequest.class);
        
        // Validate request
        Set<ConstraintViolation<GoogleLoginRequest>> violations = 
                ValidationUtils.validate(googleRequest);
        if (!violations.isEmpty()) {
            ResponseUtils.sendValidationErrors(response, violations);
            return;
        }
        
        // Process Google login
        AuthResponse authResponse = authService.googleLogin(googleRequest)
                .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ResponseUtils.sendSuccess(response, authResponse);
    }
    
    private void handleRefreshToken(HttpRequest request, HttpResponse response, String method) 
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (!"POST".equals(method)) {
            ResponseUtils.sendError(response, 405, "Method not allowed");
            return;
        }
        
        JsonObject requestBody = gson.fromJson(request.getReader(), JsonObject.class);
        String refreshToken = requestBody.get("refresh_token").getAsString();
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            ResponseUtils.sendError(response, 400, "Refresh token is required");
            return;
        }
        
        // Process token refresh
        TokenResponse tokenResponse = authService.refreshToken(refreshToken)
                .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ResponseUtils.sendSuccess(response, tokenResponse);
    }
    
    private void handleLogout(HttpRequest request, HttpResponse response, String method) 
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (!"POST".equals(method)) {
            ResponseUtils.sendError(response, 405, "Method not allowed");
            return;
        }
        
        JsonObject requestBody = gson.fromJson(request.getReader(), JsonObject.class);
        String refreshToken = requestBody.get("refresh_token").getAsString();
        boolean logoutAll = requestBody.has("logout_all") && 
                requestBody.get("logout_all").getAsBoolean();
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            ResponseUtils.sendError(response, 400, "Refresh token is required");
            return;
        }
        
        // Process logout
        authService.logout(refreshToken, logoutAll)
                .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        ResponseUtils.sendSuccess(response, new JsonObject());
    }
    
    private void handleVerifyEmail(HttpRequest request, HttpResponse response, String method) 
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (!"POST".equals(method)) {
            ResponseUtils.sendError(response, 405, "Method not allowed");
            return;
        }
        
        JsonObject requestBody = gson.fromJson(request.getReader(), JsonObject.class);
        String token = requestBody.get("token").getAsString();
        
        if (token == null || token.isEmpty()) {
            ResponseUtils.sendError(response, 400, "Token is required");
            return;
        }
        
        // Process email verification
        authService.verifyEmail(token)
                .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("message", "Email verified successfully");
        ResponseUtils.sendSuccess(response, successResponse);
    }
    
    private void handlePasswordReset(HttpRequest request, HttpResponse response, String method) 
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (!"POST".equals(method)) {
            ResponseUtils.sendError(response, 405, "Method not allowed");
            return;
        }
        
        JsonObject requestBody = gson.fromJson(request.getReader(), JsonObject.class);
        String email = requestBody.get("email").getAsString();
        
        if (email == null || email.isEmpty()) {
            ResponseUtils.sendError(response, 400, "Email is required");
            return;
        }
        
        // Process password reset request
        authService.initiatePasswordReset(email)
                .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("message", "If the email exists, a reset link has been sent");
        ResponseUtils.sendSuccess(response, successResponse);
    }
    
    private void handleConfirmReset(HttpRequest request, HttpResponse response, String method) 
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (!"POST".equals(method)) {
            ResponseUtils.sendError(response, 405, "Method not allowed");
            return;
        }
        
        JsonObject requestBody = gson.fromJson(request.getReader(), JsonObject.class);
        String token = requestBody.get("token").getAsString();
        String newPassword = requestBody.get("new_password").getAsString();
        
        if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            ResponseUtils.sendError(response, 400, "Token and new password are required");
            return;
        }
        
        // Process password reset confirmation
        authService.completePasswordReset(token, newPassword)
                .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("message", "Password reset successfully");
        ResponseUtils.sendSuccess(response, successResponse);
    }
}