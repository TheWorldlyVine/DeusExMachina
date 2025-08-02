package com.deusexmachina.novel.document;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.deusexmachina.novel.document.config.DocumentServiceModule;
import com.deusexmachina.novel.document.controller.DocumentController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NovelDocumentFunction implements HttpFunction {
    private static final Logger logger = LoggerFactory.getLogger(NovelDocumentFunction.class);
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://app.deusexmachina.com",
            "https://deusexmachina.com",
            "http://localhost:3000"
    );
    
    private Injector injector;
    private DocumentController documentController;
    
    public NovelDocumentFunction() {
        try {
            logger.info("Initializing NovelDocumentFunction...");
            this.injector = Guice.createInjector(new DocumentServiceModule());
            logger.info("Guice injector created successfully");
            
            // Delay controller initialization to first request to avoid startup timeout
            logger.info("NovelDocumentFunction initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize NovelDocumentFunction", e);
            // Don't throw - let the function start and return errors on requests
        }
    }
    
    private synchronized DocumentController getDocumentController() {
        if (documentController == null && injector != null) {
            try {
                documentController = injector.getInstance(DocumentController.class);
                logger.info("DocumentController initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize DocumentController", e);
            }
        }
        return documentController;
    }
    
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        // Set CORS headers
        handleCors(request, response);
        
        // Handle preflight requests
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatusCode(204);
            return;
        }
        
        String path = request.getPath();
        String method = request.getMethod();
        
        logger.info("Handling request: {} {}", method, path);
        
        try {
            if ("/health".equals(path) && "GET".equals(method)) {
                handleHealthCheck(response);
            } else if (injector == null) {
                handleError(response, "Service not properly initialized", 503);
            } else if (path.startsWith("/document")) {
                handleDocumentRequest(request, response);
            } else if (path.startsWith("/chapter")) {
                handleChapterRequest(request, response);
            } else if (path.startsWith("/scene")) {
                handleSceneRequest(request, response);
            } else {
                handleNotFound(response);
            }
        } catch (Exception e) {
            logger.error("Error handling request", e);
            handleError(response, "Internal server error", 500);
        }
    }
    
    private void handleCors(HttpRequest request, HttpResponse response) {
        Optional<String> origin = request.getFirstHeader("Origin");
        if (origin.isPresent() && ALLOWED_ORIGINS.contains(origin.get())) {
            response.appendHeader("Access-Control-Allow-Origin", origin.get());
        } else if (origin.isPresent() && origin.get().startsWith("http://localhost:")) {
            response.appendHeader("Access-Control-Allow-Origin", origin.get());
        }
        
        response.appendHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.appendHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Request-ID");
        response.appendHeader("Access-Control-Max-Age", "3600");
    }
    
    private void handleHealthCheck(HttpResponse response) throws IOException {
        Map<String, Object> health = Map.of(
                "status", "healthy",
                "service", "novel-document-service",
                "version", "1.0.0",
                "timestamp", Instant.now().toString()
        );
        
        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            gson.toJson(health, writer);
        }
    }
    
    private void handleDocumentRequest(HttpRequest request, HttpResponse response) throws IOException {
        DocumentController controller = getDocumentController();
        if (controller == null) {
            handleError(response, "Document controller not available", 503);
            return;
        }
        
        String path = request.getPath();
        String method = request.getMethod();
        
        try {
            if ("/document".equals(path) && "POST".equals(method)) {
                controller.createDocument(request, response);
            } else if (path.matches("/document/[^/]+") && "GET".equals(method)) {
                String documentId = extractIdFromPath(path, "/document/");
                controller.getDocument(documentId, response);
            } else if (path.matches("/document/[^/]+") && "PUT".equals(method)) {
                String documentId = extractIdFromPath(path, "/document/");
                controller.updateDocument(documentId, request, response);
            } else if (path.matches("/document/[^/]+") && "DELETE".equals(method)) {
                String documentId = extractIdFromPath(path, "/document/");
                controller.deleteDocument(documentId, response);
            } else if ("/documents".equals(path) && "GET".equals(method)) {
                String userId = extractUserId(request);
                controller.listDocuments(userId, response);
            } else {
                handleNotFound(response);
            }
        } catch (Exception e) {
            logger.error("Error in document request handling", e);
            handleError(response, "Error processing document request: " + e.getMessage(), 500);
        }
    }
    
    private void handleChapterRequest(HttpRequest request, HttpResponse response) throws IOException {
        DocumentController controller = getDocumentController();
        if (controller == null) {
            handleError(response, "Document controller not available", 503);
            return;
        }
        
        String path = request.getPath();
        String method = request.getMethod();
        
        try {
            if (path.matches("/chapter/[^/]+/[^/]+") && "POST".equals(method)) {
                // POST /chapter/{documentId}/{chapterNumber}
                String[] parts = path.split("/");
                String documentId = parts[2];
                controller.createChapter(documentId, request, response);
            } else if (path.matches("/chapter/[^/]+/[^/]+") && "PUT".equals(method)) {
                // PUT /chapter/{documentId}/{chapterNumber}
                String[] parts = path.split("/");
                String documentId = parts[2];
                String chapterNumber = parts[3];
                controller.updateChapter(documentId, chapterNumber, request, response);
            } else if (path.matches("/chapter/[^/]+/[^/]+") && "DELETE".equals(method)) {
                // DELETE /chapter/{documentId}/{chapterNumber}
                String[] parts = path.split("/");
                String documentId = parts[2];
                String chapterNumber = parts[3];
                controller.deleteChapter(documentId, chapterNumber, response);
            } else {
                handleNotFound(response);
            }
        } catch (Exception e) {
            logger.error("Error in chapter request handling", e);
            handleError(response, "Error processing chapter request: " + e.getMessage(), 500);
        }
    }
    
    private void handleSceneRequest(HttpRequest request, HttpResponse response) throws IOException {
        DocumentController controller = getDocumentController();
        if (controller == null) {
            handleError(response, "Document controller not available", 503);
            return;
        }
        
        String path = request.getPath();
        String method = request.getMethod();
        
        try {
            if (path.matches("/scene/[^/]+/[^/]+/[^/]+") && "POST".equals(method)) {
                // POST /scene/{documentId}/{chapterNumber}/{sceneNumber}
                String[] parts = path.split("/");
                String documentId = parts[2];
                String chapterNumber = parts[3];
                controller.createScene(documentId, chapterNumber, request, response);
            } else if (path.matches("/scene/[^/]+/[^/]+/[^/]+") && "PUT".equals(method)) {
                // PUT /scene/{documentId}/{chapterNumber}/{sceneNumber}
                String[] parts = path.split("/");
                String documentId = parts[2];
                String chapterNumber = parts[3];
                String sceneNumber = parts[4];
                controller.updateScene(documentId, chapterNumber, sceneNumber, request, response);
            } else if (path.matches("/scene/[^/]+/[^/]+/[^/]+") && "DELETE".equals(method)) {
                // DELETE /scene/{documentId}/{chapterNumber}/{sceneNumber}
                String[] parts = path.split("/");
                String documentId = parts[2];
                String chapterNumber = parts[3];
                String sceneNumber = parts[4];
                controller.deleteScene(documentId, chapterNumber, sceneNumber, response);
            } else {
                handleNotFound(response);
            }
        } catch (Exception e) {
            logger.error("Error in scene request handling", e);
            handleError(response, "Error processing scene request: " + e.getMessage(), 500);
        }
    }
    
    private void handleNotFound(HttpResponse response) throws IOException {
        handleError(response, "Not found", 404);
    }
    
    private void handleError(HttpResponse response, String message, int statusCode) throws IOException {
        Map<String, Object> error = Map.of(
                "error", message,
                "statusCode", statusCode,
                "timestamp", Instant.now().toString()
        );
        
        response.setStatusCode(statusCode);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            gson.toJson(error, writer);
        }
    }
    
    private String extractIdFromPath(String path, String prefix) {
        return path.substring(prefix.length());
    }
    
    private String extractUserId(HttpRequest request) {
        // TODO: Extract from JWT token in Authorization header
        return request.getFirstHeader("X-User-Id").orElse("anonymous");
    }
}