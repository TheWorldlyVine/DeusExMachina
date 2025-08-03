export interface Document {
  id: string
  contextId: string
  title: string
  subtitle?: string
  authorId: string
  authorName: string
  status: DocumentStatus
  createdAt: Date
  updatedAt: Date
  description?: string
  genre?: string
  tags?: string[]
  wordCount: number
  chapterCount: number
  sceneCount: number
  currentVersion?: string
  settings?: DocumentSettings
  metadata?: Record<string, unknown>
}

export type DocumentStatus = 'DRAFT' | 'IN_REVIEW' | 'PUBLISHED' | 'ARCHIVED'

export interface DocumentSettings {
  targetWordCount?: number
  language?: string
  style?: string
  autoSave?: boolean
  autoSaveIntervalSeconds?: number
  customSettings?: Record<string, unknown>
}

export interface Chapter {
  chapterNumber: number
  title: string
  summary?: string
  scenes: Scene[]
  wordCount: number
  status: ChapterStatus
}

export interface Scene {
  sceneNumber: number
  title?: string
  content: string
  type: SceneType
  wordCount: number
  characters?: string[]
  location?: string
  timeOfDay?: string
  mood?: string
  metadata?: Record<string, unknown>
}

export type ChapterStatus = 'OUTLINE' | 'DRAFT' | 'REVISION' | 'FINAL'
export type SceneType = 'ACTION' | 'DIALOGUE' | 'DESCRIPTION' | 'FLASHBACK' | 'TRANSITION' | 'CLIMAX'

export interface CreateDocumentInput {
  title: string
  subtitle?: string
  description?: string
  genre?: string
  tags?: string[]
  settings?: DocumentSettings
}