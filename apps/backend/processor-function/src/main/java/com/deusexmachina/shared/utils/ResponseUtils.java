package com.deusexmachina.shared.utils;

import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility class for handling HTTP responses in Cloud Functions.
 * Provides consistent response formatting across all functions.
 */
public final class ResponseUtils {
    
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String HEADER_CORS_ORIGIN = "Access-Control-Allow-Origin";
    private static final String HEADER_CORS_METHODS = "Access-Control-Allow-Methods";
    private static final String HEADER_CORS_HEADERS = "Access-Control-Allow-Headers";
    
    private ResponseUtils() {
        // Prevent instantiation
    }
    
    /**
     * Sets standard CORS headers on the response.
     * 
     * @param response the HTTP response
     * @param allowedOrigin the allowed origin (use "*" for all)
     */
    public static void setCorsHeaders(HttpResponse response, String allowedOrigin) {
        response.appendHeader(HEADER_CORS_ORIGIN, allowedOrigin);
        response.appendHeader(HEADER_CORS_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        response.appendHeader(HEADER_CORS_HEADERS, "Content-Type, Authorization, X-Requested-With");
    }
    
    /**
     * Sets standard CORS headers with default origin.
     * 
     * @param response the HTTP response
     */
    public static void setCorsHeaders(HttpResponse response) {
        setCorsHeaders(response, "*");
    }
    
    /**
     * Sends a successful JSON response.
     * 
     * @param response the HTTP response
     * @param data the data to send
     * @throws IOException if writing fails
     */
    public static void sendSuccess(HttpResponse response, Object data) throws IOException {
        sendSuccess(response, data, 200);
    }
    
    /**
     * Sends a successful JSON response with custom status code.
     * 
     * @param response the HTTP response
     * @param data the data to send
     * @param statusCode the HTTP status code
     * @throws IOException if writing fails
     */
    public static void sendSuccess(HttpResponse response, Object data, int statusCode) throws IOException {
        response.setContentType(CONTENT_TYPE_JSON);
        response.setStatusCode(statusCode);
        
        Gson gson = JsonUtils.getGson();
        try (PrintWriter writer = new PrintWriter(response.getWriter())) {
            writer.print(gson.toJson(data));
            writer.flush();
        }
    }
    
    /**
     * Sends an error JSON response.
     * 
     * @param response the HTTP response
     * @param statusCode the HTTP status code
     * @param errorCode the application error code
     * @param message the error message
     * @throws IOException if writing fails
     */
    public static void sendError(HttpResponse response, int statusCode, String errorCode, String message) 
            throws IOException {
        JsonObject errorResponse = JsonUtils.createErrorResponse(errorCode, message, statusCode);
        sendJson(response, statusCode, errorResponse);
    }
    
    /**
     * Sends an error JSON response with default error code.
     * 
     * @param response the HTTP response
     * @param statusCode the HTTP status code
     * @param message the error message
     * @throws IOException if writing fails
     */
    public static void sendError(HttpResponse response, int statusCode, String message) 
            throws IOException {
        sendError(response, statusCode, "ERROR", message);
    }
    
    /**
     * Sends a JSON response with specified status code.
     * 
     * @param response the HTTP response
     * @param statusCode the HTTP status code
     * @param body the JSON body
     * @throws IOException if writing fails
     */
    public static void sendJson(HttpResponse response, int statusCode, JsonObject body) throws IOException {
        response.setContentType(CONTENT_TYPE_JSON);
        response.setStatusCode(statusCode);
        
        try (PrintWriter writer = new PrintWriter(response.getWriter())) {
            writer.print(body.toString());
            writer.flush();
        }
    }
    
    /**
     * Sends a plain text response.
     * 
     * @param response the HTTP response
     * @param statusCode the HTTP status code
     * @param text the text to send
     * @throws IOException if writing fails
     */
    public static void sendText(HttpResponse response, int statusCode, String text) throws IOException {
        response.setContentType("text/plain");
        response.setStatusCode(statusCode);
        
        try (PrintWriter writer = new PrintWriter(response.getWriter())) {
            writer.print(text);
            writer.flush();
        }
    }
    
    /**
     * Handles preflight OPTIONS requests for CORS.
     * 
     * @param response the HTTP response
     * @param allowedOrigin the allowed origin
     * @throws IOException if writing fails
     */
    public static void handlePreflight(HttpResponse response, String allowedOrigin) throws IOException {
        setCorsHeaders(response, allowedOrigin);
        response.setStatusCode(204);
    }
    
    /**
     * Sends a 404 Not Found response.
     * 
     * @param response the HTTP response
     * @param message the error message
     * @throws IOException if writing fails
     */
    public static void sendNotFound(HttpResponse response, String message) throws IOException {
        sendError(response, 404, "RESOURCE_NOT_FOUND", message);
    }
    
    /**
     * Sends a 401 Unauthorized response.
     * 
     * @param response the HTTP response
     * @param message the error message
     * @throws IOException if writing fails
     */
    public static void sendUnauthorized(HttpResponse response, String message) throws IOException {
        sendError(response, 401, "UNAUTHORIZED", message);
    }
    
    /**
     * Sends a 400 Bad Request response.
     * 
     * @param response the HTTP response
     * @param message the error message
     * @throws IOException if writing fails
     */
    public static void sendBadRequest(HttpResponse response, String message) throws IOException {
        sendError(response, 400, "BAD_REQUEST", message);
    }
    
    /**
     * Sends a 500 Internal Server Error response.
     * 
     * @param response the HTTP response
     * @param message the error message
     * @throws IOException if writing fails
     */
    public static void sendInternalError(HttpResponse response, String message) throws IOException {
        sendError(response, 500, "INTERNAL_ERROR", message);
    }
    
    /**
     * Sends validation errors response.
     * 
     * @param response the HTTP response
     * @param violations the constraint violations
     * @throws IOException if writing fails
     */
    public static void sendValidationErrors(HttpResponse response, Set<? extends ConstraintViolation<?>> violations) 
            throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", "Validation failed");
        error.addProperty("status_code", 400);
        
        Gson gson = JsonUtils.getGson();
        var errors = violations.stream()
                .map(violation -> {
                    JsonObject fieldError = new JsonObject();
                    fieldError.addProperty("field", violation.getPropertyPath().toString());
                    fieldError.addProperty("message", violation.getMessage());
                    return fieldError;
                })
                .collect(Collectors.toList());
        
        error.add("validation_errors", gson.toJsonTree(errors));
        
        sendJson(response, 400, error);
    }
}