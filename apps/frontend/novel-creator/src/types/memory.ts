// Legacy Memory Types (for backward compatibility)
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
  metadata?: Record<string, unknown>
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
  metadata?: Record<string, unknown>
  chapterRef?: string
  sceneRef?: string
}

export interface MemorySearchResult {
  memory: Memory
  relevance: number
  highlight?: string
}

// Character Memory Types (following SCORE pattern)
export interface CharacterMemory {
  characterId: string
  projectId: string
  name: string
  role: 'protagonist' | 'antagonist' | 'supporting' | 'minor'
  currentState: CharacterState
  observations: CharacterObservation[]
  reflections: CharacterReflection[]
  executedActions: CharacterAction[]
  relationships: CharacterRelationship[]
  timelineSummary?: TimelineSummary
  metadata: Record<string, unknown>
}

export interface CharacterState {
  description: string
  emotionalState: string
  goals: string[]
  location: string
  lastUpdated: string
}

export interface CharacterObservation {
  observationId: string
  chapterNumber: number
  sceneNumber: number
  observation: string
  observationType: 'dialogue' | 'action' | 'thought' | 'environment'
  timestamp: string
}

export interface CharacterReflection {
  reflectionId: string
  reflection: string
  emotionalImpact?: string
  decisionsInfluenced: string[]
  timestamp: string
}

export interface CharacterAction {
  actionId: string
  chapterNumber: number
  sceneNumber: number
  action: string
  motivation: string
  result?: string
  timestamp: string
}

export interface CharacterRelationship {
  targetCharacterId: string
  targetCharacterName?: string
  relationshipType: string
  description?: string
  dynamics: RelationshipDynamic[]
}

export interface RelationshipDynamic {
  chapterNumber: number
  status: string
  change?: string
}

// Plot Memory Types
export interface PlotMemory {
  plotId: string
  projectId: string
  title: string
  description?: string
  storyArc: string
  currentState: PlotState
  keyMoments: PlotMoment[]
  involvedCharacters: PlotCharacter[]
  conflicts: PlotConflict[]
  relatedSubplots: string[]
  foreshadowing: string[]
  metadata: Record<string, unknown>
}

export interface PlotState {
  status: 'PLANNED' | 'ACTIVE' | 'RESOLVED' | 'ABANDONED'
  tensionLevel?: number // 0-10
  lastUpdated: string
}

export interface PlotMoment {
  momentId: string
  chapterNumber: number
  sceneNumber: number
  momentType: 'inciting' | 'rising' | 'climax' | 'falling' | 'resolution'
  description: string
  impact?: string
  timestamp: string
}

export interface PlotCharacter {
  characterId: string
  characterName: string
  role: string // role in this specific plot
}

export interface PlotConflict {
  type: 'internal' | 'external' | 'interpersonal' | 'societal'
  description: string
  resolved: boolean
  resolution?: string
}

// World Memory Types
export interface WorldMemory {
  locationId: string
  projectId: string
  name: string
  type: 'setting' | 'landmark' | 'region' | 'building'
  description: string
  currentState: WorldState
  history: WorldEvent[]
  connectedLocations: string[]
  significance: string[]
  metadata: Record<string, unknown>
}

export interface WorldState {
  atmosphere: string
  timeOfDay?: string
  weather?: string
  inhabitants: string[]
  lastUpdated: string
}

export interface WorldEvent {
  eventId: string
  chapterNumber: number
  event: string
  impact: string
  charactersPresent: string[]
  timestamp: string
}

// Shared Types
export interface TimelineSummary {
  firstAppearance?: { chapterNumber: number; sceneNumber: number }
  lastAppearance?: { chapterNumber: number; sceneNumber: number }
  totalScenes: number
  significantMoments: number
}

// Generation Context Type (for AI integration)
export interface GenerationContext {
  projectId: string
  chapterNumber: number
  sceneNumber: number
  characters: CharacterMemory[]
  activePlots: PlotMemory[]
  currentLocation?: WorldMemory
  recentEvents: string[]
  styleGuidelines: Record<string, unknown>
}