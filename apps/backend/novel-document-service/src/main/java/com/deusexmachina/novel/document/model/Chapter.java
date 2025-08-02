package com.deusexmachina.novel.document.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class Chapter {
    
    @NonNull
    private String id;
    
    @NonNull
    private String documentId;
    
    @NonNull
    private String title;
    
    @Builder.Default
    private int chapterNumber = 1;
    
    private String summary;
    
    @NonNull
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @NonNull
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Builder.Default
    private int wordCount = 0;
    
    @Builder.Default
    private int sceneCount = 0;
    
    private List<String> sceneIds;
    
    private String notes;
    
    @Builder.Default
    private boolean active = true;
    
    public void updateWordCount(int newCount) {
        this.wordCount = newCount;
        this.updatedAt = Instant.now();
    }
    
    public void incrementSceneCount() {
        this.sceneCount++;
        this.updatedAt = Instant.now();
    }
}