package com.deusexmachina.novel.memory.services.impl;

import com.deusexmachina.novel.memory.models.*;
import com.deusexmachina.novel.memory.services.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of ContextService that builds generation context from memory.
 */
@Singleton
public class ContextServiceImpl implements ContextService {
    private static final Logger logger = Logger.getLogger(ContextServiceImpl.class.getName());
    
    private final CharacterMemoryService characterService;
    private final PlotMemoryService plotService;
    private final WorldMemoryService worldService;
    
    @Inject
    public ContextServiceImpl(CharacterMemoryService characterService,
                            PlotMemoryService plotService,
                            WorldMemoryService worldService) {
        this.characterService = characterService;
        this.plotService = plotService;
        this.worldService = worldService;
    }
    
    @Override
    public CompletableFuture<GenerationContext> buildGenerationContext(String projectId, String sceneId,
            Long chapterNumber, Long sceneNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Building generation context for scene: " + sceneId);
                
                GenerationContext.GenerationContextBuilder contextBuilder = GenerationContext.builder()
                    .projectId(projectId)
                    .sceneId(sceneId);
                
                // Get all scene characters
                List<CharacterMemory> sceneCharacters = characterService.getSceneCharacters(projectId, sceneId).join();
                List<GenerationContext.CharacterContext> characterContexts = new ArrayList<>();
                Map<String, String> characterRelationships = new HashMap<>();
                
                // Build character contexts
                for (CharacterMemory character : sceneCharacters) {
                    GenerationContext.CharacterContext charContext = buildCharacterContext(character, sceneCharacters);
                    characterContexts.add(charContext);
                    
                    // Collect relationships
                    if (character.getRelationships() != null) {
                        for (Map.Entry<String, String> rel : character.getRelationships().entrySet()) {
                            String key = character.getCharacterId() + "_" + rel.getKey();
                            characterRelationships.put(key, rel.getValue());
                        }
                    }
                }
                
                contextBuilder.activeCharacters(characterContexts);
                contextBuilder.characterRelationships(characterRelationships);
                
                // Get active plot threads
                List<PlotMemory> activePlots = plotService.getActiveThreads(projectId, chapterNumber).join();
                List<String> activeThreadNames = activePlots.stream()
                    .map(PlotMemory::getThreadName)
                    .collect(Collectors.toList());
                
                // Get main plot for current phase and tension
                PlotMemory mainPlot = plotService.getMainPlot(projectId).join();
                if (mainPlot != null) {
                    contextBuilder.currentPlotPhase(mainPlot.getCurrentPhase());
                    contextBuilder.currentTensionLevel(mainPlot.getTensionLevel());
                }
                
                // Get upcoming plot points
                List<PlotMemory.PlotPoint> upcomingPoints = plotService.getUpcomingPlotPoints(projectId, chapterNumber, 3).join();
                List<String> upcomingPlotPointDescriptions = upcomingPoints.stream()
                    .map(PlotMemory.PlotPoint::getDescription)
                    .limit(5)
                    .collect(Collectors.toList());
                
                contextBuilder.activeThreads(activeThreadNames);
                contextBuilder.upcomingPlotPoints(upcomingPlotPointDescriptions);
                
                // Get world context
                List<WorldMemory> worldMemories = worldService.getProjectWorldMemory(projectId).join();
                
                // Extract current location from characters
                String currentLocation = sceneCharacters.stream()
                    .map(CharacterMemory::getCurrentLocation)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("Unknown location");
                
                contextBuilder.currentLocation(currentLocation);
                
                // Get location details
                Map<String, String> locationDetails = new HashMap<>();
                for (WorldMemory worldMem : worldMemories) {
                    if (worldMem.getLocations() != null) {
                        worldMem.getLocations().stream()
                            .filter(loc -> currentLocation.equals(loc.getName()))
                            .findFirst()
                            .ifPresent(loc -> {
                                locationDetails.put("description", loc.getDescription());
                                locationDetails.put("type", loc.getType());
                                if (loc.getProperties() != null) {
                                    loc.getProperties().forEach((k, v) -> 
                                        locationDetails.put(k, String.valueOf(v)));
                                }
                            });
                    }
                }
                contextBuilder.locationDetails(locationDetails);
                
                // Get relevant world facts
                List<String> relevantFacts = new ArrayList<>();
                for (WorldMemory worldMem : worldMemories) {
                    if (worldMem.getFacts() != null) {
                        worldMem.getFacts().stream()
                            .filter(fact -> fact.getImportance() != null && fact.getImportance() >= 7)
                            .limit(10)
                            .forEach(fact -> relevantFacts.add(fact.getFact()));
                    }
                }
                contextBuilder.relevantWorldFacts(relevantFacts);
                
                // Get recent events from character observations
                List<String> recentEvents = new ArrayList<>();
                for (CharacterMemory character : sceneCharacters) {
                    if (character.getObservations() != null) {
                        character.getObservations().stream()
                            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                            .limit(3)
                            .forEach(obs -> recentEvents.add(obs.getObservation()));
                    }
                }
                contextBuilder.recentEvents(recentEvents);
                
                // Build consistency rules
                Map<String, String> consistencyRules = new HashMap<>();
                
                // Character consistency rules
                for (CharacterMemory character : sceneCharacters) {
                    if (character.getConsistencyRules() != null) {
                        character.getConsistencyRules().forEach((rule, value) ->
                            consistencyRules.put(character.getName() + "_" + rule, String.valueOf(value)));
                    }
                }
                
                // World consistency rules
                for (WorldMemory worldMem : worldMemories) {
                    if (worldMem.getConsistencyRules() != null) {
                        worldMem.getConsistencyRules().forEach(rule ->
                            consistencyRules.put("world_" + rule.getRuleId(), rule.getRule()));
                    }
                }
                
                contextBuilder.consistencyRules(consistencyRules);
                
                // Set themes from main plot
                if (mainPlot != null && mainPlot.getThemes() != null) {
                    contextBuilder.themesToEmphasize(mainPlot.getThemes());
                }
                
                GenerationContext context = contextBuilder.build();
                logger.info("Built generation context with " + characterContexts.size() + 
                          " characters and " + activeThreadNames.size() + " active plots");
                
                return context;
            } catch (Exception e) {
                logger.severe("Error building generation context: " + e.getMessage());
                throw new RuntimeException("Failed to build generation context", e);
            }
        });
    }
    
    private GenerationContext.CharacterContext buildCharacterContext(CharacterMemory character,
            List<CharacterMemory> sceneCharacters) {
        GenerationContext.CharacterContext.CharacterContextBuilder builder = 
            GenerationContext.CharacterContext.builder()
                .characterId(character.getCharacterId())
                .name(character.getName());
        
        // Set current state
        if (character.getCurrentState() != null) {
            CharacterMemory.CharacterState state = character.getCurrentState();
            builder.currentState(String.format("Emotional: %s, Physical: %s, Mental: %s",
                state.getEmotionalState(), state.getPhysicalState(), state.getMentalState()));
            builder.emotionalState(state.getEmotionalState());
        }
        
        // Set objective
        builder.currentObjective(character.getCurrentObjective());
        
        // Set voice profile and speech patterns
        builder.voiceProfile(character.getVoiceProfile());
        builder.speechPatterns(character.getSpeechPatterns());
        
        // Get recent actions
        List<String> recentActions = new ArrayList<>();
        if (character.getObservations() != null) {
            character.getObservations().stream()
                .filter(obs -> "action".equals(obs.getObservationType()))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(5)
                .forEach(obs -> recentActions.add(obs.getObservation()));
        }
        builder.recentActions(recentActions);
        
        // Build relationships with other characters in scene
        Map<String, String> sceneRelationships = new HashMap<>();
        if (character.getRelationships() != null) {
            for (CharacterMemory other : sceneCharacters) {
                if (!other.getCharacterId().equals(character.getCharacterId())) {
                    String relationship = character.getRelationships().get(other.getCharacterId());
                    if (relationship != null) {
                        sceneRelationships.put(other.getName(), relationship);
                    }
                }
            }
        }
        builder.relationshipsInScene(sceneRelationships);
        
        return builder.build();
    }
    
    @Override
    public CompletableFuture<GenerationContext> buildCharacterContext(String projectId, String characterId,
            String sceneId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CharacterMemory character = characterService.getCharacterMemory(projectId, characterId).join();
                if (character == null) {
                    throw new RuntimeException("Character not found: " + characterId);
                }
                
                // Get basic scene context
                GenerationContext baseContext = buildGenerationContext(projectId, sceneId, 0L, 0L).join();
                
                // Focus on the specific character
                GenerationContext.GenerationContextBuilder builder = GenerationContext.builder()
                    .projectId(projectId)
                    .sceneId(sceneId);
                
                // Find the character's context
                GenerationContext.CharacterContext focusCharContext = baseContext.getActiveCharacters().stream()
                    .filter(cc -> characterId.equals(cc.getCharacterId()))
                    .findFirst()
                    .orElse(null);
                
                if (focusCharContext != null) {
                    builder.activeCharacters(Collections.singletonList(focusCharContext));
                }
                
                // Add character-specific must-includes
                List<String> mustInclude = new ArrayList<>();
                if (character.getPlannedActions() != null) {
                    mustInclude.addAll(character.getPlannedActions());
                }
                if (character.getNextSceneObjective() != null) {
                    mustInclude.add("Character objective: " + character.getNextSceneObjective());
                }
                builder.mustInclude(mustInclude);
                
                // Copy other relevant context
                builder.currentLocation(baseContext.getCurrentLocation());
                builder.locationDetails(baseContext.getLocationDetails());
                builder.relevantWorldFacts(baseContext.getRelevantWorldFacts());
                builder.consistencyRules(baseContext.getConsistencyRules());
                
                return builder.build();
            } catch (Exception e) {
                logger.severe("Error building character context: " + e.getMessage());
                throw new RuntimeException("Failed to build character context", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<GenerationContext> buildPlotContext(String projectId, String plotId,
            Long chapterNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlotMemory plot = plotService.getPlotMemory(projectId, plotId).join();
                if (plot == null) {
                    throw new RuntimeException("Plot not found: " + plotId);
                }
                
                GenerationContext.GenerationContextBuilder builder = GenerationContext.builder()
                    .projectId(projectId);
                
                // Set plot-specific context
                builder.currentPlotPhase(plot.getCurrentPhase());
                builder.currentTensionLevel(plot.getTensionLevel());
                builder.activeThreads(Collections.singletonList(plot.getThreadName()));
                
                // Get upcoming plot points for this thread
                List<String> upcomingPoints = new ArrayList<>();
                if (plot.getPlotPoints() != null) {
                    plot.getPlotPoints().stream()
                        .filter(pp -> pp.getTargetChapter() != null && 
                               pp.getTargetChapter() > chapterNumber &&
                               !"written".equals(pp.getStatus()))
                        .sorted((a, b) -> a.getTargetChapter().compareTo(b.getTargetChapter()))
                        .limit(3)
                        .forEach(pp -> upcomingPoints.add(pp.getDescription()));
                }
                builder.upcomingPlotPoints(upcomingPoints);
                
                // Set themes and tone
                builder.themesToEmphasize(plot.getThemes());
                
                // Add foreshadowing as must-include
                if (plot.getForeshadowing() != null) {
                    builder.mustInclude(plot.getForeshadowing());
                }
                
                // Get characters involved in this plot
                if (plot.getInvolvedCharacterIds() != null) {
                    List<CharacterMemory> involvedCharacters = new ArrayList<>();
                    for (String charId : plot.getInvolvedCharacterIds()) {
                        CharacterMemory character = characterService.getCharacterMemory(projectId, charId).join();
                        if (character != null) {
                            involvedCharacters.add(character);
                        }
                    }
                    
                    List<GenerationContext.CharacterContext> characterContexts = 
                        involvedCharacters.stream()
                            .map(character -> buildCharacterContext(character, involvedCharacters))
                            .collect(Collectors.toList());
                    
                    builder.activeCharacters(characterContexts);
                }
                
                return builder.build();
            } catch (Exception e) {
                logger.severe("Error building plot context: " + e.getMessage());
                throw new RuntimeException("Failed to build plot context", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<GenerationContext> buildLocationContext(String projectId, String locationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                WorldMemory.Location location = worldService.getLocation(projectId, locationId).join();
                if (location == null) {
                    throw new RuntimeException("Location not found: " + locationId);
                }
                
                GenerationContext.GenerationContextBuilder builder = GenerationContext.builder()
                    .projectId(projectId)
                    .currentLocation(location.getName());
                
                // Build location details
                Map<String, String> locationDetails = new HashMap<>();
                locationDetails.put("description", location.getDescription());
                locationDetails.put("type", location.getType());
                if (location.getProperties() != null) {
                    location.getProperties().forEach((k, v) -> 
                        locationDetails.put(k, String.valueOf(v)));
                }
                builder.locationDetails(locationDetails);
                
                // Get facts related to this location
                List<WorldMemory.WorldFact> locationFacts = 
                    worldService.getLocationFacts(projectId, locationId).join();
                List<String> relevantFacts = locationFacts.stream()
                    .map(WorldMemory.WorldFact::getFact)
                    .collect(Collectors.toList());
                builder.relevantWorldFacts(relevantFacts);
                
                // Get characters present at location
                if (location.getCharactersPresentIds() != null) {
                    List<CharacterMemory> presentCharacters = new ArrayList<>();
                    for (String charId : location.getCharactersPresentIds()) {
                        CharacterMemory character = characterService.getCharacterMemory(projectId, charId).join();
                        if (character != null) {
                            presentCharacters.add(character);
                        }
                    }
                    
                    List<GenerationContext.CharacterContext> characterContexts = 
                        presentCharacters.stream()
                            .map(character -> buildCharacterContext(character, presentCharacters))
                            .collect(Collectors.toList());
                    
                    builder.activeCharacters(characterContexts);
                }
                
                return builder.build();
            } catch (Exception e) {
                logger.severe("Error building location context: " + e.getMessage());
                throw new RuntimeException("Failed to build location context", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> validateContext(GenerationContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Basic validation
                if (context.getProjectId() == null) {
                    logger.warning("Context validation failed: missing projectId");
                    return false;
                }
                
                // Validate character consistency
                if (context.getActiveCharacters() != null) {
                    for (GenerationContext.CharacterContext charContext : context.getActiveCharacters()) {
                        if (charContext.getName() == null || charContext.getCharacterId() == null) {
                            logger.warning("Context validation failed: incomplete character context");
                            return false;
                        }
                    }
                }
                
                // Validate location
                if (context.getCurrentLocation() == null || "Unknown location".equals(context.getCurrentLocation())) {
                    logger.warning("Context validation warning: no valid location");
                    // This is a warning, not a failure
                }
                
                logger.info("Context validation passed");
                return true;
            } catch (Exception e) {
                logger.severe("Error validating context: " + e.getMessage());
                return false;
            }
        });
    }
}