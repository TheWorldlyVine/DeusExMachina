import { requireAuth, checkProjectAccess } from '../context';
import { ResolverContext } from '../types/resolver';

export const memoryResolvers = {
  Query: {
    // Character queries
    character: async (
      _: any,
      { projectId, characterId }: { projectId: string; characterId: string },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.getCharacter(projectId, characterId);
    },
    
    characters: async (_: any, { projectId }: { projectId: string }, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.getCharacters(projectId);
    },
    
    characterTimeline: async (
      _: any,
      { projectId, characterId, limit }: { projectId: string; characterId: string; limit?: number },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.getCharacterTimeline(projectId, characterId, limit);
    },
    
    // Plot queries
    plot: async (
      _: any,
      { projectId, plotId }: { projectId: string; plotId: string },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.getPlot(projectId, plotId);
    },
    
    plots: async (_: any, { projectId }: { projectId: string }, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.getPlots(projectId);
    },
    
    mainPlot: async (_: any, { projectId }: { projectId: string }, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      const plots = await context.dataSources.memoryAPI.getPlots(projectId);
      return plots.find((plot: any) => plot.threadType === 'MAIN');
    },
    
    activePlots: async (
      _: any,
      { projectId, chapterNumber }: { projectId: string; chapterNumber: number },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      const plots = await context.dataSources.memoryAPI.getPlots(projectId);
      // Filter active plots based on chapter
      return plots.filter((plot: any) => 
        plot.status !== 'COMPLETED' && plot.status !== 'RESOLUTION'
      );
    },
    
    // World queries
    worldMemory: async (_: any, { projectId }: { projectId: string }, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.getWorldMemory(projectId);
    },
    
    worldFacts: async (
      _: any,
      { projectId, category }: { projectId: string; category?: string },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      const worldMemories = await context.dataSources.memoryAPI.getWorldMemory(projectId);
      let allFacts: any[] = [];
      
      worldMemories.forEach((memory: any) => {
        if (!category || memory.category === category) {
          allFacts = allFacts.concat(memory.facts || []);
        }
      });
      
      return allFacts;
    },
    
    location: async (
      _: any,
      { projectId, locationId }: { projectId: string; locationId: string },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.getLocation(projectId, locationId);
    },
    
    // Context query
    generationContext: async (
      _: any,
      args: { projectId: string; sceneId: string; chapterNumber: number; sceneNumber: number },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, args.projectId, 'VIEWER');
      return context.dataSources.memoryAPI.getGenerationContext(
        args.projectId,
        args.sceneId,
        args.chapterNumber,
        args.sceneNumber
      );
    },
    
    // Search
    searchMemory: async (
      _: any,
      { projectId, query, type }: { projectId: string; query: string; type?: string },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.searchMemory(projectId, query, type);
    },
  },
  
  Mutation: {
    // Character mutations
    createCharacter: async (_: any, { input }: any, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, input.projectId, 'EDITOR');
      return context.dataSources.memoryAPI.createCharacter(input);
    },
    
    updateCharacterState: async (
      _: any,
      { projectId, characterId, state }: any,
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'EDITOR');
      return context.dataSources.memoryAPI.updateCharacterState(projectId, characterId, state);
    },
    
    addCharacterObservation: async (
      _: any,
      { projectId, characterId, observation }: any,
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'EDITOR');
      return context.dataSources.memoryAPI.addCharacterObservation(
        projectId,
        characterId,
        observation
      );
    },
    
    updateCharacterRelationship: async (
      _: any,
      { projectId, characterId, otherCharacterId, relationship }: any,
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'EDITOR');
      // This would need to be implemented in the memory service
      throw new Error('Not implemented yet');
    },
    
    // Plot mutations
    createPlot: async (_: any, { input }: any, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, input.projectId, 'EDITOR');
      return context.dataSources.memoryAPI.createPlot(input);
    },
    
    addPlotPoint: async (
      _: any,
      { projectId, plotId, plotPoint }: any,
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'EDITOR');
      return context.dataSources.memoryAPI.addPlotPoint(projectId, plotId, plotPoint);
    },
    
    addPlotMilestone: async (
      _: any,
      { projectId, plotId, milestone }: any,
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'EDITOR');
      return context.dataSources.memoryAPI.addPlotMilestone(projectId, plotId, milestone);
    },
    
    updatePlotTension: async (
      _: any,
      { projectId, plotId, chapterNumber, tensionLevel }: any,
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'EDITOR');
      return context.dataSources.memoryAPI.updatePlotTension(
        projectId,
        plotId,
        chapterNumber,
        tensionLevel
      );
    },
    
    // World mutations
    addWorldFact: async (
      _: any,
      { projectId, category, fact }: any,
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'EDITOR');
      return context.dataSources.memoryAPI.addWorldFact(projectId, category, fact);
    },
    
    addLocation: async (_: any, { projectId, location }: any, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'EDITOR');
      return context.dataSources.memoryAPI.addLocation(projectId, location);
    },
    
    validateWorldConsistency: async (
      _: any,
      { projectId }: { projectId: string },
      context: ResolverContext
    ) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.memoryAPI.validateWorldConsistency(projectId);
    },
  },
  
  CharacterMemory: {
    // Additional resolvers for computed fields if needed
  },
  
  PlotMemory: {
    // Transform backend fields to match frontend expectations
    title: (parent: any) => parent.threadName || parent.title || 'Untitled Plot',
    description: (parent: any) => parent.premise || parent.description || '',
    storyArc: (parent: any) => parent.centralConflict || parent.storyArc || '',
    currentState: (parent: any) => parent.currentState || {
      status: parent.status || 'SETUP',
      tensionLevel: parent.tensionLevel || 0,
      lastUpdated: parent.updatedAt || new Date().toISOString(),
    },
    keyMoments: (parent: any) => parent.keyMoments || parent.milestones?.map((m: any) => ({
      momentId: m.milestoneId,
      chapterNumber: m.chapterNumber,
      sceneNumber: 0,
      momentType: 'MILESTONE',
      description: m.description,
      impact: m.impact,
      timestamp: m.achievedAt,
    })) || [],
    involvedCharacters: (parent: any) => parent.involvedCharacters || [],
    conflicts: (parent: any) => parent.conflicts || [{
      type: 'CENTRAL',
      description: parent.centralConflict || '',
      resolved: false,
      resolution: null,
    }],
    relatedSubplots: (parent: any) => parent.relatedSubplots || [],
    foreshadowing: (parent: any) => parent.foreshadowing || [],
    metadata: (parent: any) => parent.metadata || {},
  },
  
  WorldMemory: {
    // Additional resolvers for computed fields if needed
  },
};