package com.deusexmachina.novel.memory.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryTest {
    
    @Test
    void testMemoryBuilder() {
        // Given
        Instant now = Instant.now();
        
        // When
        Memory memory = Memory.builder()
                .id("memory-123")
                .contextId("context-456")
                .type(MemoryType.STATE)
                .content("The knight drew his sword")
                .timestamp(now)
                .entityId("character-001")
                .entityType("character")
                .relevanceScore(0.9)
                .importance(0.8)
                .tags(Arrays.asList("action", "combat"))
                .metadata(Map.of("weapon", "sword"))
                .chapterRef("chapter-1")
                .sceneRef("scene-3")
                .build();
        
        // Then
        assertThat(memory.getId()).isEqualTo("memory-123");
        assertThat(memory.getContextId()).isEqualTo("context-456");
        assertThat(memory.getType()).isEqualTo(MemoryType.STATE);
        assertThat(memory.getContent()).isEqualTo("The knight drew his sword");
        assertThat(memory.getTimestamp()).isEqualTo(now);
        assertThat(memory.getEntityId()).isEqualTo("character-001");
        assertThat(memory.getEntityType()).isEqualTo("character");
        assertThat(memory.getRelevanceScore()).isEqualTo(0.9);
        assertThat(memory.getImportance()).isEqualTo(0.8);
        assertThat(memory.getAccessCount()).isEqualTo(0);
        assertThat(memory.getTags()).containsExactly("action", "combat");
        assertThat(memory.getMetadata()).containsEntry("weapon", "sword");
        assertThat(memory.isActive()).isTrue();
    }
    
    @Test
    void testIncrementAccessCount() {
        // Given
        Memory memory = Memory.builder()
                .id("memory-123")
                .contextId("context-456")
                .type(MemoryType.OBSERVATION)
                .content("The castle walls were high")
                .build();
        
        assertThat(memory.getAccessCount()).isEqualTo(0);
        assertThat(memory.getLastAccessed()).isNull();
        
        // When
        memory.incrementAccessCount();
        
        // Then
        assertThat(memory.getAccessCount()).isEqualTo(1);
        assertThat(memory.getLastAccessed()).isNotNull();
        assertThat(memory.getLastAccessed()).isBefore(Instant.now().plusSeconds(1));
        
        // When - increment again
        memory.incrementAccessCount();
        
        // Then
        assertThat(memory.getAccessCount()).isEqualTo(2);
    }
    
    @Test
    void testGetDecayedRelevance_NoAccess() {
        // Given
        Memory memory = Memory.builder()
                .id("memory-123")
                .contextId("context-456")
                .type(MemoryType.REFLECTION)
                .content("I wonder about the prophecy")
                .relevanceScore(0.8)
                .build();
        
        // When
        double decayed = memory.getDecayedRelevance(0.95);
        
        // Then
        assertThat(decayed).isEqualTo(0.8);
    }
    
    @Test
    void testGetDecayedRelevance_RecentAccess() {
        // Given
        Memory memory = Memory.builder()
                .id("memory-123")
                .contextId("context-456")
                .type(MemoryType.CONTEXT)
                .content("The tavern was crowded")
                .relevanceScore(0.8)
                .lastAccessed(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        
        // When
        double decayed = memory.getDecayedRelevance(0.95);
        
        // Then
        // After 1 hour with decay factor 0.95: 0.8 * 0.95^1 = 0.76
        assertThat(decayed).isCloseTo(0.76, org.assertj.core.data.Offset.offset(0.01));
    }
    
    @Test
    void testGetDecayedRelevance_OldAccess() {
        // Given
        Memory memory = Memory.builder()
                .id("memory-123")
                .contextId("context-456")
                .type(MemoryType.EXECUTION)
                .content("The spell was cast")
                .relevanceScore(1.0)
                .lastAccessed(Instant.now().minus(24, ChronoUnit.HOURS))
                .build();
        
        // When
        double decayed = memory.getDecayedRelevance(0.95);
        
        // Then
        // After 24 hours with decay factor 0.95: 1.0 * 0.95^24 ≈ 0.292
        assertThat(decayed).isCloseTo(0.292, org.assertj.core.data.Offset.offset(0.01));
    }
    
    @Test
    void testDefaultValues() {
        // Given/When
        Memory memory = Memory.builder()
                .id("memory-123")
                .contextId("context-456")
                .type(MemoryType.STATE)
                .content("Default test")
                .build();
        
        // Then
        assertThat(memory.getTimestamp()).isNotNull();
        assertThat(memory.getRelevanceScore()).isEqualTo(1.0);
        assertThat(memory.getImportance()).isEqualTo(0.5);
        assertThat(memory.getAccessCount()).isEqualTo(0);
        assertThat(memory.isActive()).isTrue();
    }
}