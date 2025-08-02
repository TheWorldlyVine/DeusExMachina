package com.deusexmachina.novel.ai.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response from AI text generation.
 */
@Data
@Builder
public class GenerationResponse {
    
    /**
     * Unique ID for this generation.
     */
    private String generationId;
    
    /**
     * The context ID used for memory retrieval.
     */
    private String contextId;
    
    /**
     * The type of generation performed.
     */
    private GenerationType generationType;
    
    /**
     * The generated text.
     */
    private String generatedText;
    
    /**
     * Alternative generations if candidateCount > 1.
     */
    private List<String> alternatives;
    
    /**
     * Token count of the generated text.
     */
    private int tokenCount;
    
    /**
     * Token count of the input prompt.
     */
    private int promptTokenCount;
    
    /**
     * Total tokens used (prompt + generation).
     */
    private int totalTokenCount;
    
    /**
     * Generation time in milliseconds.
     */
    private long generationTimeMs;
    
    /**
     * Model used for generation.
     */
    private String modelUsed;
    
    /**
     * Generation metrics.
     */
    private GenerationMetrics metrics;
    
    /**
     * Safety ratings for the generated content.
     */
    private SafetyRatings safetyRatings;
    
    /**
     * Memories used for context.
     */
    private List<String> memoriesUsed;
    
    /**
     * Any warnings or notes about the generation.
     */
    private List<String> warnings;
    
    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;
    
    /**
     * Timestamp of generation.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Whether the generation was truncated due to length.
     */
    private boolean truncated;
    
    /**
     * Confidence score (0.0 to 1.0).
     */
    private double confidenceScore;
    
    @Data
    @Builder
    public static class GenerationMetrics {
        /**
         * Estimated cost in USD.
         */
        private double estimatedCost;
        
        /**
         * Context window percentage used.
         */
        private double contextWindowUsage;
        
        /**
         * Number of memory items retrieved.
         */
        private int memoryItemsRetrieved;
        
        /**
         * Coherence score (0.0 to 1.0).
         */
        private double coherenceScore;
        
        /**
         * Relevance score to the prompt (0.0 to 1.0).
         */
        private double relevanceScore;
        
        /**
         * Creativity score (0.0 to 1.0).
         */
        private double creativityScore;
        
        /**
         * Response latency breakdown.
         */
        private LatencyBreakdown latency;
        
        @Data
        @Builder
        public static class LatencyBreakdown {
            private long memoryRetrievalMs;
            private long promptConstructionMs;
            private long modelInferenceMs;
            private long postProcessingMs;
        }
    }
    
    @Data
    @Builder
    public static class SafetyRatings {
        /**
         * Violence rating (NEGLIGIBLE, LOW, MEDIUM, HIGH).
         */
        private String violence;
        
        /**
         * Sexual content rating.
         */
        private String sexual;
        
        /**
         * Harmful content rating.
         */
        private String harmful;
        
        /**
         * Harassment rating.
         */
        private String harassment;
        
        /**
         * Overall safety score (0.0 to 1.0).
         */
        private double overallScore;
        
        /**
         * Whether any content was blocked.
         */
        private boolean contentBlocked;
        
        /**
         * Reason for blocking if applicable.
         */
        private String blockReason;
    }
}