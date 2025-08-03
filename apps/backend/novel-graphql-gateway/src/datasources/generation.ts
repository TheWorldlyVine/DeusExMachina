import { BaseAPI } from './base';

export class GenerationAPI extends BaseAPI {
  baseURL = process.env.AI_SERVICE_URL || 'http://localhost:8083';

  async generateText(input: {
    projectId: string;
    prompt: string;
    context?: string;
    parameters?: any;
  }) {
    try {
      return await this.post('/generate', { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async generateScene(input: {
    projectId: string;
    documentId: string;
    chapterNumber: number;
    sceneNumber: number;
    guidelines?: string;
    parameters?: any;
  }) {
    try {
      // First get the generation context from memory service
      const memoryAPI = new MemoryAPI();
      memoryAPI.initialize({ context: this.context, cache: this.cache });
      
      const context = await memoryAPI.getGenerationContext(
        input.projectId,
        `${input.documentId}_${input.chapterNumber}_${input.sceneNumber}`,
        input.chapterNumber,
        input.sceneNumber
      );

      // Then generate with context
      const generationRequest = {
        prompt: `Generate a scene based on the following context and guidelines:
Context: ${JSON.stringify(context)}
Guidelines: ${input.guidelines || 'Follow the established narrative'}`,
        context: JSON.stringify(context),
        parameters: input.parameters,
        type: 'SCENE',
      };

      return await this.post('/generate', { body: generationRequest });
    } catch (error) {
      this.handleError(error);
    }
  }

  async continueWriting(input: {
    projectId: string;
    documentId: string;
    chapterNumber: number;
    sceneNumber: number;
    continuationLength?: number;
    parameters?: any;
  }) {
    try {
      // Get current scene content
      const documentAPI = new DocumentAPI();
      documentAPI.initialize({ context: this.context, cache: this.cache });
      
      const scene = await documentAPI.getScene(
        input.documentId,
        input.chapterNumber,
        input.sceneNumber
      );

      // Get generation context
      const memoryAPI = new MemoryAPI();
      memoryAPI.initialize({ context: this.context, cache: this.cache });
      
      const context = await memoryAPI.getGenerationContext(
        input.projectId,
        `${input.documentId}_${input.chapterNumber}_${input.sceneNumber}`,
        input.chapterNumber,
        input.sceneNumber
      );

      // Generate continuation
      const generationRequest = {
        prompt: `Continue writing from: "${scene.content.slice(-500)}"
Target length: ${input.continuationLength || 500} words`,
        context: JSON.stringify({ ...context, currentScene: scene }),
        parameters: input.parameters,
        type: 'CONTINUATION',
      };

      return await this.post('/generate', { body: generationRequest });
    } catch (error) {
      this.handleError(error);
    }
  }

  async generateWithStream(input: any) {
    try {
      // For streaming, we'll need to handle this differently
      // This is a placeholder for WebSocket-based streaming
      return await this.post('/generate/stream', { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async countTokens(text: string) {
    try {
      return await this.post('/generate/count-tokens', { body: { text } });
    } catch (error) {
      this.handleError(error);
    }
  }
}

// Import MemoryAPI and DocumentAPI to avoid circular dependencies
import { MemoryAPI } from './memory';
import { DocumentAPI } from './document';