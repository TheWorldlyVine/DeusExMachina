export interface Memory {
  id: string
  contextId: string
  type: MemoryType
  content: string
  timestamp: Date
  entityId?: string
  entityType?: 'character' | 'location' | 'object'
  relevanceScore: number
  importance: number
  accessCount: number
  lastAccessed?: Date
  tags?: string[]
  metadata?: Record<string, any>
  chapterRef?: string
  sceneRef?: string
  active: boolean
}

export type MemoryType = 
  | 'STATE'
  | 'CONTEXT'
  | 'OBSERVATION'
  | 'REFLECTION'
  | 'EXECUTION'

export interface CreateMemoryInput {
  contextId: string
  type: MemoryType
  content: string
  entityId?: string
  entityType?: 'character' | 'location' | 'object'
  importance?: number
  tags?: string[]
  metadata?: Record<string, any>
  chapterRef?: string
  sceneRef?: string
}

export interface MemorySearchResult {
  memory: Memory
  relevance: number
  highlight?: string
}