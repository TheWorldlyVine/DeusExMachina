package com.deusexmachina.novel.memory.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemoryConfig {
    private final double decayFactor;
    private final double minRelevance;
    private final int maxItems;
    private final int cacheTtl;
    private final int cacheMaxSize;
    private final int batchSize;
    private final int batchTimeout;
    private final int searchMaxResults;
    private final int searchTimeout;
}