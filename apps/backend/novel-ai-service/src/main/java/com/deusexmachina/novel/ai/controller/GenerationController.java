package com.deusexmachina.novel.ai.controller;

import com.deusexmachina.novel.ai.model.GenerationRequest;
import com.deusexmachina.novel.ai.model.GenerationResponse;
import com.deusexmachina.novel.ai.service.GenerationService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param request The generation request
     * @return The generation response
     * @throws Exception if generation fails
     */
    public GenerationResponse handleGenerationRequest(GenerationRequest request) throws Exception {
        logger.info("Handling generation request for type: {} with prompt length: {}", 
            request.getGenerationType(), 
            request.getPrompt() != null ? request.getPrompt().length() : 0);
        
        // Validate request
        validateRequest(request);
        
        try {
            // Generate text asynchronously with timeout
            CompletableFuture<GenerationResponse> future = generationService.generateText(request);
            GenerationResponse response = future.get(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            logger.info("Generation completed successfully. Generated {} tokens in {}ms", 
                response.getTokenCount(), response.getGenerationTimeMs());
            
            return response;
        } catch (Exception e) {
            logger.error("Generation failed for request type: {}", request.getGenerationType(), e);
            throw new Exception("Generation failed: " + e.getMessage(), e);
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
            if (request.getParameters().getMaxTokens() != null && 
                (request.getParameters().getMaxTokens() < 1 || request.getParameters().getMaxTokens() > 8192)) {
                throw new IllegalArgumentException("MaxTokens must be between 1 and 8192");
            }
            
            if (request.getParameters().getTemperature() != null &&
                (request.getParameters().getTemperature() < 0 || request.getParameters().getTemperature() > 2)) {
                throw new IllegalArgumentException("Temperature must be between 0 and 2");
            }
        }
    }
}