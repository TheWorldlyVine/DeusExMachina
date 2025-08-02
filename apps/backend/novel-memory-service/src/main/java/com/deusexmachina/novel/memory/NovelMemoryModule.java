package com.deusexmachina.novel.memory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import java.util.logging.Logger;

/**
 * Guice module for dependency injection configuration.
 */
public class NovelMemoryModule extends AbstractModule {
    private static final Logger logger = Logger.getLogger(NovelMemoryModule.class.getName());
    
    @Override
    protected void configure() {
        // Bind interfaces to implementations
        // TODO: Add bindings as services are implemented
        logger.info("Configuring Novel Memory Module");
    }
    
    @Provides
    @Singleton
    public Firestore provideFirestore() {
        logger.info("Initializing Firestore client");
        return FirestoreOptions.getDefaultInstance().getService();
    }
}