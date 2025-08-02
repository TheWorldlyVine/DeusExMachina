package com.deusexmachina.novel.memory;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.deusexmachina.novel.memory.config.MemoryServiceModule;
import com.deusexmachina.novel.memory.controller.MemoryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NovelMemoryFunction implements HttpFunction {
    private static final Logger logger = LoggerFactory.getLogger(NovelMemoryFunction.class);
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://app.deusexmachina.com",
            "https://deusexmachina.com",
            "http://localhost:3000"
    );
    
    private final Injector injector;
    private final MemoryController memoryController;
    
    public NovelMemoryFunction() {
        this.injector = Guice.createInjector(new MemoryServiceModule());
        this.memoryController = injector.getInstance(MemoryController.class);
        logger.info("NovelMemoryFunction initialized successfully");
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
            } else if (path.startsWith("/memory")) {
                handleMemoryRequest(request, response);
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
                "service", "novel-memory-service",
                "version", "1.0.0",
                "timestamp", Instant.now().toString()
        );
        
        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            gson.toJson(health, writer);
        }
    }
    
    private void handleMemoryRequest(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getPath();
        String method = request.getMethod();
        
        if ("/memory".equals(path) && "POST".equals(method)) {
            // Store new memory
            handleError(response, "Memory storage not yet implemented", 501);
        } else if (path.matches("/memory/context/[^/]+") && "GET".equals(method)) {
            // Retrieve memories by context
            handleError(response, "Memory retrieval not yet implemented", 501);
        } else if (path.matches("/memory/[^/]+") && "GET".equals(method)) {
            // Get specific memory
            handleError(response, "Memory retrieval not yet implemented", 501);
        } else if (path.matches("/memory/[^/]+") && "PUT".equals(method)) {
            // Update memory
            handleError(response, "Memory update not yet implemented", 501);
        } else if (path.matches("/memory/[^/]+") && "DELETE".equals(method)) {
            // Delete memory
            handleError(response, "Memory deletion not yet implemented", 501);
        } else {
            handleNotFound(response);
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
}