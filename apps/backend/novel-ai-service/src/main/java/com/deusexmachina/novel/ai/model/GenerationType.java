package com.deusexmachina.novel.ai.model;

/**
 * Types of text generation supported by the AI service.
 */
public enum GenerationType {
    /**
     * Generate a complete scene with narrative, dialogue, and action.
     */
    SCENE("Generate a complete scene with narrative flow"),
    
    /**
     * Generate a full chapter with multiple scenes.
     */
    CHAPTER("Generate a cohesive chapter with beginning, middle, and end"),
    
    /**
     * Generate dialogue between characters.
     */
    DIALOGUE("Generate natural dialogue between characters"),
    
    /**
     * Generate descriptive text for settings or characters.
     */
    DESCRIPTION("Generate vivid descriptions"),
    
    /**
     * Generate action sequences.
     */
    ACTION("Generate dynamic action sequences"),
    
    /**
     * Generate transitional text between scenes.
     */
    TRANSITION("Generate smooth transitions between scenes"),
    
    /**
     * Generate a story outline or structure.
     */
    OUTLINE("Generate story structure and plot points"),
    
    /**
     * Generate internal character thoughts.
     */
    CHARACTER_THOUGHT("Generate character internal monologue"),
    
    /**
     * Generate world-building details.
     */
    WORLDBUILDING("Generate world-building elements and lore"),
    
    /**
     * Continue from where the text left off.
     */
    CONTINUATION("Continue the narrative from the last point"),
    
    /**
     * Revise or improve existing text.
     */
    REVISION("Revise and improve existing text"),
    
    /**
     * Generate a summary of existing content.
     */
    SUMMARY("Generate concise summary of content");
    
    private final String description;
    
    GenerationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the appropriate Gemini model for this generation type.
     */
    public String getRecommendedModel() {
        return switch (this) {
            case CHAPTER, OUTLINE -> "gemini-2.5-pro-preview";  // Longer context needed
            case DIALOGUE, ACTION, TRANSITION -> "gemini-2.5-flash";  // Fast generation
            default -> "gemini-2.5-pro";  // Balanced
        };
    }
    
    /**
     * Get the default max tokens for this generation type.
     */
    public int getDefaultMaxTokens() {
        return switch (this) {
            case SCENE -> 2000;
            case CHAPTER -> 5000;
            case DIALOGUE -> 1000;
            case DESCRIPTION -> 800;
            case ACTION -> 1500;
            case TRANSITION -> 500;
            case OUTLINE -> 3000;
            case CHARACTER_THOUGHT -> 600;
            case WORLDBUILDING -> 1200;
            case CONTINUATION -> 1500;
            case REVISION -> 2000;
            case SUMMARY -> 500;
        };
    }
}