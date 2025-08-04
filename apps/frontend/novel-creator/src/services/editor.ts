import axios from 'axios'
import type { Chapter, Scene, EditorState } from '@/types/editor'

const API_URL = import.meta.env.VITE_DOCUMENT_API_URL || 'http://localhost:8080'

class EditorService {
  private getAuthHeader() {
    const token = localStorage.getItem('auth_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  async loadDocument(documentId: string): Promise<EditorState> {
    const response = await axios.get(`${API_URL}/documents/${documentId}/editor`, {
      headers: this.getAuthHeader()
    })
    return response.data
  }

  async saveContent(documentId: string, content: string): Promise<void> {
    await axios.put(`${API_URL}/documents/${documentId}/content`, 
      { content }, 
      { headers: this.getAuthHeader() }
    )
  }

  async createChapter(documentId: string, title: string): Promise<Chapter> {
    const response = await axios.post(
      `${API_URL}/documents/${documentId}/chapters`,
      { title },
      { headers: this.getAuthHeader() }
    )
    return response.data
  }

  async createScene(chapterId: string, title: string): Promise<Scene> {
    const response = await axios.post(
      `${API_URL}/chapters/${chapterId}/scenes`,
      { title },
      { headers: this.getAuthHeader() }
    )
    return response.data
  }
}

export const editorService = new EditorService()