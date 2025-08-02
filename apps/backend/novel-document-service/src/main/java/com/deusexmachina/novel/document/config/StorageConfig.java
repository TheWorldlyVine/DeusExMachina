package com.deusexmachina.novel.document.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageConfig {
    private final String projectId;
    private final String bucketName;
    private final int chunkSizeKb;
    private final boolean compressionEnabled;
}