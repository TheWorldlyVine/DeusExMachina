package com.deusexmachina.novel.memory.services;

import com.deusexmachina.novel.memory.models.CharacterMemory;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing character memory operations.
 */
public interface CharacterMemoryService {
    
    /**
     * Create or update a character's memory.
     */
    CompletableFuture<CharacterMemory> saveCharacterMemory(CharacterMemory memory);
    
    /**
     * Retrieve a character's complete memory.
     */
    CompletableFuture<CharacterMemory> getCharacterMemory(String projectId, String characterId);
    
    /**
     * Update a character's current state.
     */
    CompletableFuture<CharacterMemory> updateCharacterState(String projectId, String characterId, 
            CharacterMemory.CharacterState newState);
    
    /**
     * Add an observation to a character's memory.
     */
    CompletableFuture<CharacterMemory> addObservation(String projectId, String characterId, 
            CharacterMemory.CharacterObservation observation);
    
    /**
     * Add a reflection to a character's memory.
     */
    CompletableFuture<CharacterMemory> addReflection(String projectId, String characterId,
            CharacterMemory.CharacterReflection reflection);
    
    /**
     * Get all characters for a project.
     */
    CompletableFuture<List<CharacterMemory>> getProjectCharacters(String projectId);
    
    /**
     * Get characters present in a specific scene.
     */
    CompletableFuture<List<CharacterMemory>> getSceneCharacters(String projectId, String sceneId);
    
    /**
     * Get character timeline (observations ordered by scene).
     */
    CompletableFuture<List<CharacterMemory.CharacterObservation>> getCharacterTimeline(
            String projectId, String characterId, int limit);
    
    /**
     * Update character relationships.
     */
    CompletableFuture<CharacterMemory> updateRelationships(String projectId, String characterId,
            String otherCharacterId, String relationshipType);
    
    /**
     * Delete a character's memory.
     */
    CompletableFuture<Void> deleteCharacterMemory(String projectId, String characterId);
}