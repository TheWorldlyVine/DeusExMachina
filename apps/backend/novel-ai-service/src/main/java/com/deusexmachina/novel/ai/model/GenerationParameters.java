package com.deusexmachina.novel.ai.model;

import lombok.Builder;
import lombok.Data;

/**
 * Parameters for controlling AI text generation.
 */
@Data
@Builder
public class GenerationParameters {
    
    /**
     * Temperature controls randomness (0.0 = deterministic, 2.0 = very random).
     * Default: 0.8 for creative writing
     */
    @Builder.Default
    private double temperature = 0.8;
    
    /**
     * Maximum number of tokens to generate.
     * Default: 1500
     */
    @Builder.Default
    private int maxTokens = 1500;
    
    /**
     * Top-K sampling parameter (0 = disabled).
     * Default: 40
     */
    @Builder.Default
    private int topK = 40;
    
    /**
     * Top-P (nucleus) sampling parameter.
     * Default: 0.95
     */
    @Builder.Default
    private double topP = 0.95;
    
    /**
     * Number of candidate outputs to generate.
     * Default: 1
     */
    @Builder.Default
    private int candidateCount = 1;
    
    /**
     * Stop sequences to end generation.
     */
    private String[] stopSequences;
    
    /**
     * Model preference: SPEED, BALANCED, or QUALITY.
     */
    @Builder.Default
    private ModelPreference modelPreference = ModelPreference.BALANCED;
    
    /**
     * Whether to use streaming for real-time output.
     */
    @Builder.Default
    private boolean streaming = false;
    
    /**
     * Safety settings level.
     */
    @Builder.Default
    private SafetyLevel safetyLevel = SafetyLevel.MODERATE;
    
    /**
     * Frequency penalty to reduce repetition (-2.0 to 2.0).
     */
    @Builder.Default
    private double frequencyPenalty = 0.3;
    
    /**
     * Presence penalty to encourage new topics (-2.0 to 2.0).
     */
    @Builder.Default
    private double presencePenalty = 0.3;
    
    public enum ModelPreference {
        /**
         * Use Gemini Flash for fastest response.
         */
        SPEED("gemini-2.5-flash"),
        
        /**
         * Use standard Gemini Pro for balance.
         */
        BALANCED("gemini-2.5-pro"),
        
        /**
         * Use Gemini Pro with higher parameters for best quality.
         */
        QUALITY("gemini-2.5-pro-preview");
        
        private final String modelName;
        
        ModelPreference(String modelName) {
            this.modelName = modelName;
        }
        
        public String getModelName() {
            return modelName;
        }
    }
    
    public enum SafetyLevel {
        /**
         * Block only high-severity harmful content.
         */
        MINIMAL,
        
        /**
         * Block medium and high-severity harmful content.
         */
        MODERATE,
        
        /**
         * Block all potentially harmful content.
         */
        STRICT
    }
}