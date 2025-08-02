package com.deusexmachina.novel.ai;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedWriter;
import java.io.IOException;
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
        response.appendHeader("Access-Control-Allow-Origin", "*");
        response.appendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
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
        // TODO: Implement AI generation request handling
        // This will use Vertex AI / Gemini for text generation
        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"status\":\"AI generation service is running\",\"message\":\"Gemini implementation pending\"}");
        }
    }
    
    private void handleHealthCheck(HttpResponse response) throws IOException {
        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"status\":\"healthy\",\"service\":\"novel-ai-service\"}");
        }
    }
}