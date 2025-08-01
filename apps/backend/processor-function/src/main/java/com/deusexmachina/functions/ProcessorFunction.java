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
 * Processor Cloud Function for async tasks.
 * This is a placeholder implementation.
 */
public class ProcessorFunction implements HttpFunction {
    private static final Logger logger = LoggerFactory.getLogger(ProcessorFunction.class);
    
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
        logger.info("Processor request: {} {}", request.getMethod(), path);
        
        try {
            // Route based on path
            if ("/processor/health".equals(path)) {
                handleHealthCheck(request, response);
            } else if (path.startsWith("/processor/")) {
                // Placeholder for future processor endpoints
                JsonObject result = new JsonObject();
                result.addProperty("message", "Processor endpoint not implemented yet");
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
        health.addProperty("service", "processor-function");
        health.addProperty("version", "1.0.0");
        health.addProperty("timestamp", System.currentTimeMillis());
        
        ResponseUtils.sendSuccess(response, health);
    }
}