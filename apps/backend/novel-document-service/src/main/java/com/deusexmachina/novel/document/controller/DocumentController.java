package com.deusexmachina.novel.document.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.deusexmachina.novel.document.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DocumentController {
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    
    private final DocumentService documentService;
    
    @Inject
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    // Controller methods will be implemented as we build out the service
}