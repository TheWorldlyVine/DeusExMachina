import { useParams } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { generateScene, continueGeneration } from '@/features/generation/generationSlice'
import { getDocument } from '@/features/documents/documentSlice'
import { loadDocument, updateContent, clearEditor } from '@/features/editor/editorSlice'
import { useAutoSave } from '@/hooks/useAutoSave'
import { SaveIndicator } from '@/components/SaveIndicator'
import toast from 'react-hot-toast'

export function EditorPage() {
  const { documentId } = useParams()
  const dispatch = useAppDispatch()
  const [selectedText, setSelectedText] = useState('')
  
  const { isGenerating, lastResponse, error: genError } = useAppSelector(state => state.generation)
  const { currentDocument } = useAppSelector(state => state.documents)
  const { content: editorContent, isLoading, isSaving, lastSaved, isDirty, error: editorError } = useAppSelector(state => state.editor)
  
  // Use content from Redux state
  const content = editorContent?.content || ''
  
  // Load document when component mounts or documentId changes
  useEffect(() => {
    if (documentId) {
      dispatch(getDocument(documentId))
      dispatch(loadDocument(documentId))
    }
    
    // Cleanup when unmounting
    return () => {
      dispatch(clearEditor())
    }
  }, [documentId, dispatch])
  
  // Handle generated text
  useEffect(() => {
    if (lastResponse?.generatedText) {
      // Append generated text to content using Redux
      const newContent = content + '\n\n' + lastResponse.generatedText
      dispatch(updateContent(newContent))
    }
  }, [lastResponse, content, dispatch])
  
  // Handle errors
  useEffect(() => {
    if (genError) {
      toast.error(genError)
    }
    if (editorError) {
      toast.error(editorError)
    }
  }, [genError, editorError])
  
  // Auto-save hook
  const { saveNow } = useAutoSave({
    documentId,
    content,
    delay: 2000, // Save 2 seconds after user stops typing
    enabled: !isLoading && !!documentId,
    onSaveError: (error) => {
      console.error('Auto-save failed:', error)
    }
  })
  
  // Handle content changes
  const handleContentChange = (value: string) => {
    dispatch(updateContent(value))
  }
  
  const handleGenerateScene = async () => {
    if (!documentId) return
    
    const prompt = selectedText || 'Generate a new scene for this chapter'
    
    await dispatch(generateScene({
      documentId,
      chapterId: 'chapter-1', // TODO: Get current chapter ID
      prompt,
      context: content.slice(-1000) // Last 1000 chars as context
    }))
  }
  
  const handleContinueWriting = async () => {
    if (!documentId || !content) return
    
    await dispatch(continueGeneration({
      documentId,
      previousText: content.slice(-2000) // Last 2000 chars
    }))
  }
  
  if (isLoading) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading document...</p>
        </div>
      </div>
    )
  }
  
  return (
    <div className="h-full flex">
      <div className="flex-1 bg-background">
        <div className="editor-container">
          <div className="editor-content">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h1 className="text-2xl font-bold">
                  {currentDocument?.title || 'Document Editor'}
                </h1>
                <p className="text-muted-foreground">
                  {currentDocument?.description || `Editing document: ${documentId}`}
                </p>
              </div>
              <SaveIndicator 
                isSaving={isSaving}
                lastSaved={lastSaved}
                error={editorError}
                isDirty={isDirty}
              />
            </div>
            <div className="mt-8">
              <textarea 
                className="w-full h-96 p-4 border border-input rounded-md resize-none focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="Start writing your story..."
                value={content}
                onChange={(e) => handleContentChange(e.target.value)}
                onSelect={(e) => {
                  const target = e.target as HTMLTextAreaElement
                  const selected = target.value.substring(target.selectionStart, target.selectionEnd)
                  setSelectedText(selected)
                }}
                disabled={isGenerating}
              />
            </div>
          </div>
        </div>
      </div>
      <div className="w-80 bg-muted/30 border-l border-border p-4">
        <h2 className="text-lg font-semibold mb-4">AI Assistant</h2>
        <button 
          onClick={handleGenerateScene}
          disabled={isGenerating}
          className="w-full px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 mb-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isGenerating ? 'Generating...' : 'Generate Scene'}
        </button>
        <button 
          onClick={handleContinueWriting}
          disabled={isGenerating || !content}
          className="w-full px-4 py-2 bg-secondary text-secondary-foreground rounded-md hover:bg-secondary/90 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isGenerating ? 'Generating...' : 'Continue Writing'}
        </button>
        
        <div className="mt-4 space-y-2">
          <button
            onClick={saveNow}
            disabled={isSaving || !isDirty}
            className="w-full px-4 py-2 bg-muted text-muted-foreground rounded-md hover:bg-muted/80 disabled:opacity-50 disabled:cursor-not-allowed text-sm"
          >
            Save Now
          </button>
          
          <div className="text-xs text-muted-foreground text-center">
            Auto-save enabled • Saves 2s after you stop typing
          </div>
        </div>
        
        {selectedText && (
          <div className="mt-4 p-3 bg-muted rounded-md">
            <p className="text-sm text-muted-foreground">Selected text:</p>
            <p className="text-sm mt-1 line-clamp-3">{selectedText}</p>
          </div>
        )}
      </div>
    </div>
  )
}