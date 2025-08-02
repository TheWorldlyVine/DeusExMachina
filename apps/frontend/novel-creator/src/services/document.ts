import axios from 'axios'
import type { Document, CreateDocumentInput } from '@/types/document'

const API_URL = import.meta.env.VITE_DOCUMENT_API_URL || 'http://localhost:8080'

class DocumentService {
  private getAuthHeader() {
    const token = localStorage.getItem('auth_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  async getDocuments(): Promise<Document[]> {
    const response = await axios.get(`${API_URL}/documents`, {
      headers: {
        ...this.getAuthHeader(),
        'Content-Type': 'application/json'
      }
    })
    return response.data
  }

  async getDocument(id: string): Promise<Document> {
    const response = await axios.get(`${API_URL}/documents/${id}`, {
      headers: this.getAuthHeader()
    })
    return response.data
  }

  async createDocument(input: CreateDocumentInput): Promise<Document> {
    const response = await axios.post(`${API_URL}/documents`, input, {
      headers: {
        ...this.getAuthHeader(),
        'Content-Type': 'application/json'
      }
    })
    return response.data
  }

  async updateDocument(id: string, data: Partial<Document>): Promise<Document> {
    const response = await axios.put(`${API_URL}/documents/${id}`, data, {
      headers: this.getAuthHeader()
    })
    return response.data
  }

  async deleteDocument(id: string): Promise<void> {
    await axios.delete(`${API_URL}/documents/${id}`, {
      headers: this.getAuthHeader()
    })
  }
}

export const documentService = new DocumentService()