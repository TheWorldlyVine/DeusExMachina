package com.deusexmachina.novel.memory.services;

import com.deusexmachina.novel.memory.models.PlotMemory;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing plot memory operations.
 */
public interface PlotMemoryService {
    
    /**
     * Create or update a plot thread.
     */
    CompletableFuture<PlotMemory> savePlotMemory(PlotMemory memory);
    
    /**
     * Get all plot threads for a project.
     */
    CompletableFuture<List<PlotMemory>> getProjectPlots(String projectId);
    
    /**
     * Get a specific plot thread.
     */
    CompletableFuture<PlotMemory> getPlotMemory(String projectId, String plotId);
    
    /**
     * Get main plot thread for a project.
     */
    CompletableFuture<PlotMemory> getMainPlot(String projectId);
    
    /**
     * Add a plot point to a thread.
     */
    CompletableFuture<PlotMemory> addPlotPoint(String projectId, String plotId,
            PlotMemory.PlotPoint plotPoint);
    
    /**
     * Add a milestone to a plot thread.
     */
    CompletableFuture<PlotMemory> addMilestone(String projectId, String plotId,
            PlotMemory.Milestone milestone);
    
    /**
     * Update plot tension level.
     */
    CompletableFuture<PlotMemory> updateTension(String projectId, String plotId,
            Long chapterNumber, Integer tensionLevel);
    
    /**
     * Get active plot threads for current chapter.
     */
    CompletableFuture<List<PlotMemory>> getActiveThreads(String projectId, Long chapterNumber);
    
    /**
     * Update plot thread status.
     */
    CompletableFuture<PlotMemory> updateThreadStatus(String projectId, String plotId, String status);
    
    /**
     * Get upcoming plot points within N chapters.
     */
    CompletableFuture<List<PlotMemory.PlotPoint>> getUpcomingPlotPoints(
            String projectId, Long currentChapter, int chaptersAhead);
    
    /**
     * Delete a plot thread.
     */
    CompletableFuture<Void> deletePlotMemory(String projectId, String plotId);
}