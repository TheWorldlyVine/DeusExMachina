package com.deusexmachina.novel.memory.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FirestoreConfig {
    private final String projectId;
    private final String databaseId;
    private final String memoriesCollection;
    private final String charactersCollection;
    private final String plotsCollection;
    private final String worldFactsCollection;
    private final String contextsCollection;
}