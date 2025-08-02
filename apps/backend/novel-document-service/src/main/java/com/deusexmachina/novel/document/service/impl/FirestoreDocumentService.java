package com.deusexmachina.novel.document.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.deusexmachina.novel.document.config.DocumentConfig;
import com.deusexmachina.novel.document.model.Document;
import com.deusexmachina.novel.document.model.Chapter;
import com.deusexmachina.novel.document.model.Scene;
import com.deusexmachina.novel.document.repository.DocumentRepository;
import com.deusexmachina.novel.document.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class FirestoreDocumentService implements DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreDocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final DocumentConfig documentConfig;
    
    @Inject
    public FirestoreDocumentService(DocumentRepository documentRepository, DocumentConfig documentConfig) {
        this.documentRepository = documentRepository;
        this.documentConfig = documentConfig;
        logger.info("FirestoreDocumentService initialized");
    }
    
    @Override
    public CompletableFuture<Document> createDocument(Document document) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Optional<Document>> getDocument(String documentId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Document> updateDocument(String documentId, Document document) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Void> deleteDocument(String documentId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<List<Document>> getDocumentsByContext(String contextId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Chapter> createChapter(String documentId, Chapter chapter) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Optional<Chapter>> getChapter(String chapterId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<List<Chapter>> getChaptersByDocument(String documentId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Chapter> updateChapter(String chapterId, Chapter chapter) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Void> deleteChapter(String chapterId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Scene> createScene(String chapterId, Scene scene) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Optional<Scene>> getScene(String sceneId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<List<Scene>> getScenesByChapter(String chapterId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Scene> updateScene(String sceneId, Scene scene) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<Void> deleteScene(String sceneId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<String> getFullDocumentContent(String documentId) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
    
    @Override
    public CompletableFuture<String> exportDocument(String documentId, String format) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
    }
}