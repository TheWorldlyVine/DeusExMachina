package com.deusexmachina.novel.document.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FirestoreConfig {
    private final String projectId;
    private final String databaseId;
    private final String documentsCollection;
    private final String chaptersCollection;
    private final String scenesCollection;
    private final String versionsCollection;
    private final String metadataCollection;
}