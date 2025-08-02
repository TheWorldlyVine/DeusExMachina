package com.deusexmachina.novel.ai.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Request object for AI text generation.
 */
@Data
@Builder
public class GenerationRequest {
    
    /**
     * The prompt for text generation.
     */
    private String prompt;
    
    /**
     * The type of generation (SCENE, CHAPTER, DIALOGUE, etc.).
     */
    private GenerationType generationType;
    
    /**
     * The context ID for memory retrieval.
     */
    private String contextId;
    
    /**
     * The document ID this generation is for.
     */
    private String documentId;
    
    /**
     * Model parameters for generation.
     */
    @Builder.Default
    private GenerationParameters parameters = GenerationParameters.builder().build();
    
    /**
     * Additional context from previous scenes/chapters.
     */
    private List<String> previousContext;
    
    /**
     * Character information for character-aware generation.
     */
    private List<CharacterContext> characters;
    
    /**
     * Setting/location information.
     */
    private String setting;
    
    /**
     * Style guidelines for this generation.
     */
    private StyleGuide styleGuide;
    
    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;
    
    /**
     * Whether to use memory system for context.
     */
    @Builder.Default
    private boolean useMemory = true;
    
    /**
     * User ID for authentication and quota tracking.
     */
    private String userId;
}