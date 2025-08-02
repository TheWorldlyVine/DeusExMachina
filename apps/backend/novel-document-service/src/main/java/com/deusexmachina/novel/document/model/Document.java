package com.deusexmachina.novel.document.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class Document {
    
    @NonNull
    private String id;
    
    @NonNull
    private String contextId;
    
    @NonNull
    private String title;
    
    private String subtitle;
    
    private String authorId;
    
    private String authorName;
    
    @NonNull
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;
    
    @NonNull
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @NonNull
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    private String description;
    
    private String genre;
    
    private List<String> tags;
    
    @Builder.Default
    private int wordCount = 0;
    
    @Builder.Default
    private int chapterCount = 0;
    
    @Builder.Default
    private int sceneCount = 0;
    
    private String currentVersion;
    
    private List<String> chapterIds;
    
    private DocumentSettings settings;
    
    private Map<String, Object> metadata;
    
    @Builder.Default
    private boolean active = true;
    
    @Data
    @Builder
    public static class DocumentSettings {
        @Builder.Default
        private int targetWordCount = 80000;
        
        @Builder.Default
        private String language = "en";
        
        @Builder.Default
        private String style = "narrative";
        
        @Builder.Default
        private boolean autoSave = true;
        
        @Builder.Default
        private int autoSaveIntervalSeconds = 60;
        
        private Map<String, Object> customSettings;
    }
    
    public void updateWordCount(int newCount) {
        this.wordCount = newCount;
        this.updatedAt = Instant.now();
    }
    
    public void incrementChapterCount() {
        this.chapterCount++;
        this.updatedAt = Instant.now();
    }
    
    public void incrementSceneCount() {
        this.sceneCount++;
        this.updatedAt = Instant.now();
    }
}