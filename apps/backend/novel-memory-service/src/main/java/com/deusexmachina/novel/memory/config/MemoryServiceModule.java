package com.deusexmachina.novel.memory.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.deusexmachina.novel.memory.controller.MemoryController;
import com.deusexmachina.novel.memory.service.MemoryService;
import com.deusexmachina.novel.memory.service.impl.FirestoreMemoryService;
import com.deusexmachina.novel.memory.repository.MemoryRepository;
import com.deusexmachina.novel.memory.repository.impl.FirestoreMemoryRepository;

public class MemoryServiceModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(MemoryService.class).to(FirestoreMemoryService.class).in(Singleton.class);
        bind(MemoryRepository.class).to(FirestoreMemoryRepository.class).in(Singleton.class);
        bind(MemoryController.class).in(Singleton.class);
    }
    
    @Provides
    @Singleton
    Config provideConfig() {
        String configFile = System.getProperty("config.file");
        if (configFile != null) {
            return ConfigFactory.parseFile(new java.io.File(configFile))
                    .withFallback(ConfigFactory.load());
        }
        return ConfigFactory.load();
    }
    
    @Provides
    @Singleton
    FirestoreConfig provideFirestoreConfig(Config config) {
        return FirestoreConfig.builder()
                .projectId(getEnvOrConfig(config, "GCP_PROJECT_ID", "gcp.project.id"))
                .databaseId(config.getString("firestore.database.id"))
                .memoriesCollection(config.getString("firestore.collections.memories"))
                .charactersCollection(config.getString("firestore.collections.characters"))
                .plotsCollection(config.getString("firestore.collections.plots"))
                .worldFactsCollection(config.getString("firestore.collections.worldFacts"))
                .contextsCollection(config.getString("firestore.collections.contexts"))
                .build();
    }
    
    @Provides
    @Singleton
    MemoryConfig provideMemoryConfig(Config config) {
        return MemoryConfig.builder()
                .decayFactor(config.getDouble("memory.score.decayFactor"))
                .minRelevance(config.getDouble("memory.score.minRelevance"))
                .maxItems(config.getInt("memory.score.maxItems"))
                .cacheTtl(config.getInt("memory.cache.ttl"))
                .cacheMaxSize(config.getInt("memory.cache.maxSize"))
                .batchSize(config.getInt("memory.batch.size"))
                .batchTimeout(config.getInt("memory.batch.timeout"))
                .searchMaxResults(config.getInt("search.maxResults"))
                .searchTimeout(config.getInt("search.timeout"))
                .build();
    }
    
    @Provides
    @Singleton
    Firestore provideFirestore(FirestoreConfig config) {
        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId(config.getProjectId())
                .setDatabaseId(config.getDatabaseId())
                .build();
        
        return options.getService();
    }
    
    private String getEnvOrConfig(Config config, String envKey, String configPath) {
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : config.getString(configPath);
    }
}