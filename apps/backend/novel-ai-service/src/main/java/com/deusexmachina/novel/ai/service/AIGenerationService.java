package com.deusexmachina.novel.ai.service;

import com.deusexmachina.novel.ai.model.GenerationRequest;
import com.deusexmachina.novel.ai.model.GenerationResponse;
import java.util.concurrent.CompletableFuture;

public interface AIGenerationService {
    
    /**
     * Generate text based on the provided request.
     * 
     * @param request The generation request containing prompt, context, and parameters
     * @return A future containing the generation response
     */
    CompletableFuture<GenerationResponse> generateText(GenerationRequest request);
    
    /**
     * Validate if a prompt is within acceptable limits.
     * 
     * @param prompt The prompt to validate
     * @return true if the prompt is valid, false otherwise
     */
    boolean validatePrompt(String prompt);
    
    /**
     * Estimate the number of tokens in a text.
     * 
     * @param text The text to estimate tokens for
     * @return The estimated number of tokens
     */
    int estimateTokens(String text);
}