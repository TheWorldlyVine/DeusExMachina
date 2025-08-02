package com.deusexmachina.novel.ai.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class GenerationResponse {
    
    private String generationId;
    private String contextId;
    private GenerationType generationType;
    private String generatedText;
    private int tokenCount;
    private long generationTimeMs;
    private String modelUsed;
    private GenerationMetrics metrics;
    private List<String> warnings;
    private Map<String, Object> metadata;
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    @Data
    @Builder
    public static class GenerationMetrics {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        private double estimatedCost;
        private int contextWindowUsed;
        private int memoryItemsUsed;
        private double coherenceScore;
        private double relevanceScore;
    }
}