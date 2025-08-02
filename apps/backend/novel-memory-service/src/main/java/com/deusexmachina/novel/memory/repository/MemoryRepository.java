package com.deusexmachina.novel.memory.repository;

import com.deusexmachina.novel.memory.model.Memory;
import com.deusexmachina.novel.memory.model.MemoryType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface MemoryRepository {
    
    CompletableFuture<Memory> save(Memory memory);
    
    CompletableFuture<Optional<Memory>> findById(String id);
    
    CompletableFuture<List<Memory>> findByContextId(String contextId, int limit);
    
    CompletableFuture<List<Memory>> findByContextIdAndType(String contextId, MemoryType type, int limit);
    
    CompletableFuture<List<Memory>> findByEntityId(String entityId, int limit);
    
    CompletableFuture<Memory> update(Memory memory);
    
    CompletableFuture<Void> delete(String id);
    
    CompletableFuture<List<Memory>> searchByEmbedding(String contextId, String embeddingVector, int limit);
}