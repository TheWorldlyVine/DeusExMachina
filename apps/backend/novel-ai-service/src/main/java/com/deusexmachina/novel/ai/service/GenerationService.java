package com.deusexmachina.novel.ai.service;

import com.deusexmachina.novel.ai.model.GenerationRequest;
import com.deusexmachina.novel.ai.model.GenerationResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for AI text generation using Gemini models.
 */
public interface GenerationService {
    
    /**
     * Generate text based on the provided request.
     * 
     * @param request The generation request containing prompt and parameters
     * @return A CompletableFuture containing the generation response
     */
    CompletableFuture<GenerationResponse> generateText(GenerationRequest request);
    
    /**
     * Generate text synchronously.
     * 
     * @param request The generation request
     * @return The generation response
     * @throws GenerationException if generation fails
     */
    GenerationResponse generateTextSync(GenerationRequest request) throws GenerationException;
    
    /**
     * Stream text generation for real-time output.
     * 
     * @param request The generation request
     * @param callback Callback for each generated chunk
     * @return A CompletableFuture that completes when streaming is done
     */
    CompletableFuture<Void> streamText(GenerationRequest request, StreamCallback callback);
    
    /**
     * Validate if a prompt is safe and appropriate.
     * 
     * @param prompt The prompt to validate
     * @return true if the prompt is safe, false otherwise
     */
    boolean validatePrompt(String prompt);
    
    /**
     * Get the token count for a given text.
     * 
     * @param text The text to count tokens for
     * @return The number of tokens
     */
    int countTokens(String text);
    
    /**
     * Callback interface for streaming responses.
     */
    interface StreamCallback {
        void onChunk(String chunk);
        void onComplete();
        void onError(Throwable error);
    }
    
    /**
     * Exception thrown when generation fails.
     */
    class GenerationException extends Exception {
        public GenerationException(String message) {
            super(message);
        }
        
        public GenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}