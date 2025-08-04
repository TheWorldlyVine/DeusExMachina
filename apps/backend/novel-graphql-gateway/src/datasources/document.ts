import { BaseAPI } from './base';

export class DocumentAPI extends BaseAPI {
  baseURL = process.env.DOCUMENT_SERVICE_URL || 'http://localhost:8080';

  // Document operations
  async getDocument(id: string) {
    try {
      // Use the real document service API
      return await this.get(`/document/${id}`);
    } catch (error) {
      this.handleError(error);
    }
  }

  async getDocuments(projectId: string) {
    try {
      // For now, return the documents from the API without projectId filter
      // The Java backend currently returns all documents for the authenticated user
      return await this.get('/documents');
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
      const chapterNumber = input.chapterNumber || (await this.getNextChapterNumber(documentId));
      return await this.post(`/chapter/${documentId}/${chapterNumber}`, { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  private async getNextChapterNumber(documentId: string): Promise<number> {
    try {
      const document = await this.get(`/document/${documentId}`);
      return (document.chapters?.length || 0) + 1;
    } catch (error) {
      return 1;
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
      const sceneNumber = input.sceneNumber || (await this.getNextSceneNumber(documentId, chapterNumber));
      return await this.post(`/scene/${documentId}/${chapterNumber}/${sceneNumber}`, { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  private async getNextSceneNumber(documentId: string, chapterNumber: number): Promise<number> {
    try {
      const chapter = await this.get(`/chapter/${documentId}/${chapterNumber}`);
      return (chapter.scenes?.length || 0) + 1;
    } catch (error) {
      return 1;
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