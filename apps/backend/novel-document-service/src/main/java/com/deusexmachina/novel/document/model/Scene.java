package com.deusexmachina.novel.document.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scene {
    
    @NonNull
    private String id;
    
    @NonNull
    private String chapterId;
    
    @NonNull
    private String documentId;
    
    @NonNull
    private String content;
    
    @Builder.Default
    private int sceneNumber = 1;
    
    private String title;
    
    private String summary;
    
    @NonNull
    @Builder.Default
    private SceneType type = SceneType.NARRATIVE;
    
    @NonNull
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @NonNull
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Builder.Default
    private int wordCount = 0;
    
    private List<String> characterIds;
    
    private String locationId;
    
    private Map<String, Object> metadata;
    
    private String notes;
    
    @Builder.Default
    private boolean active = true;
    
    // Storage reference for chunked content if applicable
    private String storageRef;
    
    public void updateContent(String newContent) {
        this.content = newContent;
        this.wordCount = countWords(newContent);
        this.updatedAt = Instant.now();
    }
    
    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}