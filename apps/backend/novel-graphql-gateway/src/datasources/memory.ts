import { BaseAPI } from './base';

export class MemoryAPI extends BaseAPI {
  baseURL = process.env.MEMORY_SERVICE_URL || 'http://localhost:8082';

  // Character Memory
  async createCharacter(input: any) {
    try {
      return await this.post('/memory/characters', { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async getCharacter(projectId: string, characterId: string) {
    try {
      const response = await this.get(`/memory/characters/${characterId}`);
      return response;
    } catch (error) {
      this.handleError(error);
    }
  }

  async getCharacters(projectId: string) {
    try {
      // Since the memory service uses path-based project context,
      // we need to ensure projectId is passed in headers
      const context = this.context;
      const originalProjectId = context.projectId;
      context.projectId = projectId;
      
      const response = await this.get('/memory/characters');
      
      // Restore original context
      context.projectId = originalProjectId;
      
      return response;
    } catch (error) {
      // Return empty array for now until memory service is ready
      console.warn('Memory service not available, returning empty characters');
      return [];
    }
  }

  async updateCharacterState(projectId: string, characterId: string, state: any) {
    try {
      return await this.put(`/memory/characters/${characterId}/state`, { body: state });
    } catch (error) {
      this.handleError(error);
    }
  }

  async addCharacterObservation(projectId: string, characterId: string, observation: any) {
    try {
      return await this.post(`/memory/characters/${characterId}/observations`, {
        body: observation,
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  async getCharacterTimeline(projectId: string, characterId: string, limit?: number) {
    try {
      return await this.get(`/memory/characters/${characterId}/timeline`, {
        params: limit ? { limit: String(limit) } : undefined,
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  // Plot Memory
  async createPlot(input: any) {
    try {
      return await this.post('/memory/plot', { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async getPlot(projectId: string, plotId: string) {
    try {
      return await this.get(`/memory/plot/${projectId}/${plotId}`);
    } catch (error) {
      this.handleError(error);
    }
  }

  async getPlots(projectId: string) {
    try {
      return await this.get(`/memory/plot/${projectId}`);
    } catch (error) {
      // Return empty array for now until memory service is ready
      console.warn('Memory service not available, returning empty plots');
      return [];
    }
  }

  async addPlotPoint(projectId: string, plotId: string, plotPoint: any) {
    try {
      return await this.post(`/memory/plot/${projectId}/threads/${plotId}/points`, {
        body: plotPoint,
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  async addPlotMilestone(projectId: string, plotId: string, milestone: any) {
    try {
      return await this.post(`/memory/plot/${projectId}/milestones`, {
        body: { plotId, milestone },
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  async updatePlotTension(
    projectId: string,
    plotId: string,
    chapterNumber: number,
    tensionLevel: number
  ) {
    try {
      return await this.put(`/memory/plot/${projectId}/threads/${plotId}/tension`, {
        body: { chapterNumber, tensionLevel },
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  // World Memory
  async getWorldMemory(projectId: string) {
    try {
      return await this.get(`/memory/world/${projectId}`);
    } catch (error) {
      this.handleError(error);
    }
  }

  async addWorldFact(projectId: string, category: string, fact: any) {
    try {
      return await this.post(`/memory/world/${projectId}/facts`, {
        body: { category, fact },
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  async addLocation(projectId: string, location: any) {
    try {
      return await this.post(`/memory/world/${projectId}/locations`, {
        body: location,
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  async getLocation(projectId: string, locationId: string) {
    try {
      return await this.get(`/memory/world/${projectId}/locations/${locationId}`);
    } catch (error) {
      this.handleError(error);
    }
  }

  async validateWorldConsistency(projectId: string) {
    try {
      return await this.post(`/memory/world/${projectId}/validate`);
    } catch (error) {
      this.handleError(error);
    }
  }

  // Context
  async getGenerationContext(
    projectId: string,
    sceneId: string,
    chapterNumber: number,
    sceneNumber: number
  ) {
    try {
      return await this.get(`/memory/context/${projectId}/${sceneId}`, {
        params: { chapter: String(chapterNumber), scene: String(sceneNumber) },
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  // Search
  async searchMemory(projectId: string, query: string, type?: string) {
    try {
      return await this.post('/memory/search', {
        body: { projectId, query, type },
      });
    } catch (error) {
      this.handleError(error);
    }
  }
}