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
 * World memory model for tracking world-building elements and maintaining consistency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldMemory {
    @DocumentId
    private String worldId;
    
    private String projectId;
    private String category; // location, culture, technology, magic, history, etc
    
    // World facts
    private List<WorldFact> facts;
    private Map<String, List<String>> factCategories; // category -> fact IDs
    
    // Locations
    private List<Location> locations;
    private Map<String, String> locationHierarchy; // locationId -> parentLocationId
    
    // Timeline
    private List<HistoricalEvent> timeline;
    private String currentDate; // In-story current date/time
    private String calendarSystem;
    
    // Rules and systems
    private Map<String, String> physicalLaws; // law name -> description
    private Map<String, String> socialRules; // rule name -> description
    private Map<String, Object> magicSystem; // If applicable
    private Map<String, Object> technologyLevel;
    
    // Consistency tracking
    private List<ConsistencyRule> consistencyRules;
    private List<Contradiction> detectedContradictions;
    
    @ServerTimestamp
    private Timestamp createdAt;
    @ServerTimestamp
    private Timestamp updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorldFact {
        private String factId;
        private String fact;
        private String category;
        private String source; // Where this was established
        private Long establishedChapter;
        private Long establishedScene;
        private List<String> relatedFactIds;
        private Integer importance; // 1-10
        private Boolean mutable; // Can this fact change?
        private Timestamp createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private String locationId;
        private String name;
        private String type; // city, building, room, region, etc
        private String description;
        private Map<String, Object> properties; // Flexible properties
        private List<String> connectedLocationIds;
        private List<String> charactersPresentIds;
        private String parentLocationId;
        private Timestamp lastUsed;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalEvent {
        private String eventId;
        private String name;
        private String description;
        private String date; // In-world date
        private Long yearsBeforePresent; // For sorting
        private List<String> involvedCharacters;
        private List<String> affectedLocations;
        private String significance;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsistencyRule {
        private String ruleId;
        private String rule;
        private String category;
        private String validation; // How to check this rule
        private List<String> exampleViolations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contradiction {
        private String contradictionId;
        private String factId1;
        private String factId2;
        private String description;
        private String severity; // minor, major, critical
        private String suggestedResolution;
        private Boolean resolved;
        private Timestamp detectedAt;
    }
}