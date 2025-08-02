package com.deusexmachina.novel.memory.service;

import com.deusexmachina.novel.memory.model.Memory;
import com.deusexmachina.novel.memory.model.MemoryType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface MemoryService {
    
    /**
     * Store a new memory.
     */
    CompletableFuture<Memory> storeMemory(Memory memory);
    
    /**
     * Retrieve a memory by ID.
     */
    CompletableFuture<Optional<Memory>> getMemory(String memoryId);
    
    /**
     * Retrieve memories for a specific context.
     */
    CompletableFuture<List<Memory>> getMemoriesForContext(String contextId, int limit);
    
    /**
     * Retrieve memories by type for a context.
     */
    CompletableFuture<List<Memory>> getMemoriesByType(String contextId, MemoryType type, int limit);
    
    /**
     * Search memories by content similarity.
     */
    CompletableFuture<List<Memory>> searchMemories(String contextId, String query, int limit);
    
    /**
     * Update an existing memory.
     */
    CompletableFuture<Memory> updateMemory(String memoryId, Memory memory);
    
    /**
     * Delete a memory.
     */
    CompletableFuture<Void> deleteMemory(String memoryId);
    
    /**
     * Get relevant memories for generation.
     */
    CompletableFuture<List<Memory>> getRelevantMemories(String contextId, String prompt, int limit);
}