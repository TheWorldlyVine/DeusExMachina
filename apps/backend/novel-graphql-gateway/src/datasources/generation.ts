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
    context?: any; // Context should be passed from resolver
  }) {
    try {
      // Generate with provided context
      const generationRequest = {
        prompt: `Generate a scene based on the following context and guidelines:
Context: ${JSON.stringify(input.context || {})}
Guidelines: ${input.guidelines || 'Follow the established narrative'}`,
        context: JSON.stringify(input.context || {}),
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
    currentContent?: string; // Current content should be passed from resolver
    context?: any; // Context should be passed from resolver
  }) {
    try {
      // Generate continuation
      const generationRequest = {
        prompt: `Continue writing from: "${input.currentContent?.slice(-500) || ''}"
Target length: ${input.continuationLength || 500} words`,
        context: JSON.stringify(input.context || {}),
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

