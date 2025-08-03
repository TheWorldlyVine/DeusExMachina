import { configureStore } from '@reduxjs/toolkit'
import authReducer from '@/features/auth/authSlice'
import documentReducer from '@/features/documents/documentSlice'
import editorReducer from '@/features/editor/editorSlice'
import generationReducer from '@/features/generation/generationSlice'
import memoryReducer from '@/features/memory/enhancedMemorySlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    documents: documentReducer,
    editor: editorReducer,
    generation: generationReducer,
    memory: memoryReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // Ignore these action types
        ignoredActions: ['editor/updateContent'],
        // Ignore these field paths in all actions
        ignoredActionPaths: ['payload.timestamp'],
        // Ignore these paths in the state
        ignoredPaths: ['editor.lastSaved'],
      },
    }),
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch