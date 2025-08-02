package com.deusexmachina.novel.memory.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.deusexmachina.novel.memory.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MemoryController {
    private static final Logger logger = LoggerFactory.getLogger(MemoryController.class);
    
    private final MemoryService memoryService;
    
    @Inject
    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }
    
    // Controller methods will be implemented as we build out the service
}