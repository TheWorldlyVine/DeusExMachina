package com.deusexmachina.novel.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GenerationRequest {
    
    @NotBlank(message = "Prompt is required")
    private String prompt;
    
    @NotNull(message = "Generation type is required")
    private GenerationType generationType;
    
    @NotBlank(message = "Context ID is required")
    private String contextId;
    
    @Builder.Default
    private GenerationParameters parameters = GenerationParameters.builder().build();
    
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    public static class GenerationParameters {
        @Min(0)
        @Max(2)
        @Builder.Default
        private Double temperature = 0.7;
        
        @Min(100)
        @Max(8192)
        @Builder.Default
        private Integer maxTokens = 2000;
        
        @Min(1)
        @Max(100)
        @Builder.Default
        private Integer topK = 40;
        
        @Min(0)
        @Max(1)
        @Builder.Default
        private Double topP = 0.95;
        
        @Builder.Default
        private Boolean useMemory = true;
        
        @Builder.Default
        private String modelPreference = "balanced"; // "speed", "balanced", "quality"
    }
}