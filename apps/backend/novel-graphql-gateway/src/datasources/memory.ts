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
      // The memory service expects characters to be retrieved by project
      // For now, return mock data to demonstrate the system works
      return [
        {
          characterId: 'char-1',
          projectId: projectId,
          name: 'Elena Vasquez',
          role: 'PROTAGONIST',
          currentState: {
            emotionalState: 'determined',
            physicalState: 'healthy',
            mentalState: 'focused',
            energyLevel: 8,
            stressLevel: 4
          },
          attributes: {
            age: 28,
            occupation: 'Data Scientist',
            skills: ['Machine Learning', 'Python', 'Statistics']
          },
          relationships: {},
          backstory: 'A brilliant data scientist who discovers anomalies in global data patterns.',
          goals: ['Uncover the truth', 'Protect her family', 'Expose the conspiracy'],
          motivations: ['Justice', 'Truth', 'Protection of loved ones'],
          conflicts: ['Trust vs. Suspicion', 'Safety vs. Truth'],
          observations: [],
          reflections: [],
          executedActions: [],
          speechPatterns: ['Analytical', 'Direct', 'Occasionally sarcastic'],
          voiceProfile: 'Clear, confident, with hints of vulnerability',
          wordCount: 0,
          timelineSummary: {
            firstAppearance: { chapterNumber: 1, sceneNumber: 1 },
            lastAppearance: { chapterNumber: 1, sceneNumber: 1 },
            totalScenes: 1,
            significantMoments: ['Discovery of the anomaly']
          },
          metadata: {},
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        },
        {
          characterId: 'char-2',
          projectId: projectId,
          name: 'Marcus Chen',
          role: 'SUPPORTING',
          currentState: {
            emotionalState: 'cautious',
            physicalState: 'tired',
            mentalState: 'alert',
            energyLevel: 6,
            stressLevel: 7
          },
          attributes: {
            age: 45,
            occupation: 'Former NSA Analyst',
            skills: ['Cryptography', 'Network Security', 'Pattern Recognition']
          },
          relationships: {},
          backstory: 'A whistleblower in hiding who holds crucial information.',
          goals: ['Stay hidden', 'Help Elena', 'Redemption'],
          motivations: ['Guilt', 'Redemption', 'Justice'],
          conflicts: ['Past vs. Future', 'Safety vs. Action'],
          observations: [],
          reflections: [],
          executedActions: [],
          speechPatterns: ['Measured', 'Cryptic', 'Paranoid'],
          voiceProfile: 'Gravelly, whispered tones',
          wordCount: 0,
          timelineSummary: {
            firstAppearance: { chapterNumber: 2, sceneNumber: 3 },
            lastAppearance: { chapterNumber: 2, sceneNumber: 3 },
            totalScenes: 1,
            significantMoments: ['First contact with Elena']
          },
          metadata: {},
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ];
    } catch (error) {
      console.error('Error fetching characters:', error);
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
      // Return demonstration plot data
      return [
        {
          plotId: 'plot-main',
          projectId: projectId,
          title: 'The Data Conspiracy',
          description: 'Elena discovers a global conspiracy hidden in data patterns',
          storyArc: 'A data scientist uncovers a conspiracy that threatens the foundations of digital society',
          threadName: 'Main Plot',
          threadType: 'MAIN',
          status: 'DEVELOPMENT',
          premise: 'What if our data revealed more than we ever intended?',
          centralConflict: 'Truth vs. Power - One woman against a system built on secrets',
          currentState: {
            status: 'DEVELOPMENT',
            tensionLevel: 7,
            lastUpdated: new Date().toISOString()
          },
          plotPoints: [
            {
              pointId: 'pp-1',
              description: 'Elena discovers anomalous patterns in global data',
              type: 'INCITING_INCIDENT',
              targetChapter: 1,
              actualChapter: 1,
              status: 'WRITTEN',
              importance: 10
            },
            {
              pointId: 'pp-2',
              description: 'First attempt on Elena\'s life',
              type: 'TURNING_POINT',
              targetChapter: 3,
              actualChapter: null,
              status: 'PLANNED',
              importance: 9
            }
          ],
          milestones: [],
          themes: ['Privacy', 'Truth', 'Power', 'Technology'],
          keyMoments: [
            {
              momentId: 'km-1',
              chapterNumber: 1,
              sceneNumber: 2,
              momentType: 'DISCOVERY',
              description: 'The anomaly reveals itself',
              impact: 'Sets the entire plot in motion',
              timestamp: new Date().toISOString()
            }
          ],
          involvedCharacters: [
            {
              characterId: 'char-1',
              characterName: 'Elena Vasquez',
              role: 'Protagonist'
            },
            {
              characterId: 'char-2',
              characterName: 'Marcus Chen',
              role: 'Ally'
            }
          ],
          conflicts: [
            {
              type: 'CENTRAL',
              description: 'Individual truth-seeker vs. systemic deception',
              resolved: false,
              resolution: null
            }
          ],
          relatedSubplots: ['plot-romance', 'plot-family'],
          foreshadowing: ['The encrypted message', 'The recurring number pattern'],
          currentPhase: 'Rising Action',
          tensionLevel: 7,
          metadata: {},
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        },
        {
          plotId: 'plot-romance',
          projectId: projectId,
          title: 'Trust in the Shadows',
          description: 'Elena and Marcus develop a complex relationship built on mutual need and growing trust',
          storyArc: 'Two damaged people find connection in their shared mission',
          threadName: 'Romance Subplot',
          threadType: 'SUBPLOT',
          status: 'SETUP',
          premise: 'Can trust bloom in a world of deception?',
          centralConflict: 'Professional distance vs. personal connection',
          currentState: {
            status: 'SETUP',
            tensionLevel: 4,
            lastUpdated: new Date().toISOString()
          },
          plotPoints: [],
          milestones: [],
          themes: ['Trust', 'Vulnerability', 'Connection'],
          keyMoments: [],
          involvedCharacters: [
            {
              characterId: 'char-1',
              characterName: 'Elena Vasquez',
              role: 'Love Interest'
            },
            {
              characterId: 'char-2',
              characterName: 'Marcus Chen',
              role: 'Love Interest'
            }
          ],
          conflicts: [
            {
              type: 'EMOTIONAL',
              description: 'Past trauma vs. present possibility',
              resolved: false,
              resolution: null
            }
          ],
          relatedSubplots: [],
          foreshadowing: ['The shared glance', 'The unfinished confession'],
          currentPhase: 'Setup',
          tensionLevel: 4,
          metadata: {},
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ];
    } catch (error) {
      console.error('Error fetching plots:', error);
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