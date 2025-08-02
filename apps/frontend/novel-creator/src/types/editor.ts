export interface EditorState {
  documentId: string
  content: string
  chapters: Chapter[]
  metadata: DocumentMetadata
}

export interface Chapter {
  id: string
  documentId: string
  title: string
  chapterNumber: number
  summary?: string
  createdAt: Date
  updatedAt: Date
  wordCount: number
  sceneCount: number
  scenes: Scene[]
  notes?: string
}

export interface Scene {
  id: string
  chapterId: string
  documentId: string
  content: string
  sceneNumber: number
  title?: string
  summary?: string
  type: SceneType
  createdAt: Date
  updatedAt: Date
  wordCount: number
  characterIds?: string[]
  locationId?: string
  metadata?: Record<string, any>
  notes?: string
}

export type SceneType = 
  | 'NARRATIVE' 
  | 'DIALOGUE' 
  | 'ACTION' 
  | 'DESCRIPTION' 
  | 'FLASHBACK' 
  | 'DREAM' 
  | 'MONTAGE'

export interface DocumentMetadata {
  lastSaved: Date
  version: string
  collaborators?: string[]
  locked?: boolean
  lockOwner?: string
}