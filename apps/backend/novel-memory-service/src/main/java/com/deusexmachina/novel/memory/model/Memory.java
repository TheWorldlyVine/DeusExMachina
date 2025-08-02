package com.deusexmachina.novel.memory.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class Memory {
    
    @NonNull
    private String id;
    
    @NonNull
    private String contextId;
    
    @NonNull
    private MemoryType type;
    
    @NonNull
    private String content;
    
    @NonNull
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private String entityId; // Character, location, or object ID
    
    private String entityType; // "character", "location", "object"
    
    @Builder.Default
    private double relevanceScore = 1.0;
    
    @Builder.Default
    private double importance = 0.5;
    
    @Builder.Default
    private int accessCount = 0;
    
    private Instant lastAccessed;
    
    private List<String> tags;
    
    private Map<String, Object> metadata;
    
    private String embedding; // Base64 encoded embedding vector
    
    private String chapterRef; // Chapter reference
    
    private String sceneRef; // Scene reference
    
    @Builder.Default
    private boolean active = true;
    
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessed = Instant.now();
    }
    
    public double getDecayedRelevance(double decayFactor) {
        if (lastAccessed == null) {
            return relevanceScore;
        }
        
        long hoursSinceAccess = java.time.Duration.between(lastAccessed, Instant.now()).toHours();
        return relevanceScore * Math.pow(decayFactor, hoursSinceAccess);
    }
}