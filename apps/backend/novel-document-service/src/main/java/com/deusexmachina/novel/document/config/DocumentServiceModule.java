package com.deusexmachina.novel.document.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.deusexmachina.novel.document.controller.DocumentController;
import com.deusexmachina.novel.document.service.DocumentService;
import com.deusexmachina.novel.document.service.impl.FirestoreDocumentService;
import com.deusexmachina.novel.document.repository.DocumentRepository;
import com.deusexmachina.novel.document.repository.impl.FirestoreDocumentRepository;

public class DocumentServiceModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(DocumentService.class).to(FirestoreDocumentService.class).in(Singleton.class);
        bind(DocumentRepository.class).to(FirestoreDocumentRepository.class).in(Singleton.class);
        bind(DocumentController.class).in(Singleton.class);
    }
    
    @Provides
    @Singleton
    Config provideConfig() {
        try {
            String configFile = System.getProperty("config.file");
            if (configFile != null) {
                return ConfigFactory.parseFile(new java.io.File(configFile))
                        .withFallback(ConfigFactory.load());
            }
            return ConfigFactory.load();
        } catch (Exception e) {
            // If config loading fails, return default config
            return ConfigFactory.empty()
                    .withFallback(ConfigFactory.parseString(getDefaultConfig()));
        }
    }
    
    private String getDefaultConfig() {
        return """
            gcp.project.id = "default-project"
            firestore.database.id = "(default)"
            firestore.collections.documents = "novel_documents"
            firestore.collections.chapters = "novel_chapters"
            firestore.collections.scenes = "novel_scenes"
            firestore.collections.versions = "novel_versions"
            firestore.collections.metadata = "novel_metadata"
            storage.bucket.name = "novel-documents-storage"
            storage.chunk.sizeKb = 512
            storage.chunk.compressionEnabled = true
            document.limits.maxSizeMb = 50
            document.limits.maxChapters = 100
            document.limits.maxScenesPerChapter = 50
            document.limits.maxWordsPerScene = 10000
            document.versioning.enabled = true
            document.versioning.maxVersions = 10
            document.cache.ttl = 3600
            document.cache.maxSize = 100
            chunking.strategy = "SEMANTIC"
            chunking.overlap = 50
            """;
    }
    
    @Provides
    @Singleton
    FirestoreConfig provideFirestoreConfig(Config config) {
        return FirestoreConfig.builder()
                .projectId(getEnvOrConfig(config, "GCP_PROJECT_ID", "gcp.project.id"))
                .databaseId(config.getString("firestore.database.id"))
                .documentsCollection(config.getString("firestore.collections.documents"))
                .chaptersCollection(config.getString("firestore.collections.chapters"))
                .scenesCollection(config.getString("firestore.collections.scenes"))
                .versionsCollection(config.getString("firestore.collections.versions"))
                .metadataCollection(config.getString("firestore.collections.metadata"))
                .build();
    }
    
    @Provides
    @Singleton
    StorageConfig provideStorageConfig(Config config) {
        return StorageConfig.builder()
                .projectId(getEnvOrConfig(config, "GCP_PROJECT_ID", "gcp.project.id"))
                .bucketName(config.getString("storage.bucket.name"))
                .chunkSizeKb(config.getInt("storage.chunk.sizeKb"))
                .compressionEnabled(config.getBoolean("storage.chunk.compressionEnabled"))
                .build();
    }
    
    @Provides
    @Singleton
    DocumentConfig provideDocumentConfig(Config config) {
        return DocumentConfig.builder()
                .maxSizeMb(config.getInt("document.limits.maxSizeMb"))
                .maxChapters(config.getInt("document.limits.maxChapters"))
                .maxScenesPerChapter(config.getInt("document.limits.maxScenesPerChapter"))
                .maxWordsPerScene(config.getInt("document.limits.maxWordsPerScene"))
                .versioningEnabled(config.getBoolean("document.versioning.enabled"))
                .maxVersions(config.getInt("document.versioning.maxVersions"))
                .cacheTtl(config.getInt("document.cache.ttl"))
                .cacheMaxSize(config.getInt("document.cache.maxSize"))
                .chunkingStrategy(config.getString("chunking.strategy"))
                .chunkOverlap(config.getInt("chunking.overlap"))
                .storageBucket(config.getString("storage.bucket.name"))
                .firestoreDocumentsCollection(config.getString("firestore.collections.documents"))
                .firestoreChaptersCollection(config.getString("firestore.collections.chapters"))
                .firestoreScenesCollection(config.getString("firestore.collections.scenes"))
                .build();
    }
    
    @Provides
    @Singleton
    Firestore provideFirestore(FirestoreConfig config) {
        try {
            String projectId = config.getProjectId();
            if (projectId == null || projectId.isEmpty() || "default-project".equals(projectId)) {
                throw new IllegalStateException("GCP_PROJECT_ID is not properly configured. Got: " + projectId);
            }
            
            FirestoreOptions options = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setDatabaseId(config.getDatabaseId())
                    .build();
            
            Firestore firestore = options.getService();
            
            // Test the connection
            firestore.listCollections();
            
            return firestore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Firestore: " + e.getMessage(), e);
        }
    }
    
    @Provides
    @Singleton
    Storage provideStorage(StorageConfig config) {
        StorageOptions options = StorageOptions.newBuilder()
                .setProjectId(config.getProjectId())
                .build();
        
        return options.getService();
    }
    
    private String getEnvOrConfig(Config config, String envKey, String configPath) {
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : config.getString(configPath);
    }
}