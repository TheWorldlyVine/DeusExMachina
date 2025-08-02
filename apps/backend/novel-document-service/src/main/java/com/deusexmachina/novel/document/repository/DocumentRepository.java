package com.deusexmachina.novel.document.repository;

import com.deusexmachina.novel.document.model.Document;
import com.deusexmachina.novel.document.model.Chapter;
import com.deusexmachina.novel.document.model.Scene;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DocumentRepository {
    
    // Document operations
    CompletableFuture<Document> saveDocument(Document document);
    CompletableFuture<Optional<Document>> findDocumentById(String id);
    CompletableFuture<List<Document>> findDocumentsByContextId(String contextId);
    CompletableFuture<Document> updateDocument(Document document);
    CompletableFuture<Void> deleteDocument(String id);
    
    // Chapter operations
    CompletableFuture<Chapter> saveChapter(Chapter chapter);
    CompletableFuture<Optional<Chapter>> findChapterById(String id);
    CompletableFuture<List<Chapter>> findChaptersByDocumentId(String documentId);
    CompletableFuture<Chapter> updateChapter(Chapter chapter);
    CompletableFuture<Void> deleteChapter(String id);
    
    // Scene operations
    CompletableFuture<Scene> saveScene(Scene scene);
    CompletableFuture<Optional<Scene>> findSceneById(String id);
    CompletableFuture<List<Scene>> findScenesByChapterId(String chapterId);
    CompletableFuture<Scene> updateScene(Scene scene);
    CompletableFuture<Void> deleteScene(String id);
}