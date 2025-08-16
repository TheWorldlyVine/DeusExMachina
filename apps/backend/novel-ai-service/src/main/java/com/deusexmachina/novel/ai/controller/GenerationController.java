package com.deusexmachina.novel.ai.controller;

import com.deusexmachina.novel.ai.model.GenerationRequest;
import com.deusexmachina.novel.ai.model.GenerationResponse;
import com.deusexmachina.novel.ai.service.GenerationService;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller for handling generation requests.
 */
@Singleton
public class GenerationController {
    private static final Logger logger = LoggerFactory.getLogger(GenerationController.class);
    private static final long GENERATION_TIMEOUT_SECONDS = 120; // 2 minutes timeout
    
    private final GenerationService generationService;
    
    @Inject
    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }
    
    /**
     * Handle a generation request.
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws IOException if I/O fails
     */
    public void handleGenerationRequest(HttpRequest request, HttpResponse response) throws IOException {
        try {
            // Parse request body
            Gson gson = new Gson();
            GenerationRequest generationRequest;
            try (BufferedReader reader = request.getReader()) {
                generationRequest = gson.fromJson(reader, GenerationRequest.class);
            }
            
            logger.info("Handling generation request for type: {} with prompt length: {}", 
                generationRequest.getGenerationType(), 
                generationRequest.getPrompt() != null ? generationRequest.getPrompt().length() : 0);
            
            // Validate request
            validateRequest(generationRequest);
            
            // Generate text asynchronously with timeout
            CompletableFuture<GenerationResponse> future = generationService.generateText(generationRequest);
            GenerationResponse generationResponse = future.get(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            logger.info("Generation completed successfully. Generated {} tokens in {}ms", 
                generationResponse.getTokenCount(), generationResponse.getGenerationTimeMs());
            
            // Write response
            response.setContentType("application/json");
            response.setStatusCode(200);
            try (BufferedWriter writer = response.getWriter()) {
                writer.write(gson.toJson(generationResponse));
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            response.setStatusCode(400);
            try (BufferedWriter writer = response.getWriter()) {
                writer.write("{\"error\":\"" + e.getMessage() + "\"}");
            }
        } catch (Exception e) {
            logger.error("Generation failed", e);
            response.setStatusCode(500);
            try (BufferedWriter writer = response.getWriter()) {
                writer.write("{\"error\":\"Generation failed: " + e.getMessage() + "\"}");
            }
        }
    }
    
    /**
     * Handle a streaming generation request.
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws IOException if I/O fails
     */
    public void handleStreamingRequest(HttpRequest request, HttpResponse response) throws IOException {
        // TODO: Implement streaming generation
        response.setStatusCode(501);
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"error\":\"Streaming not yet implemented\"}");
        }
    }
    
    /**
     * Handle a token count request.
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws IOException if I/O fails
     */
    public void handleTokenCountRequest(HttpRequest request, HttpResponse response) throws IOException {
        try {
            // Parse request body
            Gson gson = new Gson();
            TokenCountRequest tokenRequest;
            try (BufferedReader reader = request.getReader()) {
                tokenRequest = gson.fromJson(reader, TokenCountRequest.class);
            }
            
            if (tokenRequest == null || tokenRequest.getText() == null) {
                response.setStatusCode(400);
                try (BufferedWriter writer = response.getWriter()) {
                    writer.write("{\"error\":\"Text is required\"}");
                }
                return;
            }
            
            int tokenCount = generationService.countTokens(tokenRequest.getText());
            
            response.setContentType("application/json");
            response.setStatusCode(200);
            try (BufferedWriter writer = response.getWriter()) {
                writer.write("{\"tokenCount\":" + tokenCount + "}");
            }
        } catch (Exception e) {
            logger.error("Token count failed", e);
            response.setStatusCode(500);
            try (BufferedWriter writer = response.getWriter()) {
                writer.write("{\"error\":\"Token count failed: " + e.getMessage() + "\"}");
            }
        }
    }
    
    /**
     * Validate the generation request.
     * 
     * @param request The request to validate
     * @throws IllegalArgumentException if request is invalid
     */
    private void validateRequest(GenerationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }
        
        if (request.getGenerationType() == null) {
            throw new IllegalArgumentException("Generation type is required");
        }
        
        // Validate prompt length
        if (request.getPrompt().length() > 10000) {
            throw new IllegalArgumentException("Prompt exceeds maximum length of 10000 characters");
        }
        
        // Validate parameters if present
        if (request.getParameters() != null) {
            if (request.getParameters().getMaxTokens() < 1 || request.getParameters().getMaxTokens() > 8192) {
                throw new IllegalArgumentException("MaxTokens must be between 1 and 8192");
            }
            
            if (request.getParameters().getTemperature() < 0 || request.getParameters().getTemperature() > 2) {
                throw new IllegalArgumentException("Temperature must be between 0 and 2");
            }
        }
    }
    
    /**
     * Simple class for token count requests.
     */
    private static class TokenCountRequest {
        private String text;
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
}