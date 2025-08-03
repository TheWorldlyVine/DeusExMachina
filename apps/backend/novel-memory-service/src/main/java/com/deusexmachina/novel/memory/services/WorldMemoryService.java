package com.deusexmachina.novel.memory.services;

import com.deusexmachina.novel.memory.models.WorldMemory;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing world memory operations.
 */
public interface WorldMemoryService {
    
    /**
     * Save or update world memory.
     */
    CompletableFuture<WorldMemory> saveWorldMemory(WorldMemory memory);
    
    /**
     * Get all world memory for a project.
     */
    CompletableFuture<List<WorldMemory>> getProjectWorldMemory(String projectId);
    
    /**
     * Get world memory by category.
     */
    CompletableFuture<List<WorldMemory>> getWorldMemoryByCategory(String projectId, String category);
    
    /**
     * Add a world fact.
     */
    CompletableFuture<WorldMemory> addWorldFact(String projectId, String category,
            WorldMemory.WorldFact fact);
    
    /**
     * Add a location.
     */
    CompletableFuture<WorldMemory> addLocation(String projectId,
            WorldMemory.Location location);
    
    /**
     * Get location details.
     */
    CompletableFuture<WorldMemory.Location> getLocation(String projectId, String locationId);
    
    /**
     * Add historical event.
     */
    CompletableFuture<WorldMemory> addHistoricalEvent(String projectId,
            WorldMemory.HistoricalEvent event);
    
    /**
     * Validate world consistency.
     */
    CompletableFuture<List<WorldMemory.Contradiction>> validateConsistency(String projectId);
    
    /**
     * Add detected contradiction.
     */
    CompletableFuture<WorldMemory> addContradiction(String projectId,
            WorldMemory.Contradiction contradiction);
    
    /**
     * Search world facts.
     */
    CompletableFuture<List<WorldMemory.WorldFact>> searchFacts(String projectId, String query);
    
    /**
     * Get facts relevant to a location.
     */
    CompletableFuture<List<WorldMemory.WorldFact>> getLocationFacts(String projectId, String locationId);
    
    /**
     * Delete world memory category.
     */
    CompletableFuture<Void> deleteWorldMemory(String projectId, String worldId);
}