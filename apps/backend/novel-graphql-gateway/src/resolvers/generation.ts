import { requireAuth, checkProjectAccess } from '../context';
import { ResolverContext } from '../types/resolver';
import { pubsub } from '../utils/pubsub';

export const generationResolvers = {
  Mutation: {
    generateText: async (_: any, { input }: any, context: ResolverContext) => {
      requireAuth(context);
      await checkProjectAccess(context, input.projectId, 'EDITOR');
      
      const response = await context.dataSources.generationAPI.generateText(input);
      
      // Update memory with generation metadata
      if (response.generatedText) {
        try {
          // Add a world fact about the generation
          await context.dataSources.memoryAPI.addWorldFact(
            input.projectId,
            'generation_history',
            {
              type: 'TEXT_GENERATION',
              prompt: input.prompt.substring(0, 100) + '...',
              wordCount: response.wordCount,
              model: response.model,
              timestamp: new Date().toISOString(),
              metadata: {
                tokensUsed: response.tokensUsed,
                parameters: input.parameters
              }
            }
          );
        } catch (memoryError) {
          console.error('Failed to update memory:', memoryError);
        }
      }
      
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
            
            // Update memory with the new scene content
            try {
              // Add plot milestone for scene completion
              await context.dataSources.memoryAPI.addPlotMilestone(
                input.projectId,
                'plot-main',
                {
                  description: `Generated scene ${input.sceneNumber} of chapter ${input.chapterNumber}`,
                  type: 'SCENE_GENERATED',
                  timestamp: new Date().toISOString(),
                  metadata: {
                    documentId: input.documentId,
                    chapterNumber: input.chapterNumber,
                    sceneNumber: input.sceneNumber,
                    wordCount: response.wordCount
                  }
                }
              );
              
              // Update character states if characters are involved
              // This would be based on analyzing the generated content
              // For now, we'll add a simple observation
              const primaryCharacter = 'char-1'; // Elena - would be determined from context
              await context.dataSources.memoryAPI.addCharacterObservation(
                input.projectId,
                primaryCharacter,
                {
                  type: 'SCENE_PARTICIPATION',
                  description: `Appeared in Chapter ${input.chapterNumber}, Scene ${input.sceneNumber}`,
                  timestamp: new Date().toISOString(),
                  location: { chapterNumber: input.chapterNumber, sceneNumber: input.sceneNumber },
                  metadata: { generatedContent: true }
                }
              );
            } catch (memoryError) {
              // Log memory update errors but don't fail the generation
              console.error('Failed to update memory:', memoryError);
            }
            
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
        
        // Update memory with continuation
        try {
          // Update plot tension if this is a significant continuation
          if (response.wordCount > 200) {
            await context.dataSources.memoryAPI.updatePlotTension(
              input.projectId,
              'plot-main',
              input.chapterNumber,
              7 // Default tension level, would be analyzed from content
            );
          }
          
          // Add character observation for continuation
          const primaryCharacter = 'char-1'; // Would be determined from context
          await context.dataSources.memoryAPI.addCharacterObservation(
            input.projectId,
            primaryCharacter,
            {
              type: 'SCENE_CONTINUATION',
              description: `Scene continued with ${response.wordCount} additional words`,
              timestamp: new Date().toISOString(),
              location: { chapterNumber: input.chapterNumber, sceneNumber: input.sceneNumber },
              metadata: { 
                continuationLength: response.wordCount,
                totalSceneLength: updatedContent.split(/\s+/).length 
              }
            }
          );
        } catch (memoryError) {
          console.error('Failed to update memory:', memoryError);
        }
        
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