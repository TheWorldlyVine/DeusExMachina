package com.deusexmachina.novel.ai.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.deusexmachina.novel.ai.model.GenerationRequest;
import com.deusexmachina.novel.ai.model.GenerationResponse;
import com.deusexmachina.novel.ai.service.AIGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@Singleton
public class GenerationController {
    private static final Logger logger = LoggerFactory.getLogger(GenerationController.class);
    
    private final AIGenerationService generationService;
    
    @Inject
    public GenerationController(AIGenerationService generationService) {
        this.generationService = generationService;
    }
    
    public CompletableFuture<GenerationResponse> generateText(GenerationRequest request) {
        logger.info("Generating text for type: {} with contextId: {}", 
                request.getGenerationType(), request.getContextId());
        
        return generationService.generateText(request)
                .whenComplete((response, error) -> {
                    if (error != null) {
                        logger.error("Generation failed for contextId: {}", request.getContextId(), error);
                    } else {
                        logger.info("Generation completed for contextId: {}, tokens: {}", 
                                request.getContextId(), response.getTokenCount());
                    }
                });
    }
    
    public boolean validateRequest(GenerationRequest request) {
        if (request == null) {
            return false;
        }
        
        return generationService.validatePrompt(request.getPrompt());
    }
    
    public int estimateTokens(String text) {
        return generationService.estimateTokens(text);
    }
}