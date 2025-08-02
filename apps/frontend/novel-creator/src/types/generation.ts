export interface GenerationRequest {
  prompt: string
  generationType: GenerationType
  contextId: string
  parameters?: GenerationParameters
  metadata?: Record<string, unknown>
}

export type GenerationType = 
  | 'SCENE'
  | 'CHAPTER'
  | 'DIALOGUE'
  | 'DESCRIPTION'
  | 'ACTION'
  | 'TRANSITION'
  | 'OUTLINE'
  | 'CHARACTER_THOUGHT'
  | 'WORLDBUILDING'
  | 'CONTINUATION'

export interface GenerationParameters {
  temperature?: number
  maxTokens?: number
  topK?: number
  topP?: number
  useMemory?: boolean
  modelPreference?: 'speed' | 'balanced' | 'quality'
}

export interface GenerationResponse {
  generationId: string
  contextId: string
  generationType: GenerationType
  generatedText: string
  tokenCount: number
  generationTimeMs: number
  modelUsed: string
  metrics?: GenerationMetrics
  warnings?: string[]
  metadata?: Record<string, unknown>
  timestamp: Date
}

export interface GenerationMetrics {
  promptTokens: number
  completionTokens: number
  totalTokens: number
  estimatedCost?: number
  contextWindowUsed: number
  memoryItemsUsed?: number
  coherenceScore?: number
  relevanceScore?: number
}