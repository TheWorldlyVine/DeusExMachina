package com.deusexmachina.novel.ai;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.deusexmachina.novel.ai.config.NovelAIModule;
import com.deusexmachina.novel.ai.controller.GenerationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NovelAIFunction implements HttpFunction {
    private static final Logger logger = LoggerFactory.getLogger(NovelAIFunction.class);
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://app.deusexmachina.com",
            "https://deusexmachina.com",
            "http://localhost:3000"
    );
    
    private final Injector injector;
    private final GenerationController generationController;
    
    public NovelAIFunction() {
        this.injector = Guice.createInjector(new NovelAIModule());
        this.generationController = injector.getInstance(GenerationController.class);
        logger.info("NovelAIFunction initialized successfully");
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
            switch (path) {
                case "/health" -> handleHealthCheck(request, response);
                case "/generate" -> handleGenerate(request, response);
                default -> handleNotFound(response);
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
            // Allow any localhost port for development
            response.appendHeader("Access-Control-Allow-Origin", origin.get());
        }
        
        response.appendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.appendHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Request-ID");
        response.appendHeader("Access-Control-Max-Age", "3600");
    }
    
    private void handleHealthCheck(HttpRequest request, HttpResponse response) throws IOException {
        if (!"GET".equals(request.getMethod())) {
            handleError(response, "Method not allowed", 405);
            return;
        }
        
        Map<String, Object> health = Map.of(
                "status", "healthy",
                "service", "novel-ai-service",
                "version", "1.0.0",
                "timestamp", Instant.now().toString()
        );
        
        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            gson.toJson(health, writer);
        }
    }
    
    private void handleGenerate(HttpRequest request, HttpResponse response) throws IOException {
        if (!"POST".equals(request.getMethod())) {
            handleError(response, "Method not allowed", 405);
            return;
        }
        
        // Check content type
        Optional<String> contentType = request.getContentType();
        if (contentType.isEmpty() || !contentType.get().contains("application/json")) {
            handleError(response, "Content-Type must be application/json", 400);
            return;
        }
        
        // For now, return 501 Not Implemented as we haven't built the generation service yet
        handleError(response, "Generation service not yet implemented", 501);
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