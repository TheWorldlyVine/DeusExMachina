package com.deusexmachina.novel.memory.services.impl;

import com.deusexmachina.novel.memory.models.PlotMemory;
import com.deusexmachina.novel.memory.services.PlotMemoryService;
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
 * Implementation of PlotMemoryService using Firestore.
 */
@Singleton
public class PlotMemoryServiceImpl implements PlotMemoryService {
    private static final Logger logger = Logger.getLogger(PlotMemoryServiceImpl.class.getName());
    private static final String COLLECTION_NAME = "plotMemory";
    
    private final Firestore firestore;
    
    @Inject
    public PlotMemoryServiceImpl(Firestore firestore) {
        this.firestore = firestore;
    }
    
    @Override
    public CompletableFuture<PlotMemory> savePlotMemory(PlotMemory memory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (memory.getPlotId() == null) {
                    memory.setPlotId(UUID.randomUUID().toString());
                }
                
                DocumentReference docRef = firestore
                    .collection("projects").document(memory.getProjectId())
                    .collection(COLLECTION_NAME).document(memory.getPlotId());
                
                ApiFuture<WriteResult> future = docRef.set(memory);
                future.get();
                
                logger.info("Saved plot memory: " + memory.getPlotId());
                return memory;
            } catch (Exception e) {
                logger.severe("Error saving plot memory: " + e.getMessage());
                throw new RuntimeException("Failed to save plot memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<PlotMemory>> getProjectPlots(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CollectionReference collection = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME);
                
                ApiFuture<QuerySnapshot> future = collection.get();
                QuerySnapshot snapshot = future.get();
                
                List<PlotMemory> plots = snapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(PlotMemory.class))
                    .collect(Collectors.toList());
                
                logger.info("Retrieved " + plots.size() + " plots for project: " + projectId);
                return plots;
            } catch (Exception e) {
                logger.severe("Error getting project plots: " + e.getMessage());
                throw new RuntimeException("Failed to get project plots", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<PlotMemory> getPlotMemory(String projectId, String plotId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(plotId);
                
                ApiFuture<DocumentSnapshot> future = docRef.get();
                DocumentSnapshot document = future.get();
                
                if (document.exists()) {
                    PlotMemory memory = document.toObject(PlotMemory.class);
                    logger.info("Retrieved plot memory: " + plotId);
                    return memory;
                } else {
                    logger.warning("Plot memory not found: " + plotId);
                    return null;
                }
            } catch (Exception e) {
                logger.severe("Error getting plot memory: " + e.getMessage());
                throw new RuntimeException("Failed to get plot memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<PlotMemory> getMainPlot(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Query query = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME)
                    .whereEqualTo("threadType", "main")
                    .limit(1);
                
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot snapshot = future.get();
                
                if (!snapshot.isEmpty()) {
                    PlotMemory mainPlot = snapshot.getDocuments().get(0).toObject(PlotMemory.class);
                    logger.info("Retrieved main plot for project: " + projectId);
                    return mainPlot;
                } else {
                    logger.warning("No main plot found for project: " + projectId);
                    return null;
                }
            } catch (Exception e) {
                logger.severe("Error getting main plot: " + e.getMessage());
                throw new RuntimeException("Failed to get main plot", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<PlotMemory> addPlotPoint(String projectId, String plotId,
            PlotMemory.PlotPoint plotPoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(plotId);
                
                if (plotPoint.getPointId() == null) {
                    plotPoint.setPointId(UUID.randomUUID().toString());
                }
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("plotPoints", FieldValue.arrayUnion(plotPoint));
                updates.put("updatedAt", FieldValue.serverTimestamp());
                
                ApiFuture<WriteResult> future = docRef.update(updates);
                future.get();
                
                logger.info("Added plot point to plot: " + plotId);
                
                return getPlotMemory(projectId, plotId).join();
            } catch (Exception e) {
                logger.severe("Error adding plot point: " + e.getMessage());
                throw new RuntimeException("Failed to add plot point", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<PlotMemory> addMilestone(String projectId, String plotId,
            PlotMemory.Milestone milestone) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(plotId);
                
                if (milestone.getMilestoneId() == null) {
                    milestone.setMilestoneId(UUID.randomUUID().toString());
                }
                if (milestone.getAchievedAt() == null) {
                    milestone.setAchievedAt(Timestamp.now());
                }
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("milestones", FieldValue.arrayUnion(milestone));
                updates.put("updatedAt", FieldValue.serverTimestamp());
                
                ApiFuture<WriteResult> future = docRef.update(updates);
                future.get();
                
                logger.info("Added milestone to plot: " + plotId);
                
                return getPlotMemory(projectId, plotId).join();
            } catch (Exception e) {
                logger.severe("Error adding milestone: " + e.getMessage());
                throw new RuntimeException("Failed to add milestone", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<PlotMemory> updateTension(String projectId, String plotId,
            Long chapterNumber, Integer tensionLevel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(plotId);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("tensionLevel", tensionLevel);
                updates.put("chapterTension." + chapterNumber, tensionLevel);
                updates.put("updatedAt", FieldValue.serverTimestamp());
                
                ApiFuture<WriteResult> future = docRef.update(updates);
                future.get();
                
                logger.info("Updated tension level for plot: " + plotId);
                
                return getPlotMemory(projectId, plotId).join();
            } catch (Exception e) {
                logger.severe("Error updating tension: " + e.getMessage());
                throw new RuntimeException("Failed to update tension", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<PlotMemory>> getActiveThreads(String projectId, Long chapterNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Query query = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME)
                    .whereNotEqualTo("status", "completed");
                
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot snapshot = future.get();
                
                List<PlotMemory> activeThreads = snapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(PlotMemory.class))
                    .filter(plot -> isActiveInChapter(plot, chapterNumber))
                    .collect(Collectors.toList());
                
                logger.info("Found " + activeThreads.size() + " active threads for chapter: " + chapterNumber);
                return activeThreads;
            } catch (Exception e) {
                logger.severe("Error getting active threads: " + e.getMessage());
                throw new RuntimeException("Failed to get active threads", e);
            }
        });
    }
    
    private boolean isActiveInChapter(PlotMemory plot, Long chapterNumber) {
        // Check if any plot points are near this chapter
        if (plot.getPlotPoints() != null) {
            return plot.getPlotPoints().stream()
                .anyMatch(pp -> pp.getTargetChapter() != null && 
                         Math.abs(pp.getTargetChapter() - chapterNumber) <= 2);
        }
        return "development".equals(plot.getStatus()) || "climax".equals(plot.getStatus());
    }
    
    @Override
    public CompletableFuture<PlotMemory> updateThreadStatus(String projectId, String plotId, String status) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(plotId);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", status);
                updates.put("updatedAt", FieldValue.serverTimestamp());
                
                ApiFuture<WriteResult> future = docRef.update(updates);
                future.get();
                
                logger.info("Updated status for plot: " + plotId + " to: " + status);
                
                return getPlotMemory(projectId, plotId).join();
            } catch (Exception e) {
                logger.severe("Error updating thread status: " + e.getMessage());
                throw new RuntimeException("Failed to update thread status", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<PlotMemory.PlotPoint>> getUpcomingPlotPoints(
            String projectId, Long currentChapter, int chaptersAhead) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<PlotMemory> plots = getProjectPlots(projectId).join();
                List<PlotMemory.PlotPoint> upcomingPoints = new ArrayList<>();
                
                for (PlotMemory plot : plots) {
                    if (plot.getPlotPoints() != null) {
                        plot.getPlotPoints().stream()
                            .filter(pp -> pp.getTargetChapter() != null &&
                                   pp.getTargetChapter() > currentChapter &&
                                   pp.getTargetChapter() <= currentChapter + chaptersAhead &&
                                   !"written".equals(pp.getStatus()))
                            .forEach(upcomingPoints::add);
                    }
                }
                
                // Sort by target chapter
                upcomingPoints.sort((a, b) -> a.getTargetChapter().compareTo(b.getTargetChapter()));
                
                logger.info("Found " + upcomingPoints.size() + " upcoming plot points");
                return upcomingPoints;
            } catch (Exception e) {
                logger.severe("Error getting upcoming plot points: " + e.getMessage());
                throw new RuntimeException("Failed to get upcoming plot points", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deletePlotMemory(String projectId, String plotId) {
        return CompletableFuture.runAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("projects").document(projectId)
                    .collection(COLLECTION_NAME).document(plotId);
                
                ApiFuture<WriteResult> future = docRef.delete();
                future.get();
                
                logger.info("Deleted plot memory: " + plotId);
            } catch (Exception e) {
                logger.severe("Error deleting plot memory: " + e.getMessage());
                throw new RuntimeException("Failed to delete plot memory", e);
            }
        });
    }
}