package com.deusexmachina.novel.memory;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Cloud Function entry point for the Novel Memory Service.
 * Handles HTTP requests for memory management operations.
 */
public class NovelMemoryFunction implements HttpFunction {
    private static final Logger logger = Logger.getLogger(NovelMemoryFunction.class.getName());
    
    private final Injector injector;
    
    public NovelMemoryFunction() {
        // Initialize Guice injector with module
        this.injector = Guice.createInjector(new NovelMemoryModule());
        logger.info("Novel Memory Function initialized");
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
        
        // Log request info
        logger.info(String.format("Received %s request to %s", 
            request.getMethod(), request.getPath()));
        
        try {
            // Route request based on path and method
            String path = request.getPath();
            String method = request.getMethod();
            
            if (path.startsWith("/memory")) {
                handleMemoryRequest(request, response);
            } else {
                response.setStatusCode(404);
                try (BufferedWriter writer = response.getWriter()) {
                    writer.write("{\"error\":\"Not found\"}");
                }
            }
        } catch (Exception e) {
            logger.severe("Error processing request: " + e.getMessage());
            response.setStatusCode(500);
            try (BufferedWriter writer = response.getWriter()) {
                writer.write("{\"error\":\"Internal server error\"}");
            }
        }
    }
    
    private void handleMemoryRequest(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getPath();
        String method = request.getMethod();
        
        // Remove /memory prefix for cleaner routing
        String subPath = path.startsWith("/memory") ? path.substring(7) : path;
        
        logger.info(String.format("Processing memory request: %s %s", method, subPath));
        
        // Route based on path patterns
        if (subPath.startsWith("/characters/")) {
            handleCharacterMemoryRequest(subPath, method, request, response);
        } else if (subPath.startsWith("/plot/")) {
            handlePlotMemoryRequest(subPath, method, request, response);
        } else if (subPath.startsWith("/world/")) {
            handleWorldMemoryRequest(subPath, method, request, response);
        } else if (subPath.equals("/search") && "POST".equals(method)) {
            handleSearchRequest(request, response);
        } else if (subPath.startsWith("/context/")) {
            handleContextRequest(subPath, method, request, response);
        } else {
            response.setStatusCode(404);
            response.setContentType("application/json");
            try (BufferedWriter writer = response.getWriter()) {
                writer.write("{\"error\":\"Memory endpoint not found\",\"path\":\"" + path + "\"}");
            }
        }
    }
    
    private void handleCharacterMemoryRequest(String path, String method, 
            HttpRequest request, HttpResponse response) throws IOException {
        // Extract character ID from path
        String[] parts = path.split("/");
        if (parts.length < 3) {
            sendBadRequest(response, "Invalid character path");
            return;
        }
        
        String characterId = parts[2];
        
        // Route based on specific character endpoints
        if (parts.length == 3 && "GET".equals(method)) {
            // GET /characters/{characterId}
            getCharacterMemory(characterId, request, response);
        } else if (parts.length == 4 && "state".equals(parts[3]) && "PUT".equals(method)) {
            // PUT /characters/{characterId}/state
            updateCharacterState(characterId, request, response);
        } else if (parts.length == 4 && "observations".equals(parts[3]) && "POST".equals(method)) {
            // POST /characters/{characterId}/observations
            addCharacterObservation(characterId, request, response);
        } else if (parts.length == 4 && "timeline".equals(parts[3]) && "GET".equals(method)) {
            // GET /characters/{characterId}/timeline
            getCharacterTimeline(characterId, request, response);
        } else {
            sendNotFound(response);
        }
    }
    
    private void handlePlotMemoryRequest(String path, String method, 
            HttpRequest request, HttpResponse response) throws IOException {
        // Extract project ID from path
        String[] parts = path.split("/");
        if (parts.length < 3) {
            sendBadRequest(response, "Invalid plot path");
            return;
        }
        
        String projectId = parts[2];
        
        // Route based on specific plot endpoints
        if (parts.length == 3 && "GET".equals(method)) {
            // GET /plot/{projectId}
            getPlotMemory(projectId, request, response);
        } else if (parts.length == 5 && "threads".equals(parts[3]) && "PUT".equals(method)) {
            // PUT /plot/{projectId}/threads/{threadId}
            String threadId = parts[4];
            updatePlotThread(projectId, threadId, request, response);
        } else if (parts.length == 4 && "milestones".equals(parts[3]) && "POST".equals(method)) {
            // POST /plot/{projectId}/milestones
            addPlotMilestone(projectId, request, response);
        } else {
            sendNotFound(response);
        }
    }
    
