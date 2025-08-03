package com.deusexmachina.novel.memory.services;

import com.deusexmachina.novel.memory.models.GenerationContext;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for building generation context from memory.
 */
public interface ContextService {
    
    /**
     * Build complete generation context for a scene.
     */
    CompletableFuture<GenerationContext> buildGenerationContext(String projectId, String sceneId,
            Long chapterNumber, Long sceneNumber);
    
    /**
     * Build character-focused context.
     */
    CompletableFuture<GenerationContext> buildCharacterContext(String projectId, String characterId,
            String sceneId);
    
    /**
     * Build plot-focused context.
     */
    CompletableFuture<GenerationContext> buildPlotContext(String projectId, String plotId,
            Long chapterNumber);
    
    /**
     * Build location-focused context.
     */
    CompletableFuture<GenerationContext> buildLocationContext(String projectId, String locationId);
    
    /**
     * Validate context for consistency.
     */
    CompletableFuture<Boolean> validateContext(GenerationContext context);
}