import { requireAuth, checkProjectAccess } from '../context';
import { ResolverContext } from '../types/resolver';

export const documentResolvers = {
  Query: {
    document: async (_: any, { id }: { id: string }, context: ResolverContext) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(id);
      await checkProjectAccess(context, document.projectId, 'VIEWER');
      return document;
    },
    
    documents: async (_: any, { projectId }: { projectId: string }, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, projectId, 'VIEWER');
      return context.dataSources.documentAPI.getDocuments(projectId);
    },
    
    chapter: async (
      _: any,
      { documentId, chapterNumber }: { documentId: string; chapterNumber: number },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(documentId);
      await checkProjectAccess(context, document.projectId, 'VIEWER');
      return context.dataSources.documentAPI.getChapter(documentId, chapterNumber);
    },
    
    scene: async (
      _: any,
      args: { documentId: string; chapterNumber: number; sceneNumber: number },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(args.documentId);
      await checkProjectAccess(context, document.projectId, 'VIEWER');
      return context.dataSources.documentAPI.getScene(
        args.documentId,
        args.chapterNumber,
        args.sceneNumber
      );
    },
  },
  
  Mutation: {
    createDocument: async (_: any, { input }: any, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, input.projectId, 'EDITOR');
      return context.dataSources.documentAPI.createDocument(input);
    },
    
    updateDocument: async (
      _: any,
      { id, input }: { id: string; input: any },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(id);
      await checkProjectAccess(context, document.projectId, 'EDITOR');
      return context.dataSources.documentAPI.updateDocument(id, input);
    },
    
    deleteDocument: async (_: any, { id }: { id: string }, context: ResolverContext) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(id);
      await checkProjectAccess(context, document.projectId, 'OWNER');
      return context.dataSources.documentAPI.deleteDocument(id);
    },
    
    createChapter: async (
      _: any,
      { documentId, input }: { documentId: string; input: any },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(documentId);
      await checkProjectAccess(context, document.projectId, 'EDITOR');
      return context.dataSources.documentAPI.createChapter(documentId, input);
    },
    
    updateChapter: async (
      _: any,
      args: { documentId: string; chapterNumber: number; input: any },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(args.documentId);
      await checkProjectAccess(context, document.projectId, 'EDITOR');
      return context.dataSources.documentAPI.updateChapter(
        args.documentId,
        args.chapterNumber,
        args.input
      );
    },
    
    deleteChapter: async (
      _: any,
      { documentId, chapterNumber }: { documentId: string; chapterNumber: number },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(documentId);
      await checkProjectAccess(context, document.projectId, 'EDITOR');
      return context.dataSources.documentAPI.deleteChapter(documentId, chapterNumber);
    },
    
    createScene: async (
      _: any,
      args: { documentId: string; chapterNumber: number; input: any },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(args.documentId);
      await checkProjectAccess(context, document.projectId, 'EDITOR');
      return context.dataSources.documentAPI.createScene(
        args.documentId,
        args.chapterNumber,
        args.input
      );
    },
    
    updateScene: async (
      _: any,
      args: { documentId: string; chapterNumber: number; sceneNumber: number; input: any },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(args.documentId);
      await checkProjectAccess(context, document.projectId, 'EDITOR');
      return context.dataSources.documentAPI.updateScene(
        args.documentId,
        args.chapterNumber,
        args.sceneNumber,
        args.input
      );
    },
    
    deleteScene: async (
      _: any,
      args: { documentId: string; chapterNumber: number; sceneNumber: number },
      context: ResolverContext
    ) => {
      requireAuth(context);
      const document = await context.dataSources.documentAPI.getDocument(args.documentId);
      await checkProjectAccess(context, document.projectId, 'EDITOR');
      return context.dataSources.documentAPI.deleteScene(
        args.documentId,
        args.chapterNumber,
        args.sceneNumber
      );
    },
  },
  
  Document: {
    chapters: async (document: any, _: any, context: ResolverContext) => {
      // Chapters are already included in the document response
      return document.chapters || [];
    },
    
    currentWordCount: (document: any) => {
      // Calculate total word count from all chapters
      if (!document.chapters) return 0;
      return document.chapters.reduce((total: number, chapter: any) => {
        return total + (chapter.wordCount || 0);
      }, 0);
    },
  },
  
  Chapter: {
    scenes: async (chapter: any) => {
      // Scenes are already included in the chapter response
      return chapter.scenes || [];
    },
    
    wordCount: (chapter: any) => {
      // Calculate total word count from all scenes
      if (!chapter.scenes) return 0;
      return chapter.scenes.reduce((total: number, scene: any) => {
        return total + (scene.wordCount || 0);
      }, 0);
    },
  },
};