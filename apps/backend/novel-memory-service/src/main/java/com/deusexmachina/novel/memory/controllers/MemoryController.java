package com.deusexmachina.novel.memory.controllers;

import com.deusexmachina.novel.memory.models.*;
import com.deusexmachina.novel.memory.services.*;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Controller for handling memory-related HTTP requests.
 */
@Singleton
public class MemoryController {
    private static final Logger logger = Logger.getLogger(MemoryController.class.getName());
    
    private final CharacterMemoryService characterService;
    private final PlotMemoryService plotService;
    private final WorldMemoryService worldService;
    private final ContextService contextService;
    private final Gson gson;
    
    @Inject
    public MemoryController(CharacterMemoryService characterService,
                          PlotMemoryService plotService,
                          WorldMemoryService worldService,
                          ContextService contextService,
                          Gson gson) {
        this.characterService = characterService;
        this.plotService = plotService;
        this.worldService = worldService;
        this.contextService = contextService;
        this.gson = gson;
    }
    
    // Character Memory Handlers
    
    public void createCharacter(HttpRequest request, HttpResponse response) throws IOException {
        try {
            JsonObject body = parseRequestBody(request);
            CreateCharacterRequest createRequest = gson.fromJson(body, CreateCharacterRequest.class);
            
            // Build character memory from request
            CharacterMemory memory = CharacterMemory.builder()
                .projectId(createRequest.getProjectId())
                .name(createRequest.getName())
                .role(createRequest.getRole())
                .backstory(createRequest.getBackstory())
                .goals(createRequest.getGoals())
                .motivations(createRequest.getMotivations())
                .conflicts(createRequest.getConflicts())
                .attributes(createRequest.getAttributes())
                .voiceProfile(createRequest.getVoiceProfile())
                .speechPatterns(createRequest.getSpeechPatterns())
                .relationships(createRequest.getRelationships())
                .currentState(CharacterMemory.CharacterState.builder()
                    .emotionalState("neutral")
                    .physicalState("healthy")
                    .mentalState("focused")
                    .energyLevel(100)
                    .stressLevel(0)
                    .build())
                .observations(new ArrayList<>())
                .reflections(new ArrayList<>())
                .plannedActions(new ArrayList<>())
                .recentSceneIds(new ArrayList<>())
                .wordCount(0L)
                .build();
            
            CharacterMemory saved = characterService.saveCharacterMemory(memory).join();
            sendJsonResponse(response, 201, saved);
        } catch (Exception e) {
            logger.severe("Error creating character: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    public void getCharacterMemory(String characterId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            String projectId = extractProjectId(request);
            CharacterMemory memory = characterService.getCharacterMemory(projectId, characterId).join();
            
            if (memory != null) {
                sendJsonResponse(response, 200, memory);
            } else {
                sendErrorResponse(response, 404, "Character not found");
            }
        } catch (Exception e) {
            logger.severe("Error getting character memory: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    public void updateCharacterState(String characterId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            String projectId = extractProjectId(request);
            JsonObject body = parseRequestBody(request);
            
            CharacterMemory.CharacterState state = gson.fromJson(body, CharacterMemory.CharacterState.class);
            CharacterMemory updated = characterService.updateCharacterState(projectId, characterId, state).join();
            
            sendJsonResponse(response, 200, updated);
        } catch (Exception e) {
            logger.severe("Error updating character state: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    public void addCharacterObservation(String characterId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            String projectId = extractProjectId(request);
            JsonObject body = parseRequestBody(request);
            
            CharacterMemory.CharacterObservation observation = 
                gson.fromJson(body, CharacterMemory.CharacterObservation.class);
            CharacterMemory updated = characterService.addObservation(projectId, characterId, observation).join();
            
            sendJsonResponse(response, 200, updated);
        } catch (Exception e) {
            logger.severe("Error adding observation: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    public void getCharacterTimeline(String characterId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            String projectId = extractProjectId(request);
            int limit = extractIntParam(request, "limit", 50);
            
            List<CharacterMemory.CharacterObservation> timeline = 
                characterService.getCharacterTimeline(projectId, characterId, limit).join();
            
            sendJsonResponse(response, 200, timeline);
        } catch (Exception e) {
            logger.severe("Error getting character timeline: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    // Plot Memory Handlers
    
    public void getPlotMemory(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            List<PlotMemory> plots = plotService.getProjectPlots(projectId).join();
            sendJsonResponse(response, 200, plots);
        } catch (Exception e) {
            logger.severe("Error getting plot memory: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    public void updatePlotThread(String projectId, String threadId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            JsonObject body = parseRequestBody(request);
            PlotMemory plotMemory = gson.fromJson(body, PlotMemory.class);
            plotMemory.setProjectId(projectId);
            plotMemory.setPlotId(threadId);
            
            PlotMemory updated = plotService.savePlotMemory(plotMemory).join();
            sendJsonResponse(response, 200, updated);
        } catch (Exception e) {
            logger.severe("Error updating plot thread: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    public void addPlotMilestone(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            JsonObject body = parseRequestBody(request);
            String plotId = body.get("plotId").getAsString();
            PlotMemory.Milestone milestone = gson.fromJson(body.get("milestone"), PlotMemory.Milestone.class);
            
            PlotMemory updated = plotService.addMilestone(projectId, plotId, milestone).join();
            sendJsonResponse(response, 200, updated);
        } catch (Exception e) {
            logger.severe("Error adding plot milestone: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    // World Memory Handlers
    
    public void getWorldMemory(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            List<WorldMemory> worldMemories = worldService.getProjectWorldMemory(projectId).join();
            sendJsonResponse(response, 200, worldMemories);
        } catch (Exception e) {
            logger.severe("Error getting world memory: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    public void addWorldFact(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            JsonObject body = parseRequestBody(request);
            String category = body.get("category").getAsString();
            WorldMemory.WorldFact fact = gson.fromJson(body.get("fact"), WorldMemory.WorldFact.class);
            
            WorldMemory updated = worldService.addWorldFact(projectId, category, fact).join();
            sendJsonResponse(response, 200, updated);
        } catch (Exception e) {
            logger.severe("Error adding world fact: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    public void validateWorld(String projectId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            List<WorldMemory.Contradiction> contradictions = worldService.validateConsistency(projectId).join();
            sendJsonResponse(response, 200, contradictions);
        } catch (Exception e) {
            logger.severe("Error validating world: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    // Search Handler
    
    public void searchMemory(HttpRequest request, HttpResponse response) throws IOException {
        try {
            JsonObject body = parseRequestBody(request);
            String projectId = body.get("projectId").getAsString();
            String query = body.get("query").getAsString();
            String searchType = body.has("type") ? body.get("type").getAsString() : "all";
            
            JsonObject results = new JsonObject();
            
            if ("all".equals(searchType) || "facts".equals(searchType)) {
                List<WorldMemory.WorldFact> facts = worldService.searchFacts(projectId, query).join();
                results.add("facts", gson.toJsonTree(facts));
            }
            
            if ("all".equals(searchType) || "characters".equals(searchType)) {
                List<CharacterMemory> characters = characterService.getProjectCharacters(projectId).join();
                List<CharacterMemory> matchingCharacters = characters.stream()
                    .filter(c -> c.getName().toLowerCase().contains(query.toLowerCase()) ||
                               (c.getBackstory() != null && c.getBackstory().toLowerCase().contains(query.toLowerCase())))
                    .toList();
                results.add("characters", gson.toJsonTree(matchingCharacters));
            }
            
            if ("all".equals(searchType) || "plots".equals(searchType)) {
                List<PlotMemory> plots = plotService.getProjectPlots(projectId).join();
                List<PlotMemory> matchingPlots = plots.stream()
                    .filter(p -> p.getThreadName().toLowerCase().contains(query.toLowerCase()) ||
                               (p.getPremise() != null && p.getPremise().toLowerCase().contains(query.toLowerCase())))
                    .toList();
                results.add("plots", gson.toJsonTree(matchingPlots));
            }
            
            sendJsonResponse(response, 200, results);
        } catch (Exception e) {
            logger.severe("Error searching memory: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    // Context Handler
    
    public void getGenerationContext(String projectId, String sceneId, HttpRequest request, HttpResponse response) 
            throws IOException {
        try {
            Long chapterNumber = extractLongParam(request, "chapter", 1L);
            Long sceneNumber = extractLongParam(request, "scene", 1L);
            
            GenerationContext context = contextService.buildGenerationContext(
                projectId, sceneId, chapterNumber, sceneNumber).join();
            
            sendJsonResponse(response, 200, context);
        } catch (Exception e) {
            logger.severe("Error getting generation context: " + e.getMessage());
            sendErrorResponse(response, 500, "Internal server error");
        }
    }
    
    // Helper methods
    
    private String extractProjectId(HttpRequest request) {
        String userId = request.getFirstHeader("X-User-ID").orElse("default");
        String projectIdHeader = request.getFirstHeader("X-Project-ID").orElse(null);
        
        if (projectIdHeader != null) {
            return projectIdHeader;
        }
        
        // Extract from path if not in header
        String path = request.getPath();
        if (path.contains("/project/")) {
            int start = path.indexOf("/project/") + 9;
            int end = path.indexOf("/", start);
            if (end == -1) end = path.length();
            return path.substring(start, end);
        }
        
        return userId + "_default_project";
    }
    
    private int extractIntParam(HttpRequest request, String param, int defaultValue) {
        return request.getFirstQueryParameter(param)
            .map(Integer::parseInt)
            .orElse(defaultValue);
    }
    
    private long extractLongParam(HttpRequest request, String param, long defaultValue) {
        return request.getFirstQueryParameter(param)
            .map(Long::parseLong)
            .orElse(defaultValue);
    }
    
    private JsonObject parseRequestBody(HttpRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    
    private void sendJsonResponse(HttpResponse response, int status, Object data) throws IOException {
        response.setStatusCode(status);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write(gson.toJson(data));
        }
    }
    
    private void sendErrorResponse(HttpResponse response, int status, String message) throws IOException {
        response.setStatusCode(status);
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", message);
            writer.write(error.toString());
        }
    }
}