    private void handleWorldMemoryRequest(String path, String method, 
            HttpRequest request, HttpResponse response) throws IOException {
        // Extract project ID from path
        String[] parts = path.split("/");
        if (parts.length < 3) {
            sendBadRequest(response, "Invalid world path");
            return;
        }
        
        String projectId = parts[2];
        
        // Route based on specific world endpoints
        if (parts.length == 3 && "GET".equals(method)) {
            // GET /world/{projectId}
            getWorldMemory(projectId, request, response);
        } else if (parts.length == 4 && "facts".equals(parts[3]) && "POST".equals(method)) {
            // POST /world/{projectId}/facts
            addWorldFact(projectId, request, response);
        } else if (parts.length == 4 && "validate".equals(parts[3]) && "POST".equals(method)) {
            // POST /world/{projectId}/validate
            validateWorld(projectId, request, response);
        } else {
            sendNotFound(response);
        }
    }
    
    private void handleSearchRequest(HttpRequest request, HttpResponse response) throws IOException {
        // TODO: Implement search functionality
        sendNotImplemented(response, "Memory search");
    }
    
    private void handleContextRequest(String path, String method, 
            HttpRequest request, HttpResponse response) throws IOException {
        if (!"GET".equals(method)) {
            sendMethodNotAllowed(response);
            return;
        }
        
        // Extract project ID and scene ID from path
        String[] parts = path.split("/");
        if (parts.length < 4) {
            sendBadRequest(response, "Invalid context path");
            return;
        }
        
        String projectId = parts[2];
        String sceneId = parts[3];
        
        getGenerationContext(projectId, sceneId, request, response);
    }
    
    // Character Memory Methods
    private void getCharacterMemory(String characterId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement character memory retrieval
        sendNotImplemented(response, "Get character memory");
    }
    
    private void updateCharacterState(String characterId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement character state update
        sendNotImplemented(response, "Update character state");
    }
    
    private void addCharacterObservation(String characterId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement character observation addition
        sendNotImplemented(response, "Add character observation");
    }
    
    private void getCharacterTimeline(String characterId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement character timeline retrieval
        sendNotImplemented(response, "Get character timeline");
    }
    
    // Plot Memory Methods
    private void getPlotMemory(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement plot memory retrieval
        sendNotImplemented(response, "Get plot memory");
    }
    
    private void updatePlotThread(String projectId, String threadId, 
            HttpRequest request, HttpResponse response) throws IOException {
        // TODO: Implement plot thread update
        sendNotImplemented(response, "Update plot thread");
    }
    
    private void addPlotMilestone(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement plot milestone addition
        sendNotImplemented(response, "Add plot milestone");
    }
    
    // World Memory Methods
    private void getWorldMemory(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement world memory retrieval
        sendNotImplemented(response, "Get world memory");
    }
    
    private void addWorldFact(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement world fact addition
        sendNotImplemented(response, "Add world fact");
    }
    
    private void validateWorld(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        // TODO: Implement world validation
        sendNotImplemented(response, "Validate world");
    }
    
    // Context Method
    private void getGenerationContext(String projectId, String sceneId, 
            HttpRequest request, HttpResponse response) throws IOException {
        // TODO: Implement generation context retrieval
        sendNotImplemented(response, "Get generation context");
    }
    
    // Helper methods for response handling
    private void sendNotImplemented(HttpResponse response, String feature) throws IOException {
        response.setStatusCode(501);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"error\":\"Not implemented\",\"feature\":\"" + feature + "\"}");
        }
    }
    
    private void sendBadRequest(HttpResponse response, String message) throws IOException {
        response.setStatusCode(400);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"error\":\"Bad request\",\"message\":\"" + message + "\"}");
        }
    }
    
    private void sendNotFound(HttpResponse response) throws IOException {
        response.setStatusCode(404);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"error\":\"Not found\"}");
        }
    }
    
    private void sendMethodNotAllowed(HttpResponse response) throws IOException {
        response.setStatusCode(405);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write("{\"error\":\"Method not allowed\"}");
        }
    }
    
    private void handleCors(HttpRequest request, HttpResponse response) {
        Optional<String> origin = request.getFirstHeader("Origin");
        if (origin.isPresent()) {
            // In production, we'd check against allowed origins
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
}