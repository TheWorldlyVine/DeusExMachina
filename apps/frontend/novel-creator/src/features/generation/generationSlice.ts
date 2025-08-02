import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { generationService } from '@/services/generation'
import type { GenerationRequest, GenerationResponse, GenerationType } from '@/types/generation'

interface GenerationState {
  isGenerating: boolean
  currentRequest: GenerationRequest | null
  lastResponse: GenerationResponse | null
  history: GenerationResponse[]
  error: string | null
  progress: number
}

const initialState: GenerationState = {
  isGenerating: false,
  currentRequest: null,
  lastResponse: null,
  history: [],
  error: null,
  progress: 0,
}

export const generateText = createAsyncThunk(
  'generation/generateText',
  async (request: GenerationRequest) => {
    return await generationService.generateText(request)
  }
)

export const generateScene = createAsyncThunk(
  'generation/generateScene',
  async (params: {
    documentId: string
    chapterId: string
    prompt: string
    context?: string
  }) => {
    const request: GenerationRequest = {
      prompt: params.prompt,
      generationType: 'SCENE' as GenerationType,
      contextId: params.documentId,
      metadata: {
        chapterId: params.chapterId,
        context: params.context,
      },
    }
    return await generationService.generateText(request)
  }
)

export const continueGeneration = createAsyncThunk(
  'generation/continue',
  async (params: {
    documentId: string
    previousText: string
  }) => {
    const request: GenerationRequest = {
      prompt: 'Continue the story from where it left off',
      generationType: 'CONTINUATION' as GenerationType,
      contextId: params.documentId,
      metadata: {
        previousText: params.previousText,
      },
    }
    return await generationService.generateText(request)
  }
)

const generationSlice = createSlice({
  name: 'generation',
  initialState,
  reducers: {
    setProgress: (state, action: PayloadAction<number>) => {
      state.progress = action.payload
    },
    clearError: (state) => {
      state.error = null
    },
    clearHistory: (state) => {
      state.history = []
    },
  },
  extraReducers: (builder) => {
    builder
      // Generate Text
      .addCase(generateText.pending, (state, action) => {
        state.isGenerating = true
        state.currentRequest = action.meta.arg
        state.error = null
        state.progress = 0
      })
      .addCase(generateText.fulfilled, (state, action) => {
        state.isGenerating = false
        state.lastResponse = action.payload
        state.history.unshift(action.payload)
        state.progress = 100
      })
      .addCase(generateText.rejected, (state, action) => {
        state.isGenerating = false
        state.error = action.error.message || 'Generation failed'
        state.progress = 0
      })
      // Generate Scene
      .addCase(generateScene.pending, (state) => {
        state.isGenerating = true
        state.error = null
        state.progress = 0
      })
      .addCase(generateScene.fulfilled, (state, action) => {
        state.isGenerating = false
        state.lastResponse = action.payload
        state.history.unshift(action.payload)
        state.progress = 100
      })
      .addCase(generateScene.rejected, (state, action) => {
        state.isGenerating = false
        state.error = action.error.message || 'Scene generation failed'
        state.progress = 0
      })
      // Continue Generation
      .addCase(continueGeneration.pending, (state) => {
        state.isGenerating = true
        state.error = null
        state.progress = 0
      })
      .addCase(continueGeneration.fulfilled, (state, action) => {
        state.isGenerating = false
        state.lastResponse = action.payload
        state.history.unshift(action.payload)
        state.progress = 100
      })
      .addCase(continueGeneration.rejected, (state, action) => {
        state.isGenerating = false
        state.error = action.error.message || 'Continuation failed'
        state.progress = 0
      })
  },
})

export const { setProgress, clearError, clearHistory } = generationSlice.actions
export default generationSlice.reducer