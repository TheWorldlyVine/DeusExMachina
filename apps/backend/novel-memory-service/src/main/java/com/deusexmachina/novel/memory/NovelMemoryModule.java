package com.deusexmachina.novel.memory;

import com.deusexmachina.novel.memory.controllers.MemoryController;
import com.deusexmachina.novel.memory.services.*;
import com.deusexmachina.novel.memory.services.impl.*;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.logging.Logger;

/**
 * Guice module for dependency injection configuration.
 */
public class NovelMemoryModule extends AbstractModule {
    private static final Logger logger = Logger.getLogger(NovelMemoryModule.class.getName());
    
    @Override
    protected void configure() {
        // Bind service interfaces to implementations
        bind(CharacterMemoryService.class).to(CharacterMemoryServiceImpl.class).in(Singleton.class);
        bind(PlotMemoryService.class).to(PlotMemoryServiceImpl.class).in(Singleton.class);
        bind(WorldMemoryService.class).to(WorldMemoryServiceImpl.class).in(Singleton.class);
        bind(ContextService.class).to(ContextServiceImpl.class).in(Singleton.class);
        
        // Bind controller
        bind(MemoryController.class).in(Singleton.class);
        
        logger.info("Configured Novel Memory Module with all service bindings");
    }
    
    @Provides
    @Singleton
    public Firestore provideFirestore() {
        logger.info("Initializing Firestore client");
        return FirestoreOptions.getDefaultInstance().getService();
    }
    
    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();
    }
}