import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { memoryService } from '@/services/memory'
import type { Memory, CreateMemoryInput } from '@/types/memory'

interface MemoryState {
  memories: Memory[]
  characterMemories: Record<string, Memory[]>
  isLoading: boolean
  error: string | null
}

const initialState: MemoryState = {
  memories: [],
  characterMemories: {},
  isLoading: false,
  error: null,
}

export const fetchMemories = createAsyncThunk(
  'memory/fetchAll',
  async (contextId: string) => {
    return await memoryService.getMemories(contextId)
  }
)

export const fetchCharacterMemories = createAsyncThunk(
  'memory/fetchByCharacter',
  async ({ contextId, characterId }: { contextId: string; characterId: string }) => {
    const memories = await memoryService.getCharacterMemories(contextId, characterId)
    return { characterId, memories }
  }
)

export const createMemory = createAsyncThunk(
  'memory/create',
  async (input: CreateMemoryInput) => {
    return await memoryService.createMemory(input)
  }
)

export const searchMemories = createAsyncThunk(
  'memory/search',
  async ({ contextId, query }: { contextId: string; query: string }) => {
    return await memoryService.searchMemories(contextId, query)
  }
)

export const getRelevantMemories = createAsyncThunk(
  'memory/getRelevant',
  async ({ contextId, prompt }: { contextId: string; prompt: string }) => {
    return await memoryService.getRelevantMemories(contextId, prompt)
  }
)

const memorySlice = createSlice({
  name: 'memory',
  initialState,
  reducers: {
    clearMemories: (state) => {
      state.memories = []
      state.characterMemories = {}
    },
    clearError: (state) => {
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch All
      .addCase(fetchMemories.pending, (state) => {
        state.isLoading = true
      })
      .addCase(fetchMemories.fulfilled, (state, action) => {
        state.isLoading = false
        state.memories = action.payload
      })
      .addCase(fetchMemories.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.error.message || 'Failed to fetch memories'
      })
      // Fetch Character Memories
      .addCase(fetchCharacterMemories.fulfilled, (state, action) => {
        state.characterMemories[action.payload.characterId] = action.payload.memories
      })
      // Create Memory
      .addCase(createMemory.fulfilled, (state, action) => {
        state.memories.push(action.payload)
        // If it's a character memory, add to character memories
        if (action.payload.entityType === 'character' && action.payload.entityId) {
          if (!state.characterMemories[action.payload.entityId]) {
            state.characterMemories[action.payload.entityId] = []
          }
          state.characterMemories[action.payload.entityId].push(action.payload)
        }
      })
      // Search Memories
      .addCase(searchMemories.fulfilled, (state, action) => {
        state.memories = action.payload
      })
      // Get Relevant Memories
      .addCase(getRelevantMemories.fulfilled, (state, action) => {
        state.memories = action.payload
      })
  },
})

export const { clearMemories, clearError } = memorySlice.actions
export default memorySlice.reducer