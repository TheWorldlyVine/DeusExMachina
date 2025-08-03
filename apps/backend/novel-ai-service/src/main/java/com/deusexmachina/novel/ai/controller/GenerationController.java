package com.deusexmachina.novel.ai.controller;

import com.deusexmachina.novel.ai.auth.AuthenticationMiddleware;
import com.deusexmachina.novel.ai.model.GenerationRequest;
import com.deusexmachina.novel.ai.model.GenerationResponse;
import com.deusexmachina.novel.ai.service.GenerationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Controller for handling generation requests.
 */
@Singleton
public class GenerationController {
    private static final Logger logger = LoggerFactory.getLogger(GenerationController.class);
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .create();
    
    private final GenerationService generationService;
    
    @Inject
    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
        logger.info("GenerationController initialized");
    }
    
    /**
     * Handle a generation request.
     */
    public void handleGenerationRequest(
            com.google.cloud.functions.HttpRequest request,
            com.google.cloud.functions.HttpResponse response) throws IOException {
        
        try {
            // Parse request body
            String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));
            
            GenerationRequest genRequest = gson.fromJson(body, GenerationRequest.class);
            
            // Validate request
            ValidationResult validation = validateRequest(genRequest);
            if (!validation.isValid()) {
                sendErrorResponse(response, validation.getError(), 400);
                return;
            }
            
            // Add user ID from auth header if not present
            if (genRequest.getUserId() == null) {
                String userId = extractUserId(request);
                logger.info("Extracted user ID from JWT: {}", userId);
                genRequest.setUserId(userId);
            }
            
            // Log request details
            logger.info("Generation request details - prompt: {}, metadata: {}", 
                genRequest.getPrompt(), genRequest.getMetadata());
            
            // Generate text
            logger.info("Processing generation request for user: {} type: {}", 
                genRequest.getUserId(), genRequest.getGenerationType());
            
            GenerationResponse genResponse = generationService.generateTextSync(genRequest);
            
            // Send response
            sendSuccessResponse(response, genResponse);
            
        } catch (JsonSyntaxException e) {
            logger.error("Invalid JSON in request", e);
            sendErrorResponse(response, "Invalid JSON format", 400);
        } catch (GenerationService.GenerationException e) {
            logger.error("Generation failed", e);
            sendErrorResponse(response, "Generation failed: " + e.getMessage(), 500);
        } catch (Exception e) {
            logger.error("Unexpected error in generation request - Type: {}, Message: {}", 
                e.getClass().getName(), e.getMessage(), e);
            sendErrorResponse(response, "Internal server error: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Handle a streaming generation request.
     */
    public void handleStreamingRequest(
            com.google.cloud.functions.HttpRequest request,
            com.google.cloud.functions.HttpResponse response) throws IOException {
        
        try {
            // Parse request
            String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));
            
            GenerationRequest genRequest = gson.fromJson(body, GenerationRequest.class);
            genRequest.getParameters().setStreaming(true);
            
            // Validate
            ValidationResult validation = validateRequest(genRequest);
            if (!validation.isValid()) {
                sendErrorResponse(response, validation.getError(), 400);
                return;
            }
            
            // Set up streaming response
            response.setContentType("text/event-stream");
            response.appendHeader("Cache-Control", "no-cache");
            response.appendHeader("Connection", "keep-alive");
            
            BufferedWriter writer = response.getWriter();
            
            // Stream generation
            CompletableFuture<Void> streamFuture = generationService.streamText(genRequest, 
                new GenerationService.StreamCallback() {
                    @Override
                    public void onChunk(String chunk) {
                        try {
                            writer.write("data: " + gson.toJson(Map.of("chunk", chunk)) + "\n\n");
                            writer.flush();
                        } catch (IOException e) {
                            logger.error("Failed to write chunk", e);
                        }
                    }
                    
                    @Override
                    public void onComplete() {
                        try {
                            writer.write("data: " + gson.toJson(Map.of("done", true)) + "\n\n");
                            writer.flush();
                        } catch (IOException e) {
                            logger.error("Failed to write completion", e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            writer.write("data: " + gson.toJson(Map.of("error", error.getMessage())) + "\n\n");
                            writer.flush();
                        } catch (IOException e) {
                            logger.error("Failed to write error", e);
                        }
                    }
                });
            
            // Wait for streaming to complete
            streamFuture.join();
            
        } catch (Exception e) {
            logger.error("Streaming failed", e);
            sendErrorResponse(response, "Streaming failed: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Handle token counting request.
     */
    public void handleTokenCountRequest(
            com.google.cloud.functions.HttpRequest request,
            com.google.cloud.functions.HttpResponse response) throws IOException {
        
        try {
            String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));
            
            Map<String, String> countRequest = gson.fromJson(body, Map.class);
            String text = countRequest.get("text");
            
            if (text == null || text.isEmpty()) {
                sendErrorResponse(response, "Text is required", 400);
                return;
            }
            
            int tokenCount = generationService.countTokens(text);
            
            Map<String, Object> result = Map.of(
                "text_length", text.length(),
                "token_count", tokenCount,
                "estimated", true
            );
            
            sendSuccessResponse(response, result);
            
        } catch (Exception e) {
            logger.error("Token counting failed", e);
            sendErrorResponse(response, "Failed to count tokens", 500);
        }
    }
    
    private ValidationResult validateRequest(GenerationRequest request) {
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return ValidationResult.invalid("Prompt is required");
        }
        
        if (request.getGenerationType() == null) {
            return ValidationResult.invalid("Generation type is required");
        }
        
        if (!generationService.validatePrompt(request.getPrompt())) {
            return ValidationResult.invalid("Prompt validation failed");
        }
        
        // Check token limits
        int estimatedTokens = generationService.countTokens(request.getPrompt());
        if (estimatedTokens > 100000) {
            return ValidationResult.invalid("Prompt too long (max ~100k tokens)");
        }
        
        return ValidationResult.valid();
    }
    
    private String extractUserId(com.google.cloud.functions.HttpRequest request) {
        return AuthenticationMiddleware.extractUserId(request);
    }
    
    private void sendSuccessResponse(com.google.cloud.functions.HttpResponse response, Object data) throws IOException {
        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            gson.toJson(data, writer);
        }
    }
    
    private void sendErrorResponse(com.google.cloud.functions.HttpResponse response, String message, int statusCode) throws IOException {
        response.setStatusCode(statusCode);
        response.setContentType("application/json");
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("statusCode", statusCode);
        error.put("timestamp", Instant.now());
        
        try (BufferedWriter writer = response.getWriter()) {
            gson.toJson(error, writer);
        }
    }
    
    private static class ValidationResult {
        private final boolean valid;
        private final String error;
        
        private ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, error);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * Gson adapter for Instant serialization.
     */
    private static class InstantTypeAdapter extends com.google.gson.TypeAdapter<Instant> {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, Instant value) throws IOException {
            out.value(value.toString());
        }
        
        @Override
        public Instant read(com.google.gson.stream.JsonReader in) throws IOException {
            return Instant.parse(in.nextString());
        }
    }
}