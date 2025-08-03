package com.deusexmachina.novel.memory.services.impl;

import com.deusexmachina.novel.memory.models.WorldMemory;
import com.deusexmachina.novel.memory.services.WorldMemoryService;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of WorldMemoryService using Firestore.
 */
@Singleton
public class WorldMemoryServiceImpl implements WorldMemoryService {
    private static final Logger logger = Logger.getLogger(WorldMemoryServiceImpl.class.getName());
    private static final String COLLECTION_NAME = "worldMemory";
    
    private final Firestore firestore;
    
    @Inject
    public WorldMemoryServiceImpl(Firestore firestore) {
        this.firestore = firestore;
    }
    
    @Override
    public CompletableFuture<WorldMemory> saveWorldMemory(WorldMemory memory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (memory.getWorldId() == null) {
                    memory.setWorldId(UUID.randomUUID().toString());
                }
                
                DocumentReference docRef = firestore
                    .collection("projects").document(memory.getProjectId())
                    .collection(COLLECTION_NAME).document(memory.getWorldId());
                
                ApiFuture<WriteResult> future = docRef.set(memory);
                future.get();
                
                logger.info("Saved world memory: " + memory.getWorldId());
                return memory;
            } catch (Exception e) {
                logger.severe("Error saving world memory: " + e.getMessage());
                throw new RuntimeException("Failed to save world memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<WorldMemory>> getProjectWorldMemory(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CollectionReference collection = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME);
                
                ApiFuture<QuerySnapshot> future = collection.get();
                QuerySnapshot snapshot = future.get();
                
                List<WorldMemory> worldMemories = snapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(WorldMemory.class))
                    .collect(Collectors.toList());
                
                logger.info("Retrieved " + worldMemories.size() + " world memories for project: " + projectId);
                return worldMemories;
            } catch (Exception e) {
                logger.severe("Error getting project world memory: " + e.getMessage());
                throw new RuntimeException("Failed to get project world memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<WorldMemory>> getWorldMemoryByCategory(String projectId, String category) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Query query = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME)
                    .whereEqualTo("category", category);
                
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot snapshot = future.get();
                
                List<WorldMemory> memories = snapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(WorldMemory.class))
                    .collect(Collectors.toList());
                
                logger.info("Retrieved " + memories.size() + " world memories for category: " + category);
                return memories;
            } catch (Exception e) {
                logger.severe("Error getting world memory by category: " + e.getMessage());
                throw new RuntimeException("Failed to get world memory by category", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<WorldMemory> addWorldFact(String projectId, String category,
            WorldMemory.WorldFact fact) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Find or create world memory for this category
                List<WorldMemory> categoryMemories = getWorldMemoryByCategory(projectId, category).join();
                WorldMemory memory;
                
                if (categoryMemories.isEmpty()) {
                    // Create new world memory for this category
                    memory = WorldMemory.builder()
                        .worldId(UUID.randomUUID().toString())
                        .projectId(projectId)
                        .category(category)
                        .facts(new ArrayList<>())
                        .factCategories(new HashMap<>())
                        .locations(new ArrayList<>())
                        .timeline(new ArrayList<>())
                        .consistencyRules(new ArrayList<>())
                        .detectedContradictions(new ArrayList<>())
                        .build();
                } else {
                    memory = categoryMemories.get(0);
                    if (memory.getFacts() == null) {
                        memory.setFacts(new ArrayList<>());
                    }
                }
                
                if (fact.getFactId() == null) {
                    fact.setFactId(UUID.randomUUID().toString());
                }
                if (fact.getCreatedAt() == null) {
                    fact.setCreatedAt(Timestamp.now());
                }
                
                memory.getFacts().add(fact);
                
                // Update fact categories
                if (memory.getFactCategories() == null) {
                    memory.setFactCategories(new HashMap<>());
                }
                memory.getFactCategories().computeIfAbsent(fact.getCategory(), k -> new ArrayList<>())
                    .add(fact.getFactId());
                
                return saveWorldMemory(memory).join();
            } catch (Exception e) {
                logger.severe("Error adding world fact: " + e.getMessage());
                throw new RuntimeException("Failed to add world fact", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<WorldMemory> addLocation(String projectId, WorldMemory.Location location) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Find or create world memory for locations
                List<WorldMemory> locationMemories = getWorldMemoryByCategory(projectId, "location").join();
                WorldMemory memory;
                
                if (locationMemories.isEmpty()) {
                    memory = WorldMemory.builder()
                        .worldId(UUID.randomUUID().toString())
                        .projectId(projectId)
                        .category("location")
                        .facts(new ArrayList<>())
                        .locations(new ArrayList<>())
                        .locationHierarchy(new HashMap<>())
                        .build();
                } else {
                    memory = locationMemories.get(0);
                    if (memory.getLocations() == null) {
                        memory.setLocations(new ArrayList<>());
                    }
                }
                
                if (location.getLocationId() == null) {
                    location.setLocationId(UUID.randomUUID().toString());
                }
                
                memory.getLocations().add(location);
                
                // Update location hierarchy
                if (location.getParentLocationId() != null) {
                    if (memory.getLocationHierarchy() == null) {
                        memory.setLocationHierarchy(new HashMap<>());
                    }
                    memory.getLocationHierarchy().put(location.getLocationId(), location.getParentLocationId());
                }
                
                return saveWorldMemory(memory).join();
            } catch (Exception e) {
                logger.severe("Error adding location: " + e.getMessage());
                throw new RuntimeException("Failed to add location", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<WorldMemory.Location> getLocation(String projectId, String locationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<WorldMemory> locationMemories = getWorldMemoryByCategory(projectId, "location").join();
                
                for (WorldMemory memory : locationMemories) {
                    if (memory.getLocations() != null) {
                        for (WorldMemory.Location location : memory.getLocations()) {
                            if (locationId.equals(location.getLocationId())) {
                                return location;
                            }
                        }
                    }
                }
                
                logger.warning("Location not found: " + locationId);
                return null;
            } catch (Exception e) {
                logger.severe("Error getting location: " + e.getMessage());
                throw new RuntimeException("Failed to get location", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<WorldMemory> addHistoricalEvent(String projectId,
            WorldMemory.HistoricalEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Find or create world memory for history
                List<WorldMemory> historyMemories = getWorldMemoryByCategory(projectId, "history").join();
                WorldMemory memory;
                
                if (historyMemories.isEmpty()) {
                    memory = WorldMemory.builder()
                        .worldId(UUID.randomUUID().toString())
                        .projectId(projectId)
                        .category("history")
                        .timeline(new ArrayList<>())
                        .build();
                } else {
                    memory = historyMemories.get(0);
                    if (memory.getTimeline() == null) {
                        memory.setTimeline(new ArrayList<>());
                    }
                }
                
                if (event.getEventId() == null) {
                    event.setEventId(UUID.randomUUID().toString());
                }
                
                memory.getTimeline().add(event);
                
                // Sort timeline by years before present
                memory.getTimeline().sort((a, b) -> {
                    if (a.getYearsBeforePresent() != null && b.getYearsBeforePresent() != null) {
                        return b.getYearsBeforePresent().compareTo(a.getYearsBeforePresent());
                    }
                    return 0;
                });
                
                return saveWorldMemory(memory).join();
            } catch (Exception e) {
                logger.severe("Error adding historical event: " + e.getMessage());
                throw new RuntimeException("Failed to add historical event", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<WorldMemory.Contradiction>> validateConsistency(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<WorldMemory> allMemories = getProjectWorldMemory(projectId).join();
                List<WorldMemory.Contradiction> contradictions = new ArrayList<>();
                
                // Collect all facts
                List<WorldMemory.WorldFact> allFacts = new ArrayList<>();
                for (WorldMemory memory : allMemories) {
                    if (memory.getFacts() != null) {
                        allFacts.addAll(memory.getFacts());
                    }
                }
                
                // Simple contradiction detection - look for conflicting facts
                for (int i = 0; i < allFacts.size(); i++) {
                    for (int j = i + 1; j < allFacts.size(); j++) {
                        WorldMemory.WorldFact fact1 = allFacts.get(i);
                        WorldMemory.WorldFact fact2 = allFacts.get(j);
                        
                        // Check if facts contradict (simplified logic)
                        if (factsContradict(fact1, fact2)) {
                            WorldMemory.Contradiction contradiction = WorldMemory.Contradiction.builder()
                                .contradictionId(UUID.randomUUID().toString())
                                .factId1(fact1.getFactId())
                                .factId2(fact2.getFactId())
                                .description("Conflicting facts: " + fact1.getFact() + " vs " + fact2.getFact())
                                .severity("major")
                                .resolved(false)
                                .detectedAt(Timestamp.now())
                                .build();
                            contradictions.add(contradiction);
                        }
                    }
                }
                
                // Save detected contradictions
                for (WorldMemory memory : allMemories) {
                    if (!contradictions.isEmpty()) {
                        if (memory.getDetectedContradictions() == null) {
                            memory.setDetectedContradictions(new ArrayList<>());
                        }
                        memory.getDetectedContradictions().addAll(contradictions);
                        saveWorldMemory(memory).join();
                    }
                }
                
                logger.info("Detected " + contradictions.size() + " contradictions");
                return contradictions;
            } catch (Exception e) {
                logger.severe("Error validating consistency: " + e.getMessage());
                throw new RuntimeException("Failed to validate consistency", e);
            }
        });
    }
    
    private boolean factsContradict(WorldMemory.WorldFact fact1, WorldMemory.WorldFact fact2) {
        // Simplified contradiction detection
        // In a real implementation, this would use NLP or more sophisticated logic
        String f1Lower = fact1.getFact().toLowerCase();
        String f2Lower = fact2.getFact().toLowerCase();
        
        // Check for direct negation patterns
        if ((f1Lower.contains("not") && !f2Lower.contains("not") && 
             f1Lower.replace("not ", "").trim().equals(f2Lower)) ||
            (!f1Lower.contains("not") && f2Lower.contains("not") && 
             f2Lower.replace("not ", "").trim().equals(f1Lower))) {
            return true;
        }
        
        // Check for mutually exclusive states
        String[] opposites = {"alive:dead", "open:closed", "hot:cold", "day:night"};
        for (String pair : opposites) {
            String[] parts = pair.split(":");
            if ((f1Lower.contains(parts[0]) && f2Lower.contains(parts[1])) ||
                (f1Lower.contains(parts[1]) && f2Lower.contains(parts[0]))) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public CompletableFuture<WorldMemory> addContradiction(String projectId,
            WorldMemory.Contradiction contradiction) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Add to first world memory found
                List<WorldMemory> memories = getProjectWorldMemory(projectId).join();
                if (memories.isEmpty()) {
                    throw new RuntimeException("No world memory found for project");
                }
                
                WorldMemory memory = memories.get(0);
                if (memory.getDetectedContradictions() == null) {
                    memory.setDetectedContradictions(new ArrayList<>());
                }
                
                if (contradiction.getContradictionId() == null) {
                    contradiction.setContradictionId(UUID.randomUUID().toString());
                }
                if (contradiction.getDetectedAt() == null) {
                    contradiction.setDetectedAt(Timestamp.now());
                }
                
                memory.getDetectedContradictions().add(contradiction);
                
                return saveWorldMemory(memory).join();
            } catch (Exception e) {
                logger.severe("Error adding contradiction: " + e.getMessage());
                throw new RuntimeException("Failed to add contradiction", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<WorldMemory.WorldFact>> searchFacts(String projectId, String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<WorldMemory> memories = getProjectWorldMemory(projectId).join();
                List<WorldMemory.WorldFact> matchingFacts = new ArrayList<>();
                String queryLower = query.toLowerCase();
                
                for (WorldMemory memory : memories) {
                    if (memory.getFacts() != null) {
                        memory.getFacts().stream()
                            .filter(fact -> fact.getFact().toLowerCase().contains(queryLower) ||
                                          (fact.getCategory() != null && fact.getCategory().toLowerCase().contains(queryLower)))
                            .forEach(matchingFacts::add);
                    }
                }
                
                logger.info("Found " + matchingFacts.size() + " facts matching query: " + query);
                return matchingFacts;
            } catch (Exception e) {
                logger.severe("Error searching facts: " + e.getMessage());
                throw new RuntimeException("Failed to search facts", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<WorldMemory.WorldFact>> getLocationFacts(String projectId, String locationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                WorldMemory.Location location = getLocation(projectId, locationId).join();
                if (location == null) {
                    return new ArrayList<>();
                }
                
                // Search facts that mention this location
                return searchFacts(projectId, location.getName()).join();
            } catch (Exception e) {
                logger.severe("Error getting location facts: " + e.getMessage());
                throw new RuntimeException("Failed to get location facts", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteWorldMemory(String projectId, String worldId) {
        return CompletableFuture.runAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(worldId);
                
                ApiFuture<WriteResult> future = docRef.delete();
                future.get();
                
                logger.info("Deleted world memory: " + worldId);
            } catch (Exception e) {
                logger.severe("Error deleting world memory: " + e.getMessage());
                throw new RuntimeException("Failed to delete world memory", e);
            }
        });
    }
}