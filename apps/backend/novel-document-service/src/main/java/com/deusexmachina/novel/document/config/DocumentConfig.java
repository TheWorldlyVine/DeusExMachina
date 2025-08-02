package com.deusexmachina.novel.document.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentConfig {
    private final int maxSizeMb;
    private final int maxChapters;
    private final int maxScenesPerChapter;
    private final int maxWordsPerScene;
    private final boolean versioningEnabled;
    private final int maxVersions;
    private final int cacheTtl;
    private final int cacheMaxSize;
    private final String chunkingStrategy;
    private final int chunkOverlap;
    private final String storageBucket;
}