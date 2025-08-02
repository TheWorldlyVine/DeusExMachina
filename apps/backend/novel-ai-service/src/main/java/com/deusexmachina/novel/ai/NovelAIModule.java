package com.deusexmachina.novel.ai;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import java.util.logging.Logger;

/**
 * Guice module for dependency injection configuration.
 */
public class NovelAIModule extends AbstractModule {
    private static final Logger logger = Logger.getLogger(NovelAIModule.class.getName());
    
    @Override
    protected void configure() {
        // Bind interfaces to implementations
        // TODO: Add bindings for AI generation services
        logger.info("Configuring Novel AI Module");
    }
    
    @Provides
    @Singleton
    public Firestore provideFirestore() {
        logger.info("Initializing Firestore client");
        return FirestoreOptions.getDefaultInstance().getService();
    }
    
    // TODO: Add Vertex AI client provider
}