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
 * Character memory model following SCORE pattern.
 * Stores comprehensive character state, context, observations, reflections, and execution plans.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterMemory {
    @DocumentId
    private String characterId;
    
    private String projectId;
    private String name;
    private String role; // protagonist, antagonist, supporting, minor
    
    // State (S in SCORE)
    private CharacterState currentState;
    private Map<String, Object> attributes; // Physical, personality traits
    private Map<String, String> relationships; // Character ID -> relationship type
    
    // Context (C in SCORE)
    private String backstory;
    private List<String> goals;
    private List<String> motivations;
    private List<String> conflicts;
    private String currentLocation;
    private String currentObjective;
    
    // Observations (O in SCORE)
    private List<CharacterObservation> observations;
    private List<String> recentSceneIds; // Last 10 scenes character appeared in
    
    // Reflections (R in SCORE)
    private List<CharacterReflection> reflections;
    private String characterArc;
    private Map<String, Integer> emotionalJourney; // Chapter -> emotional state
    
    // Execution (E in SCORE)
    private List<String> plannedActions;
    private String nextSceneObjective;
    
    // Metadata
    @ServerTimestamp
    private Timestamp createdAt;
    @ServerTimestamp
    private Timestamp updatedAt;
    private Long lastSceneNumber;
    private Long wordCount; // Total words written for this character
    
    // Consistency tracking
    private Map<String, Object> consistencyRules; // Rules to maintain
    private List<String> speechPatterns;
    private String voiceProfile;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterState {
        private String emotionalState;
        private String physicalState;
        private String mentalState;
        private Map<String, Object> inventory; // Items character possesses
        private Integer energyLevel; // 0-100
        private Integer stressLevel; // 0-100
        private Timestamp stateTimestamp;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterObservation {
        private String observationId;
        private String sceneId;
        private String observation;
        private String observationType; // action, dialogue, thought, interaction
        private List<String> involvedCharacterIds;
        private Timestamp timestamp;
        private Long sceneNumber;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterReflection {
        private String reflectionId;
        private String reflection;
        private String trigger; // What prompted this reflection
        private String impact; // How it affects character development
        private Timestamp timestamp;
        private Long chapterNumber;
    }
}