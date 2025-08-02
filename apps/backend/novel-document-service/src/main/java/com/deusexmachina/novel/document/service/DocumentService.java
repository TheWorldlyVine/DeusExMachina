package com.deusexmachina.novel.document.service;

import com.deusexmachina.novel.document.model.Document;
import com.deusexmachina.novel.document.model.Chapter;
import com.deusexmachina.novel.document.model.Scene;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DocumentService {
    
    // Document operations
    Document createDocument(Document document);
    Document getDocument(String documentId);
    Document updateDocument(Document document);
    boolean deleteDocument(String documentId);
    List<Document> listDocumentsByUser(String userId);
    List<Document> getDocumentsByContext(String contextId);
    
    // Chapter operations
    Chapter createChapter(Chapter chapter);
    Chapter getChapter(String chapterId);
    Chapter getChapterByNumber(String documentId, int chapterNumber);
    List<Chapter> getChaptersByDocument(String documentId);
    Chapter updateChapter(Chapter chapter);
    boolean deleteChapter(String documentId, int chapterNumber);
    
    // Scene operations
    Scene createScene(Scene scene);
    Scene getScene(String sceneId);
    Scene getSceneByNumber(String chapterId, int sceneNumber);
    List<Scene> getScenesByChapter(String chapterId);
    Scene updateScene(Scene scene);
    boolean deleteScene(String documentId, int chapterNumber, int sceneNumber);
    
    // Content operations
    String getFullDocumentContent(String documentId);
    String exportDocument(String documentId, String format);
    
    // Async versions for heavy operations
    CompletableFuture<String> getFullDocumentContentAsync(String documentId);
    CompletableFuture<String> exportDocumentAsync(String documentId, String format);
}