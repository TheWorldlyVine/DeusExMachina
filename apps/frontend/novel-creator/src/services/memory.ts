import axios from 'axios'
import type { Memory, CreateMemoryInput } from '@/types/memory'

const API_URL = import.meta.env.VITE_API_URL || '/api'

class MemoryService {
  private getAuthHeader() {
    const token = localStorage.getItem('auth_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  async getMemories(contextId: string): Promise<Memory[]> {
    const response = await axios.get(
      `${API_URL}/memory/context/${contextId}`,
      { headers: this.getAuthHeader() }
    )
    return response.data
  }

  async getCharacterMemories(contextId: string, characterId: string): Promise<Memory[]> {
    const response = await axios.get(
      `${API_URL}/memory/context/${contextId}/character/${characterId}`,
      { headers: this.getAuthHeader() }
    )
    return response.data
  }

  async createMemory(input: CreateMemoryInput): Promise<Memory> {
    const response = await axios.post(
      `${API_URL}/memory`,
      input,
      { headers: this.getAuthHeader() }
    )
    return response.data
  }

  async searchMemories(contextId: string, query: string): Promise<Memory[]> {
    const response = await axios.get(
      `${API_URL}/memory/context/${contextId}/search`,
      { 
        params: { q: query },
        headers: this.getAuthHeader() 
      }
    )
    return response.data
  }

  async getRelevantMemories(contextId: string, prompt: string): Promise<Memory[]> {
    const response = await axios.post(
      `${API_URL}/memory/context/${contextId}/relevant`,
      { prompt },
      { headers: this.getAuthHeader() }
    )
    return response.data
  }
}

export const memoryService = new MemoryService()