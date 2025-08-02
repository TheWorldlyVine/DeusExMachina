package com.deusexmachina.novel.ai;

import com.deusexmachina.novel.ai.controller.GenerationController;
import com.deusexmachina.novel.ai.service.GenerationService;
import com.deusexmachina.novel.ai.service.impl.GeminiGenerationService;
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
        bind(GenerationService.class).to(GeminiGenerationService.class).in(Singleton.class);
        bind(GenerationController.class).in(Singleton.class);
        
        logger.info("Configuring Novel AI Module");
    }
    
    @Provides
    @Singleton
    public Firestore provideFirestore() {
        logger.info("Initializing Firestore client");
        return FirestoreOptions.getDefaultInstance().getService();
    }
}