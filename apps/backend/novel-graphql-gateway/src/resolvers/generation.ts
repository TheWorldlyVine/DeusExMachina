import { requireAuth, checkProjectAccess } from '../context';
import { ResolverContext } from '../types/resolver';
import { pubsub } from '../utils/pubsub';

export const generationResolvers = {
  Mutation: {
    generateText: async (_: any, { input }: any, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, input.projectId, 'EDITOR');
      
      const response = await context.dataSources.generationAPI.generateText(input);
      
      // Publish progress updates
      await pubsub.publish(`GENERATION_PROGRESS_${response.requestId}`, {
        generationProgress: {
          requestId: response.requestId,
          progress: 100,
          status: 'COMPLETED',
        },
      });
      
      return response;
    },
    
    generateScene: async (_: any, { input }: any, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, input.projectId, 'EDITOR');
      
      // Generate a request ID for tracking
      const requestId = `scene_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      
      // Start generation asynchronously
      (async () => {
        try {
          // Publish initial progress
          await pubsub.publish(`GENERATION_PROGRESS_${requestId}`, {
            generationProgress: {
              requestId,
              progress: 0,
              status: 'IN_PROGRESS',
            },
          });
          
          const response = await context.dataSources.generationAPI.generateScene(input);
          
          // Update the scene with generated content
          if (response.generatedText) {
            await context.dataSources.documentAPI.updateScene(
              input.documentId,
              input.chapterNumber,
              input.sceneNumber,
              { content: response.generatedText }
            );
            
            // Publish scene update
            await pubsub.publish(`SCENE_UPDATED_${input.documentId}`, {
              sceneUpdated: {
                documentId: input.documentId,
                chapterNumber: input.chapterNumber,
                sceneNumber: input.sceneNumber,
                content: response.generatedText,
              },
            });
          }
          
          // Publish completion
          await pubsub.publish(`GENERATION_PROGRESS_${requestId}`, {
            generationProgress: {
              requestId,
              progress: 100,
              status: 'COMPLETED',
            },
          });
        } catch (error) {
          // Publish error
          await pubsub.publish(`GENERATION_PROGRESS_${requestId}`, {
            generationProgress: {
              requestId,
              progress: 0,
              status: 'FAILED',
            },
          });
        }
      })();
      
      // Return immediately with request ID
      return {
        requestId,
        status: 'IN_PROGRESS',
        generatedText: '',
        wordCount: 0,
        tokensUsed: 0,
        model: 'gemini-pro',
        parameters: input.parameters || {},
      };
    },
    
    continueWriting: async (_: any, { input }: any, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, input.projectId, 'EDITOR');
      
      const response = await context.dataSources.generationAPI.continueWriting(input);
      
      if (response.generatedText) {
        // Get current scene
        const scene = await context.dataSources.documentAPI.getScene(
          input.documentId,
          input.chapterNumber,
          input.sceneNumber
        );
        
        // Append generated text
        const updatedContent = scene.content + ' ' + response.generatedText;
        
        // Update scene
        await context.dataSources.documentAPI.updateScene(
          input.documentId,
          input.chapterNumber,
          input.sceneNumber,
          { content: updatedContent }
        );
        
        // Publish scene update
        await pubsub.publish(`SCENE_UPDATED_${input.documentId}`, {
          sceneUpdated: {
            documentId: input.documentId,
            chapterNumber: input.chapterNumber,
            sceneNumber: input.sceneNumber,
            content: updatedContent,
          },
        });
      }
      
      return response;
    },
  },
};