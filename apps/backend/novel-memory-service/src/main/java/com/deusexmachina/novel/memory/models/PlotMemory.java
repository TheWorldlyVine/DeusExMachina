package com.deusexmachina.novel.memory.models;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Plot memory model for tracking narrative threads and story progression.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlotMemory {
    @DocumentId
    private String plotId;
    
    private String projectId;
    private String threadName;
    private String threadType; // main, subplot, character-arc, thematic
    private String status; // setup, development, climax, resolution, completed
    
    // Plot structure
    private String premise;
    private String centralConflict;
    private List<PlotPoint> plotPoints;
    private List<PlotThread> subThreads;
    
    // Story elements
    private List<String> themes;
    private List<String> motifs;
    private Map<String, String> symbolism; // symbol -> meaning
    
    // Progression tracking
    private Integer tensionLevel; // 0-100
    private String currentPhase;
    private List<Milestone> milestones;
    private List<String> foreshadowing;
    private List<String> payoffs;
    
    // Relationships
    private List<String> involvedCharacterIds;
    private Map<String, String> characterRoles; // characterId -> role in plot
    private List<String> relatedSceneIds;
    
    // Pacing
    private Map<Long, Integer> chapterTension; // chapterNumber -> tension level
    private List<PacingMarker> pacingMarkers;
    
    @ServerTimestamp
    private Timestamp createdAt;
    @ServerTimestamp
    private Timestamp updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlotPoint {
        private String pointId;
        private String description;
        private String type; // inciting incident, turning point, climax, etc
        private Long targetChapter;
        private Long actualChapter;
        private String status; // planned, written, revised
        private Integer importance; // 1-10
        private List<String> setupRequirements;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlotThread {
        private String threadId;
        private String parentThreadId;
        private String name;
        private String description;
        private String status;
        private List<String> dependencies; // Other thread IDs this depends on
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Milestone {
        private String milestoneId;
        private String name;
        private String description;
        private Long chapterNumber;
        private Long sceneNumber;
        private Timestamp achievedAt;
        private String impact; // How it affects the story
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PacingMarker {
        private String markerId;
        private Long chapterNumber;
        private String paceType; // fast, moderate, slow
        private String reason;
        private String recommendation;
    }
}