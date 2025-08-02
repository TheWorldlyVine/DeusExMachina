package com.deusexmachina.novel.memory;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Cloud Function entry point for the Novel Memory Service.
 * Handles HTTP requests for memory management operations.
 */
public class NovelMemoryFunction implements HttpFunction {
    private static final Logger logger = Logger.getLogger(NovelMemoryFunction.class.getName());
    
    private final Injector injector;
    
    public NovelMemoryFunction() {
        // Initialize Guice injector with module
        this.injector = Guice.createInjector(new NovelMemoryModule());
        logger.info("Novel Memory Function initialized");
    }
    
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        // Set CORS headers
        response.appendHeader("Access-Control-Allow-Origin", "*");
        response.appendHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.appendHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Handle preflight requests
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatusCode(200);
            return;
        }
        
        // Log request info
        logger.info(String.format("Received %s request to %s", 
            request.getMethod(), request.getPath()));
        
        try {
            // Route request based on path and method
            String path = request.getPath();
            String method = request.getMethod();
            
            if (path.startsWith("/memory")) {
                handleMemoryRequest(request, response);
            } else {
                response.setStatusCode(404);
                try (BufferedWriter writer = response.getWriter()) {
                    writer.write("{\"error\":\"Not found\"}");
                }
            }
        } catch (Exception e) {
            logger.severe("Error processing request: " + e.getMessage());
            response.setStatusCode(500);
            try (BufferedWriter writer = response.getWriter()) {
                writer.write("{\"error\":\"Internal server error\"}");
            }
        }
    }
    
    private void handleMemoryRequest(HttpRequest request, HttpResponse response) throws IOException {
        // TODO: Implement memory request handling
        // This will route to appropriate controllers/services
        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"status\":\"Memory service is running\",\"message\":\"Implementation pending\"}");
        }
    }
}