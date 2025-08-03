package com.deusexmachina.novel.memory.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Generation context model that aggregates relevant memory for AI text generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationContext {
    private String projectId;
    private String sceneId;
    
    // Character context
    private List<CharacterContext> activeCharacters;
    private Map<String, String> characterRelationships; // char1_char2 -> relationship
    
    // Plot context
    private String currentPlotPhase;
    private List<String> activeThreads;
    private List<String> upcomingPlotPoints;
    private Integer currentTensionLevel;
    
    // World context
    private String currentLocation;
    private Map<String, String> locationDetails;
    private List<String> relevantWorldFacts;
    private String timeOfDay;
    private String weather;
    
    // Recent narrative context
    private String previousSceneSummary;
    private List<String> recentEvents; // Last 5-10 significant events
    private List<String> recentDialogue; // Key dialogue from recent scenes
    
    // Style and tone
    private String narrativeVoice;
    private String currentTone;
    private List<String> themesToEmphasize;
    
    // Constraints and requirements
    private List<String> mustInclude; // Elements that must appear
    private List<String> mustAvoid; // Elements to avoid
    private Map<String, String> consistencyRules; // Rules to follow
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterContext {
        private String characterId;
        private String name;
        private String currentState;
        private String currentObjective;
        private String emotionalState;
        private List<String> recentActions;
        private String voiceProfile;
        private List<String> speechPatterns;
        private Map<String, String> relationshipsInScene; // Other chars -> relationship
    }
}