import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { documentService } from '@/services/document'
import type { Document, CreateDocumentInput, Chapter, Scene } from '@/types/document'

interface DocumentsState {
  documents: Document[]
  currentDocument: Document | null
  currentChapter: Chapter | null
  currentScene: Scene | null
  isLoading: boolean
  error: string | null
}

const initialState: DocumentsState = {
  documents: [],
  currentDocument: null,
  currentChapter: null,
  currentScene: null,
  isLoading: false,
  error: null,
}

export const fetchDocuments = createAsyncThunk(
  'documents/fetchAll',
  async () => {
    return await documentService.getDocuments()
  }
)

export const fetchDocument = createAsyncThunk(
  'documents/fetchOne',
  async (documentId: string) => {
    return await documentService.getDocument(documentId)
  }
)

export const getDocument = createAsyncThunk(
  'documents/getOne',
  async (documentId: string) => {
    return await documentService.getDocument(documentId)
  }
)

export const createDocument = createAsyncThunk(
  'documents/create',
  async (input: CreateDocumentInput) => {
    return await documentService.createDocument(input)
  }
)

export const updateDocument = createAsyncThunk(
  'documents/update',
  async ({ id, data }: { id: string; data: Partial<Document> }) => {
    return await documentService.updateDocument(id, data)
  }
)

export const deleteDocument = createAsyncThunk(
  'documents/delete',
  async (documentId: string) => {
    await documentService.deleteDocument(documentId)
    return documentId
  }
)

export const updateScene = createAsyncThunk(
  'documents/updateScene',
  async ({ 
    documentId, 
    chapterNumber, 
    sceneNumber, 
    content 
  }: { 
    documentId: string
    chapterNumber: number
    sceneNumber: number
    content: string 
  }) => {
    // TODO: Implement scene update in document service
    return { documentId, chapterNumber, sceneNumber, content }
  }
)

const documentsSlice = createSlice({
  name: 'documents',
  initialState,
  reducers: {
    setCurrentDocument: (state, action: PayloadAction<Document | null>) => {
      state.currentDocument = action.payload
    },
    setCurrentChapter: (state, action: PayloadAction<Chapter | null>) => {
      state.currentChapter = action.payload
    },
    setCurrentScene: (state, action: PayloadAction<Scene | null>) => {
      state.currentScene = action.payload
    },
    clearError: (state) => {
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch All
      .addCase(fetchDocuments.pending, (state) => {
        state.isLoading = true
      })
      .addCase(fetchDocuments.fulfilled, (state, action) => {
        state.isLoading = false
        state.documents = action.payload
      })
      .addCase(fetchDocuments.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.error.message || 'Failed to fetch documents'
      })
      // Fetch One
      .addCase(fetchDocument.fulfilled, (state, action) => {
        state.currentDocument = action.payload
      })
      // Create
      .addCase(createDocument.fulfilled, (state, action) => {
        state.documents.push(action.payload)
      })
      // Update
      .addCase(updateDocument.fulfilled, (state, action) => {
        const index = state.documents.findIndex(d => d.id === action.payload.id)
        if (index !== -1) {
          state.documents[index] = action.payload
        }
        if (state.currentDocument?.id === action.payload.id) {
          state.currentDocument = action.payload
        }
      })
      // Get One
      .addCase(getDocument.fulfilled, (state, action) => {
        state.currentDocument = action.payload
      })
      // Delete
      .addCase(deleteDocument.fulfilled, (state, action) => {
        state.documents = state.documents.filter(d => d.id !== action.payload)
        if (state.currentDocument?.id === action.payload) {
          state.currentDocument = null
        }
      })
  },
})

export const { setCurrentDocument, setCurrentChapter, setCurrentScene, clearError } = documentsSlice.actions
export default documentsSlice.reducer