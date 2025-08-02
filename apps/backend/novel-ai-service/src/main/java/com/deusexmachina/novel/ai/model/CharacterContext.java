package com.deusexmachina.novel.ai.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * Character information for context-aware generation.
 */
@Data
@Builder
public class CharacterContext {
    
    /**
     * Unique character ID.
     */
    private String characterId;
    
    /**
     * Character name.
     */
    private String name;
    
    /**
     * Character role in the story (protagonist, antagonist, supporting, etc.).
     */
    private String role;
    
    /**
     * Brief character description.
     */
    private String description;
    
    /**
     * Personality traits.
     */
    private String personality;
    
    /**
     * Speaking style and voice.
     */
    private String voice;
    
    /**
     * Character's current emotional state.
     */
    private String currentMood;
    
    /**
     * Character's goals and motivations.
     */
    private String goals;
    
    /**
     * Relationships with other characters.
     */
    private Map<String, String> relationships;
    
    /**
     * Recent actions or events involving this character.
     */
    private String recentHistory;
    
    /**
     * Any specific constraints or rules for this character.
     */
    private String constraints;
}