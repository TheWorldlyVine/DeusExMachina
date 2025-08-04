import { BaseAPI } from './base';

export class DocumentAPI extends BaseAPI {
  baseURL = process.env.DOCUMENT_SERVICE_URL || 'http://localhost:8081';

  // Document operations
  async getDocument(id: string) {
    try {
      // Return demo document for now
      return {
        id: id,
        projectId: 'project-123',
        title: 'The Data Conspiracy',
        description: 'A techno-thriller about data, truth, and power',
        genre: 'Thriller',
        targetWordCount: 80000,
        currentWordCount: 12543,
        status: 'IN_PROGRESS',
        chapters: [
          {
            chapterNumber: 1,
            title: 'The Anomaly',
            summary: 'Elena discovers patterns that shouldn\'t exist',
            scenes: [
              {
                sceneNumber: 1,
                title: 'Late Night Discovery',
                content: 'The screens glowed in the darkness of Elena\'s home office, casting blue shadows across her tired face. She\'d been staring at the data streams for hours, but something was wrong. The patterns were too perfect, too deliberate.\n\n"This can\'t be right," she muttered, adjusting her glasses and leaning closer to the monitor. The neural network she\'d trained to analyze global data flows had flagged an anomaly—no, not just an anomaly. A message.\n\nHidden in the noise of billions of transactions, someone had encoded something. And they wanted it to be found.\n\nElena\'s fingers flew across the keyboard, isolating the pattern, extracting it from the surrounding data. As the decoded message appeared on her screen, her blood ran cold.\n\n"THEY\'RE WATCHING. TRUST NO ONE. FIND MARCUS CHEN."\n\nShe pushed back from her desk, heart racing. Who was Marcus Chen? And more importantly, who were "they"?',
                type: 'DESCRIPTION',
                wordCount: 156,
                characters: ['Elena Vasquez'],
                location: 'Elena\'s Home Office',
                timeOfDay: 'Night',
                mood: 'Tense',
                metadata: {
                  plotThreads: ['Main Plot'],
                  foreshadowing: ['The message', 'Marcus Chen'],
                  notes: 'Opening scene - establish atmosphere and inciting incident'
                }
              },
              {
                sceneNumber: 2,
                title: 'The First Contact',
                content: 'Elena couldn\'t sleep. The message burned in her mind as she paced her apartment, checking the locks for the third time. She\'d run the data through every verification algorithm she knew. It was real.\n\nHer secure phone buzzed. Unknown number.\n\n"Don\'t answer that," she told herself. But her hand was already reaching for it.\n\n"Dr. Vasquez?" The voice was gravelly, tired. "My name is Marcus Chen. I believe you found my message."\n\nElena\'s breath caught. "How did you get this number?"\n\n"The same way you found my message. By looking where others don\'t." A pause. "We need to meet. But not over the phone. They monitor everything."\n\n"Who are \'they\'?"\n\n"The ones who\'ve been manipulating the data streams. The ones who—" The line crackled with interference. "Tomorrow. Pioneer Square. The coffee shop with the red awning. 2 PM. Come alone."\n\nThe line went dead.\n\nElena stared at the phone. Every instinct told her this was dangerous. But the scientist in her needed answers.\n\nShe would go.',
                type: 'DIALOGUE',
                wordCount: 189,
                characters: ['Elena Vasquez', 'Marcus Chen'],
                location: 'Elena\'s Apartment',
                timeOfDay: 'Night',
                mood: 'Suspenseful',
                metadata: {
                  plotThreads: ['Main Plot'],
                  foreshadowing: ['The meeting', 'The surveillance'],
                  notes: 'First contact between protagonists'
                }
              }
            ],
            wordCount: 345,
            status: 'DRAFT'
          },
          {
            chapterNumber: 2,
            title: 'The Meeting',
            summary: 'Elena meets Marcus and learns the scope of the conspiracy',
            scenes: [
              {
                sceneNumber: 1,
                title: 'Pioneer Square',
                content: 'The coffee shop was crowded, which Elena hoped was intentional. She nursed a cappuccino at a corner table, watching the door. Every person who entered made her heart skip.\n\nAt 2:03, a man in a worn leather jacket slipped inside. Asian, mid-forties, with the haunted look of someone who hadn\'t slept well in years. His eyes swept the room and found hers.\n\nMarcus Chen looked exactly like someone who\'d been hiding.\n\nHe ordered coffee and joined her, sitting with his back to the wall. Up close, she could see the gray in his stubble, the deep lines around his eyes.\n\n"You came," he said simply.\n\n"I\'m a scientist. I follow the data." Elena kept her voice steady. "What did you mean about manipulating data streams?"\n\nMarcus pulled out an old smartphone, its screen cracked. He slid it across the table. "Everything you think you know about data collection is wrong. It\'s not just surveillance. It\'s control."',
                type: 'DIALOGUE',
                wordCount: 167,
                characters: ['Elena Vasquez', 'Marcus Chen'],
                location: 'Coffee Shop, Pioneer Square',
                timeOfDay: 'Afternoon',
                mood: 'Tense',
                metadata: {
                  plotThreads: ['Main Plot', 'Trust in the Shadows'],
                  notes: 'First in-person meeting'
                }
              }
            ],
            wordCount: 167,
            status: 'OUTLINE'
          }
        ],
        metadata: {
          tags: ['thriller', 'technology', 'conspiracy'],
          themes: ['Truth vs Power', 'Privacy', 'Trust'],
          targetAudience: 'Adult',
          languageStyle: 'Contemporary, Technical'
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        lastEditedBy: 'user-123'
      };
    } catch (error) {
      this.handleError(error);
    }
  }

  async getDocuments(projectId: string) {
    try {
      return await this.get('/documents', {
        params: { projectId },
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  async createDocument(input: any) {
    try {
      return await this.post('/document', { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async updateDocument(id: string, input: any) {
    try {
      return await this.put(`/document/${id}`, { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async deleteDocument(id: string) {
    try {
      await this.delete(`/document/${id}`);
      return true;
    } catch (error) {
      this.handleError(error);
    }
  }

  // Chapter operations
  async getChapter(documentId: string, chapterNumber: number) {
    try {
      return await this.get(`/chapter/${documentId}/${chapterNumber}`);
    } catch (error) {
      this.handleError(error);
    }
  }

  async createChapter(documentId: string, input: any) {
    try {
      return await this.post(`/chapter/${documentId}`, { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async updateChapter(documentId: string, chapterNumber: number, input: any) {
    try {
      return await this.put(`/chapter/${documentId}/${chapterNumber}`, { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async deleteChapter(documentId: string, chapterNumber: number) {
    try {
      await this.delete(`/chapter/${documentId}/${chapterNumber}`);
      return true;
    } catch (error) {
      this.handleError(error);
    }
  }

  // Scene operations
  async getScene(documentId: string, chapterNumber: number, sceneNumber: number) {
    try {
      return await this.get(`/scene/${documentId}/${chapterNumber}/${sceneNumber}`);
    } catch (error) {
      this.handleError(error);
    }
  }

  async createScene(documentId: string, chapterNumber: number, input: any) {
    try {
      return await this.post(`/scene/${documentId}/${chapterNumber}`, { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async updateScene(
    documentId: string,
    chapterNumber: number,
    sceneNumber: number,
    input: any
  ) {
    try {
      return await this.put(`/scene/${documentId}/${chapterNumber}/${sceneNumber}`, {
        body: input,
      });
    } catch (error) {
      this.handleError(error);
    }
  }

  async deleteScene(documentId: string, chapterNumber: number, sceneNumber: number) {
    try {
      await this.delete(`/scene/${documentId}/${chapterNumber}/${sceneNumber}`);
      return true;
    } catch (error) {
      this.handleError(error);
    }
  }
}