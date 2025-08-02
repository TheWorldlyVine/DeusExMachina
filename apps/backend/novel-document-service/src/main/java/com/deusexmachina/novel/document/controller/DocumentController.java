package com.deusexmachina.novel.document.controller;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.deusexmachina.novel.document.model.Document;
import com.deusexmachina.novel.document.model.Chapter;
import com.deusexmachina.novel.document.model.Scene;
import com.deusexmachina.novel.document.model.DocumentStatus;
import com.deusexmachina.novel.document.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class DocumentController {
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .create();
    
    private final DocumentService documentService;
    
    @Inject
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
        logger.info("DocumentController initialized");
    }
    
    public void createDocument(HttpRequest request, HttpResponse response) throws IOException {
        try {
            String body = readRequestBody(request);
            Map<String, Object> docMap = gson.fromJson(body, Map.class);
            
            // Build document with defaults
            Document.DocumentBuilder builder = Document.builder()
                    .id(docMap.get("id") != null ? (String)docMap.get("id") : UUID.randomUUID().toString())
                    .contextId(docMap.get("contextId") != null ? (String)docMap.get("contextId") : UUID.randomUUID().toString())
                    .title((String)docMap.get("title"))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .active(true);
            
            // Set optional fields
            if (docMap.get("subtitle") != null) builder.subtitle((String)docMap.get("subtitle"));
            if (docMap.get("description") != null) builder.description((String)docMap.get("description"));
            if (docMap.get("authorId") != null) builder.authorId((String)docMap.get("authorId"));
            if (docMap.get("authorName") != null) builder.authorName((String)docMap.get("authorName"));
            if (docMap.get("genre") != null) builder.genre((String)docMap.get("genre"));
            if (docMap.get("tags") != null) builder.tags((List<String>)docMap.get("tags"));
            
            Document document = builder.build();
            
            Document created = documentService.createDocument(document);
            sendSuccessResponse(response, created, 201);
        } catch (JsonSyntaxException e) {
            sendErrorResponse(response, "Invalid JSON format", 400);
        } catch (Exception e) {
            logger.error("Error creating document", e);
            sendErrorResponse(response, "Failed to create document: " + e.getMessage(), 500);
        }
    }
    
    public void getDocument(String documentId, HttpResponse response) throws IOException {
        try {
            Document document = documentService.getDocument(documentId);
            if (document == null) {
                sendErrorResponse(response, "Document not found", 404);
                return;
            }
            sendSuccessResponse(response, document, 200);
        } catch (Exception e) {
            logger.error("Error getting document", e);
            sendErrorResponse(response, "Failed to get document: " + e.getMessage(), 500);
        }
    }
    
    public void updateDocument(String documentId, HttpRequest request, HttpResponse response) throws IOException {
        try {
            String body = readRequestBody(request);
            Document updates = gson.fromJson(body, Document.class);
            updates.setId(documentId);
            updates.setUpdatedAt(Instant.now());
            
            Document updated = documentService.updateDocument(updates);
            if (updated == null) {
                sendErrorResponse(response, "Document not found", 404);
                return;
            }
            sendSuccessResponse(response, updated, 200);
        } catch (JsonSyntaxException e) {
            sendErrorResponse(response, "Invalid JSON format", 400);
        } catch (Exception e) {
            logger.error("Error updating document", e);
            sendErrorResponse(response, "Failed to update document: " + e.getMessage(), 500);
        }
    }
    
    public void deleteDocument(String documentId, HttpResponse response) throws IOException {
        try {
            boolean deleted = documentService.deleteDocument(documentId);
            if (!deleted) {
                sendErrorResponse(response, "Document not found", 404);
                return;
            }
            sendSuccessResponse(response, Map.of("message", "Document deleted successfully"), 200);
        } catch (Exception e) {
            logger.error("Error deleting document", e);
            sendErrorResponse(response, "Failed to delete document: " + e.getMessage(), 500);
        }
    }
    
    public void listDocuments(String userId, HttpResponse response) throws IOException {
        try {
            List<Document> documents = documentService.listDocumentsByUser(userId);
            sendSuccessResponse(response, Map.of("documents", documents, "count", documents.size()), 200);
        } catch (Exception e) {
            logger.error("Error listing documents", e);
            sendErrorResponse(response, "Failed to list documents: " + e.getMessage(), 500);
        }
    }
    
    public void createChapter(String documentId, HttpRequest request, HttpResponse response) throws IOException {
        try {
            String body = readRequestBody(request);
            Chapter chapter = gson.fromJson(body, Chapter.class);
            
            // Set defaults
            if (chapter.getId() == null) {
                chapter.setId(UUID.randomUUID().toString());
            }
            chapter.setDocumentId(documentId);
            chapter.setCreatedAt(Instant.now());
            chapter.setUpdatedAt(Instant.now());
            chapter.setActive(true);
            
            Chapter created = documentService.createChapter(chapter);
            sendSuccessResponse(response, created, 201);
        } catch (JsonSyntaxException e) {
            sendErrorResponse(response, "Invalid JSON format", 400);
        } catch (Exception e) {
            logger.error("Error creating chapter", e);
            sendErrorResponse(response, "Failed to create chapter: " + e.getMessage(), 500);
        }
    }
    
    public void updateChapter(String documentId, String chapterNumber, HttpRequest request, HttpResponse response) throws IOException {
        try {
            String body = readRequestBody(request);
            Chapter updates = gson.fromJson(body, Chapter.class);
            updates.setDocumentId(documentId);
            updates.setChapterNumber(Integer.parseInt(chapterNumber));
            updates.setUpdatedAt(Instant.now());
            
            Chapter updated = documentService.updateChapter(updates);
            if (updated == null) {
                sendErrorResponse(response, "Chapter not found", 404);
                return;
            }
            sendSuccessResponse(response, updated, 200);
        } catch (JsonSyntaxException e) {
            sendErrorResponse(response, "Invalid JSON format", 400);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid chapter number", 400);
        } catch (Exception e) {
            logger.error("Error updating chapter", e);
            sendErrorResponse(response, "Failed to update chapter: " + e.getMessage(), 500);
        }
    }
    
    public void deleteChapter(String documentId, String chapterNumber, HttpResponse response) throws IOException {
        try {
            int chapterNum = Integer.parseInt(chapterNumber);
            boolean deleted = documentService.deleteChapter(documentId, chapterNum);
            if (!deleted) {
                sendErrorResponse(response, "Chapter not found", 404);
                return;
            }
            sendSuccessResponse(response, Map.of("message", "Chapter deleted successfully"), 200);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid chapter number", 400);
        } catch (Exception e) {
            logger.error("Error deleting chapter", e);
            sendErrorResponse(response, "Failed to delete chapter: " + e.getMessage(), 500);
        }
    }
    
    public void createScene(String documentId, String chapterNumber, HttpRequest request, HttpResponse response) throws IOException {
        try {
            String body = readRequestBody(request);
            Scene scene = gson.fromJson(body, Scene.class);
            
            // Set defaults
            if (scene.getId() == null) {
                scene.setId(UUID.randomUUID().toString());
            }
            scene.setDocumentId(documentId);
            scene.setCreatedAt(Instant.now());
            scene.setUpdatedAt(Instant.now());
            scene.setActive(true);
            
            // Get chapter to set chapterId
            int chapterNum = Integer.parseInt(chapterNumber);
            Chapter chapter = documentService.getChapterByNumber(documentId, chapterNum);
            if (chapter == null) {
                sendErrorResponse(response, "Chapter not found", 404);
                return;
            }
            scene.setChapterId(chapter.getId());
            
            Scene created = documentService.createScene(scene);
            sendSuccessResponse(response, created, 201);
        } catch (JsonSyntaxException e) {
            sendErrorResponse(response, "Invalid JSON format", 400);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid chapter number", 400);
        } catch (Exception e) {
            logger.error("Error creating scene", e);
            sendErrorResponse(response, "Failed to create scene: " + e.getMessage(), 500);
        }
    }
    
    public void updateScene(String documentId, String chapterNumber, String sceneNumber, HttpRequest request, HttpResponse response) throws IOException {
        try {
            String body = readRequestBody(request);
            Scene updates = gson.fromJson(body, Scene.class);
            updates.setDocumentId(documentId);
            updates.setSceneNumber(Integer.parseInt(sceneNumber));
            updates.setUpdatedAt(Instant.now());
            
            // Get chapter to validate
            int chapterNum = Integer.parseInt(chapterNumber);
            Chapter chapter = documentService.getChapterByNumber(documentId, chapterNum);
            if (chapter == null) {
                sendErrorResponse(response, "Chapter not found", 404);
                return;
            }
            updates.setChapterId(chapter.getId());
            
            Scene updated = documentService.updateScene(updates);
            if (updated == null) {
                sendErrorResponse(response, "Scene not found", 404);
                return;
            }
            sendSuccessResponse(response, updated, 200);
        } catch (JsonSyntaxException e) {
            sendErrorResponse(response, "Invalid JSON format", 400);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid chapter or scene number", 400);
        } catch (Exception e) {
            logger.error("Error updating scene", e);
            sendErrorResponse(response, "Failed to update scene: " + e.getMessage(), 500);
        }
    }
    
    public void deleteScene(String documentId, String chapterNumber, String sceneNumber, HttpResponse response) throws IOException {
        try {
            int chapterNum = Integer.parseInt(chapterNumber);
            int sceneNum = Integer.parseInt(sceneNumber);
            
            boolean deleted = documentService.deleteScene(documentId, chapterNum, sceneNum);
            if (!deleted) {
                sendErrorResponse(response, "Scene not found", 404);
                return;
            }
            sendSuccessResponse(response, Map.of("message", "Scene deleted successfully"), 200);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid chapter or scene number", 400);
        } catch (Exception e) {
            logger.error("Error deleting scene", e);
            sendErrorResponse(response, "Failed to delete scene: " + e.getMessage(), 500);
        }
    }
    
    private String readRequestBody(HttpRequest request) throws IOException {
        return new BufferedReader(new InputStreamReader(request.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));
    }
    
    private void sendSuccessResponse(HttpResponse response, Object data, int statusCode) throws IOException {
        response.setStatusCode(statusCode);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            gson.toJson(data, writer);
        }
    }
    
    private void sendErrorResponse(HttpResponse response, String message, int statusCode) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("statusCode", statusCode);
        error.put("timestamp", Instant.now());
        
        response.setStatusCode(statusCode);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            gson.toJson(error, writer);
        }
    }
    
    private static class InstantTypeAdapter extends com.google.gson.TypeAdapter<Instant> {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, Instant value) throws IOException {
            out.value(value.toString());
        }
        
        @Override
        public Instant read(com.google.gson.stream.JsonReader in) throws IOException {
            return Instant.parse(in.nextString());
        }
    }
}