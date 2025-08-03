package com.deusexmachina.novel.ai;

import com.deusexmachina.novel.ai.auth.AuthenticationMiddleware;
import com.deusexmachina.novel.ai.controller.GenerationController;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Cloud Function entry point for the Novel AI Generation Service.
 * Handles HTTP requests for AI text generation using Gemini.
 */
public class NovelAIFunction implements HttpFunction {
    private static final Logger logger = Logger.getLogger(NovelAIFunction.class.getName());
    
    private final Injector injector;
    
    public NovelAIFunction() {
        // Initialize Guice injector with module
        this.injector = Guice.createInjector(new NovelAIModule());
        logger.info("Novel AI Function initialized");
    }
    
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        // Set CORS headers
        handleCors(request, response);
        
        // Handle preflight requests
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatusCode(204);
            return;
        }
        
        // Log request info
        logger.info(String.format("Received %s request to %s", 
            request.getMethod(), request.getPath()));
        
        // Check authentication
        if (AuthenticationMiddleware.requiresAuthentication(request)) {
            if (!AuthenticationMiddleware.validateAuthentication(request, response)) {
                logger.warning("Unauthorized request to " + request.getPath());
                return;
            }
        }
        
        try {
            // Route request based on path and method
            String path = request.getPath();
            String method = request.getMethod();
            
            if (path.startsWith("/generate")) {
                handleGenerationRequest(request, response);
            } else if (path.equals("/health")) {
                handleHealthCheck(response);
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
    
    private void handleGenerationRequest(HttpRequest request, HttpResponse response) throws IOException {
        try {
            // Get the controller
            GenerationController controller = injector.getInstance(GenerationController.class);
            
            // Delegate based on specific path
            String path = request.getPath();
            
            if (path.equals("/generate")) {
                controller.handleGenerationRequest(request, response);
            } else if (path.equals("/generate/stream")) {
                controller.handleStreamingRequest(request, response);
            } else if (path.equals("/generate/count-tokens")) {
                controller.handleTokenCountRequest(request, response);
            } else {
                response.setStatusCode(404);
                try (BufferedWriter writer = response.getWriter()) {
                    writer.write("{\"error\":\"Unknown generation endpoint\"}");
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to handle generation request: " + e.getMessage());
            response.setStatusCode(500);
            try (BufferedWriter writer = response.getWriter()) {
                writer.write("{\"error\":\"Internal server error\"}");
            }
        }
    }
    
    private void handleHealthCheck(HttpResponse response) throws IOException {
        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"status\":\"healthy\",\"service\":\"novel-ai-service\"}");
        }
    }
    
    private void handleCors(HttpRequest request, HttpResponse response) {
        Optional<String> origin = request.getFirstHeader("Origin");
        if (origin.isPresent()) {
            // In production, we'd check against allowed origins
            // For now, allow all origins to support development
            response.appendHeader("Access-Control-Allow-Origin", origin.get());
        } else {
            // Fallback to wildcard if no origin header
            response.appendHeader("Access-Control-Allow-Origin", "*");
        }
        
        response.appendHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.appendHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Request-ID");
        response.appendHeader("Access-Control-Max-Age", "3600");
        response.appendHeader("Access-Control-Allow-Credentials", "true");
    }
}