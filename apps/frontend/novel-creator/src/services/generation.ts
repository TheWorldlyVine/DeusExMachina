import axios from 'axios'
import type { GenerationRequest, GenerationResponse } from '@/types/generation'

const API_URL = import.meta.env.VITE_API_URL || '/api'

class GenerationService {
  private getAuthHeader() {
    const token = localStorage.getItem('auth_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  async generateText(request: GenerationRequest): Promise<GenerationResponse> {
    const response = await axios.post(
      `${API_URL}/generate`,
      request,
      { headers: this.getAuthHeader() }
    )
    return response.data
  }

  async getGenerationHistory(contextId: string): Promise<GenerationResponse[]> {
    const response = await axios.get(
      `${API_URL}/generate/history/${contextId}`,
      { headers: this.getAuthHeader() }
    )
    return response.data
  }

  async cancelGeneration(generationId: string): Promise<void> {
    await axios.post(
      `${API_URL}/generate/${generationId}/cancel`,
      {},
      { headers: this.getAuthHeader() }
    )
  }
}

export const generationService = new GenerationService()