package com.deusexmachina.novel.document.service;

import com.deusexmachina.novel.document.model.Document;
import com.deusexmachina.novel.document.model.Chapter;
import com.deusexmachina.novel.document.model.Scene;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DocumentService {
    
    // Document operations
    CompletableFuture<Document> createDocument(Document document);
    CompletableFuture<Optional<Document>> getDocument(String documentId);
    CompletableFuture<Document> updateDocument(String documentId, Document document);
    CompletableFuture<Void> deleteDocument(String documentId);
    CompletableFuture<List<Document>> getDocumentsByContext(String contextId);
    
    // Chapter operations
    CompletableFuture<Chapter> createChapter(String documentId, Chapter chapter);
    CompletableFuture<Optional<Chapter>> getChapter(String chapterId);
    CompletableFuture<List<Chapter>> getChaptersByDocument(String documentId);
    CompletableFuture<Chapter> updateChapter(String chapterId, Chapter chapter);
    CompletableFuture<Void> deleteChapter(String chapterId);
    
    // Scene operations
    CompletableFuture<Scene> createScene(String chapterId, Scene scene);
    CompletableFuture<Optional<Scene>> getScene(String sceneId);
    CompletableFuture<List<Scene>> getScenesByChapter(String chapterId);
    CompletableFuture<Scene> updateScene(String sceneId, Scene scene);
    CompletableFuture<Void> deleteScene(String sceneId);
    
    // Content operations
    CompletableFuture<String> getFullDocumentContent(String documentId);
    CompletableFuture<String> exportDocument(String documentId, String format);
}