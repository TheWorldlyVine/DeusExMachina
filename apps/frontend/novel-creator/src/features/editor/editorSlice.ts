import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { editorService } from '@/services/editor'
import type { Chapter, Scene, EditorState as EditorContent } from '@/types/editor'

interface EditorState {
  content: EditorContent | null
  chapters: Chapter[]
  currentChapter: Chapter | null
  currentScene: Scene | null
  isLoading: boolean
  isSaving: boolean
  lastSaved: Date | null
  isDirty: boolean
  error: string | null
}

const initialState: EditorState = {
  content: null,
  chapters: [],
  currentChapter: null,
  currentScene: null,
  isLoading: false,
  isSaving: false,
  lastSaved: null,
  isDirty: false,
  error: null,
}

export const loadDocument = createAsyncThunk(
  'editor/loadDocument',
  async (documentId: string) => {
    const content = await editorService.loadDocument(documentId)
    return content
  }
)

export const saveContent = createAsyncThunk(
  'editor/saveContent',
  async ({ documentId, content }: { documentId: string; content: string }) => {
    console.log('[EditorSlice] Saving content for document:', documentId, 'Content length:', content.length)
    try {
      await editorService.saveContent(documentId, content)
      console.log('[EditorSlice] Save completed')
      return new Date()
    } catch (error) {
      console.error('[EditorSlice] Save failed with error:', error)
      throw error
    }
  }
)

export const createChapter = createAsyncThunk(
  'editor/createChapter',
  async ({ documentId, title }: { documentId: string; title: string }) => {
    return await editorService.createChapter(documentId, title)
  }
)

export const createScene = createAsyncThunk(
  'editor/createScene',
  async ({ chapterId, title }: { chapterId: string; title: string }) => {
    return await editorService.createScene(chapterId, title)
  }
)

const editorSlice = createSlice({
  name: 'editor',
  initialState,
  reducers: {
    updateContent: (state, action: PayloadAction<string>) => {
      if (state.content) {
        state.content.content = action.payload
        state.isDirty = true
      }
    },
    setCurrentChapter: (state, action: PayloadAction<Chapter>) => {
      state.currentChapter = action.payload
    },
    setCurrentScene: (state, action: PayloadAction<Scene>) => {
      state.currentScene = action.payload
    },
    markClean: (state) => {
      state.isDirty = false
    },
    clearEditor: () => {
      return initialState
    },
  },
  extraReducers: (builder) => {
    builder
      // Load Document
      .addCase(loadDocument.pending, (state) => {
        state.isLoading = true
      })
      .addCase(loadDocument.fulfilled, (state, action) => {
        state.isLoading = false
        state.content = action.payload
        state.chapters = action.payload.chapters
        state.isDirty = false
        
        // Auto-select first chapter and scene if available
        if (action.payload.chapters && action.payload.chapters.length > 0) {
          state.currentChapter = action.payload.chapters[0]
          if (state.currentChapter.scenes && state.currentChapter.scenes.length > 0) {
            state.currentScene = state.currentChapter.scenes[0]
          }
        }
      })
      .addCase(loadDocument.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.error.message || 'Failed to load document'
      })
      // Save Content
      .addCase(saveContent.pending, (state) => {
        state.isSaving = true
      })
      .addCase(saveContent.fulfilled, (state, action) => {
        state.isSaving = false
        state.lastSaved = action.payload
        state.isDirty = false
      })
      .addCase(saveContent.rejected, (state, action) => {
        state.isSaving = false
        state.error = action.error.message || 'Failed to save content'
      })
      // Create Chapter
      .addCase(createChapter.fulfilled, (state, action) => {
        state.chapters.push(action.payload)
      })
      // Create Scene
      .addCase(createScene.fulfilled, (state, action) => {
        if (state.currentChapter) {
          state.currentChapter.scenes.push(action.payload)
        }
      })
  },
})

export const { 
  updateContent, 
  setCurrentChapter, 
  setCurrentScene, 
  markClean, 
  clearEditor 
} = editorSlice.actions

export default editorSlice.reducer