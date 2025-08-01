package com.deusexmachina.functions;

import com.deusexmachina.shared.utils.ResponseUtils;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main API Cloud Function.
 * This is a placeholder implementation.
 */
public class ApiFunction implements HttpFunction {
    private static final Logger logger = LoggerFactory.getLogger(ApiFunction.class);
    
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        // Set CORS headers
        ResponseUtils.setCorsHeaders(response);
        
        // Handle preflight requests
        if ("OPTIONS".equals(request.getMethod())) {
            ResponseUtils.handlePreflight(response, "*");
            return;
        }
        
        // Get the path
        String path = request.getPath();
        logger.info("API request: {} {}", request.getMethod(), path);
        
        try {
            // Route based on path
            if ("/api/health".equals(path)) {
                handleHealthCheck(request, response);
            } else if (path.startsWith("/api/")) {
                // Placeholder for future API endpoints
                JsonObject result = new JsonObject();
                result.addProperty("message", "API endpoint not implemented yet");
                result.addProperty("path", path);
                result.addProperty("method", request.getMethod());
                ResponseUtils.sendSuccess(response, result);
            } else {
                ResponseUtils.sendNotFound(response, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error processing request", e);
            ResponseUtils.sendInternalError(response, "Internal server error");
        }
    }
    
    private void handleHealthCheck(HttpRequest request, HttpResponse response) throws IOException {
        JsonObject health = new JsonObject();
        health.addProperty("status", "healthy");
        health.addProperty("service", "api-function");
        health.addProperty("version", "1.0.0");
        health.addProperty("timestamp", System.currentTimeMillis());
        
        ResponseUtils.sendSuccess(response, health);
    }
}