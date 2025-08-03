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
import com.deusexmachina.novel.document.auth.AuthenticationMiddleware;
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
            "https://34.95.119.251",
            "http://localhost:3000",
            "http://localhost:3001"
    );
    
    private Injector injector;
    private DocumentController documentController;
    
    public NovelDocumentFunction() {
        try {
            logger.info("Initializing NovelDocumentFunction...");
            logger.info("Environment - Project ID: {}", System.getenv("GCP_PROJECT_ID"));
            logger.info("Environment - Service Account: {}", System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
            
            this.injector = Guice.createInjector(new DocumentServiceModule());
            logger.info("Guice injector created successfully");
            
            // Delay controller initialization to first request to avoid startup timeout
            logger.info("NovelDocumentFunction initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize NovelDocumentFunction: {} - {}", 
                e.getClass().getName(), e.getMessage(), e);
            // Don't throw - let the function start and return errors on requests
        }
    }
    
    private synchronized DocumentController getDocumentController() {
        if (documentController == null && injector != null) {
            try {
                logger.info("Attempting to get DocumentController from injector...");
                documentController = injector.getInstance(DocumentController.class);
                logger.info("DocumentController initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize DocumentController: {} - {}", 
                    e.getClass().getName(), e.getMessage(), e);
                throw new RuntimeException("DocumentController initialization failed", e);
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
        
        // Check authentication for protected endpoints
        if (AuthenticationMiddleware.requiresAuthentication(request)) {
            if (!AuthenticationMiddleware.validateAuthentication(request, response)) {
                return; // Response already sent by middleware
            }
        }
        
        try {
            if ("/health".equals(path) && "GET".equals(method)) {
                handleHealthCheck(response);
            } else if (injector == null) {
                logger.error("Service not initialized - injector is null");
                handleError(response, "Service not properly initialized", 503);
            } else if (path.startsWith("/document")) {
                handleDocumentRequest(request, response);
            } else if (path.startsWith("/chapter")) {
                handleChapterRequest(request, response);
            } else if (path.startsWith("/scene")) {
                handleSceneRequest(request, response);
            } else {
                logger.warn("Path not found: {}", path);
                handleNotFound(response);
            }
        } catch (Exception e) {
            logger.error("Error handling request: {} - {}", e.getClass().getName(), e.getMessage(), e);
            handleError(response, "Internal server error: " + e.getMessage(), 500);
        }
    }
    
    private void handleCors(HttpRequest request, HttpResponse response) {
        Optional<String> origin = request.getFirstHeader("Origin");
        if (origin.isPresent()) {
            // In production, we'd check against ALLOWED_ORIGINS
            // For now, allow all origins to support development
            response.appendHeader("Access-Control-Allow-Origin", origin.get());
        } else {
            // Fallback to wildcard if no origin header
            response.appendHeader("Access-Control-Allow-Origin", "*");
        }
        
        response.appendHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.appendHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Request-ID");
        response.appendHeader("Access-Control-Max-Age", "3600");
        response.appendHeader("Access-Control-Allow-Credentials", "true");
    }
    
    private void handleHealthCheck(HttpResponse response) throws IOException {
        Map<String, Object> health = new java.util.HashMap<>();
        health.put("status", injector != null ? "healthy" : "unhealthy");
        health.put("service", "novel-document-service");
        health.put("version", "1.0.0");
        health.put("timestamp", Instant.now().toString());
        health.put("environment", Map.of(
            "projectId", System.getenv("GCP_PROJECT_ID") != null ? System.getenv("GCP_PROJECT_ID") : "not-set",
            "hasInjector", injector != null,
            "hasController", documentController != null
        ));
        
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
            if (("/document".equals(path) || "/documents".equals(path)) && "POST".equals(method)) {
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
                String userId = AuthenticationMiddleware.extractUserId(request);
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
    
}