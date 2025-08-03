package com.deusexmachina.novel.memory.services.impl;

import com.deusexmachina.novel.memory.models.CharacterMemory;
import com.deusexmachina.novel.memory.services.CharacterMemoryService;
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
 * Implementation of CharacterMemoryService using Firestore.
 */
@Singleton
public class CharacterMemoryServiceImpl implements CharacterMemoryService {
    private static final Logger logger = Logger.getLogger(CharacterMemoryServiceImpl.class.getName());
    private static final String COLLECTION_NAME = "characterMemory";
    
    private final Firestore firestore;
    
    @Inject
    public CharacterMemoryServiceImpl(Firestore firestore) {
        this.firestore = firestore;
    }
    
    @Override
    public CompletableFuture<CharacterMemory> saveCharacterMemory(CharacterMemory memory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (memory.getCharacterId() == null) {
                    memory.setCharacterId(UUID.randomUUID().toString());
                }
                
                // Create document reference
                DocumentReference docRef = firestore
                    .collection("projects").document(memory.getProjectId())
                    .collection(COLLECTION_NAME).document(memory.getCharacterId());
                
                // Save to Firestore
                ApiFuture<WriteResult> future = docRef.set(memory);
                future.get();
                
                logger.info("Saved character memory: " + memory.getCharacterId());
                return memory;
            } catch (Exception e) {
                logger.severe("Error saving character memory: " + e.getMessage());
                throw new RuntimeException("Failed to save character memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<CharacterMemory> getCharacterMemory(String projectId, String characterId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(characterId);
                
                ApiFuture<DocumentSnapshot> future = docRef.get();
                DocumentSnapshot document = future.get();
                
                if (document.exists()) {
                    CharacterMemory memory = document.toObject(CharacterMemory.class);
                    logger.info("Retrieved character memory: " + characterId);
                    return memory;
                } else {
                    logger.warning("Character memory not found: " + characterId);
                    return null;
                }
            } catch (Exception e) {
                logger.severe("Error getting character memory: " + e.getMessage());
                throw new RuntimeException("Failed to get character memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<CharacterMemory> updateCharacterState(String projectId, String characterId,
            CharacterMemory.CharacterState newState) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(characterId);
                
                newState.setStateTimestamp(Timestamp.now());
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("currentState", newState);
                updates.put("updatedAt", FieldValue.serverTimestamp());
                
                ApiFuture<WriteResult> future = docRef.update(updates);
                future.get();
                
                logger.info("Updated character state for: " + characterId);
                
                // Return updated character memory
                return getCharacterMemory(projectId, characterId).join();
            } catch (Exception e) {
                logger.severe("Error updating character state: " + e.getMessage());
                throw new RuntimeException("Failed to update character state", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<CharacterMemory> addObservation(String projectId, String characterId,
            CharacterMemory.CharacterObservation observation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(characterId);
                
                if (observation.getObservationId() == null) {
                    observation.setObservationId(UUID.randomUUID().toString());
                }
                if (observation.getTimestamp() == null) {
                    observation.setTimestamp(Timestamp.now());
                }
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("observations", FieldValue.arrayUnion(observation));
                updates.put("updatedAt", FieldValue.serverTimestamp());
                
                // Update recent scenes if provided
                if (observation.getSceneId() != null) {
                    updates.put("recentSceneIds", FieldValue.arrayUnion(observation.getSceneId()));
                }
                
                ApiFuture<WriteResult> future = docRef.update(updates);
                future.get();
                
                logger.info("Added observation to character: " + characterId);
                
                return getCharacterMemory(projectId, characterId).join();
            } catch (Exception e) {
                logger.severe("Error adding observation: " + e.getMessage());
                throw new RuntimeException("Failed to add observation", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<CharacterMemory> addReflection(String projectId, String characterId,
            CharacterMemory.CharacterReflection reflection) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(characterId);
                
                if (reflection.getReflectionId() == null) {
                    reflection.setReflectionId(UUID.randomUUID().toString());
                }
                if (reflection.getTimestamp() == null) {
                    reflection.setTimestamp(Timestamp.now());
                }
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("reflections", FieldValue.arrayUnion(reflection));
                updates.put("updatedAt", FieldValue.serverTimestamp());
                
                ApiFuture<WriteResult> future = docRef.update(updates);
                future.get();
                
                logger.info("Added reflection to character: " + characterId);
                
                return getCharacterMemory(projectId, characterId).join();
            } catch (Exception e) {
                logger.severe("Error adding reflection: " + e.getMessage());
                throw new RuntimeException("Failed to add reflection", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<CharacterMemory>> getProjectCharacters(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CollectionReference collection = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME);
                
                ApiFuture<QuerySnapshot> future = collection.get();
                QuerySnapshot snapshot = future.get();
                
                List<CharacterMemory> characters = snapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(CharacterMemory.class))
                    .collect(Collectors.toList());
                
                logger.info("Retrieved " + characters.size() + " characters for project: " + projectId);
                return characters;
            } catch (Exception e) {
                logger.severe("Error getting project characters: " + e.getMessage());
                throw new RuntimeException("Failed to get project characters", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<CharacterMemory>> getSceneCharacters(String projectId, String sceneId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Query query = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME)
                    .whereArrayContains("recentSceneIds", sceneId);
                
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot snapshot = future.get();
                
                List<CharacterMemory> characters = snapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(CharacterMemory.class))
                    .collect(Collectors.toList());
                
                logger.info("Found " + characters.size() + " characters in scene: " + sceneId);
                return characters;
            } catch (Exception e) {
                logger.severe("Error getting scene characters: " + e.getMessage());
                throw new RuntimeException("Failed to get scene characters", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<CharacterMemory.CharacterObservation>> getCharacterTimeline(
            String projectId, String characterId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CharacterMemory memory = getCharacterMemory(projectId, characterId).join();
                if (memory == null || memory.getObservations() == null) {
                    return new ArrayList<>();
                }
                
                // Sort observations by scene number (most recent first)
                List<CharacterMemory.CharacterObservation> timeline = memory.getObservations().stream()
                    .sorted((a, b) -> {
                        if (a.getSceneNumber() != null && b.getSceneNumber() != null) {
                            return b.getSceneNumber().compareTo(a.getSceneNumber());
                        }
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
                
                logger.info("Retrieved timeline for character: " + characterId);
                return timeline;
            } catch (Exception e) {
                logger.severe("Error getting character timeline: " + e.getMessage());
                throw new RuntimeException("Failed to get character timeline", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<CharacterMemory> updateRelationships(String projectId, String characterId,
            String otherCharacterId, String relationshipType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(characterId);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("relationships." + otherCharacterId, relationshipType);
                updates.put("updatedAt", FieldValue.serverTimestamp());
                
                ApiFuture<WriteResult> future = docRef.update(updates);
                future.get();
                
                logger.info("Updated relationship for character: " + characterId);
                
                return getCharacterMemory(projectId, characterId).join();
            } catch (Exception e) {
                logger.severe("Error updating relationships: " + e.getMessage());
                throw new RuntimeException("Failed to update relationships", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteCharacterMemory(String projectId, String characterId) {
        return CompletableFuture.runAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(characterId);
                
                ApiFuture<WriteResult> future = docRef.delete();
                future.get();
                
                logger.info("Deleted character memory: " + characterId);
            } catch (Exception e) {
                logger.severe("Error deleting character memory: " + e.getMessage());
                throw new RuntimeException("Failed to delete character memory", e);
            }
        });
    }
}