import { useParams } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { generateScene, continueGeneration } from '@/features/generation/generationSlice'
import { getDocument } from '@/features/documents/documentSlice'
import toast from 'react-hot-toast'

export function EditorPage() {
  const { documentId } = useParams()
  const dispatch = useAppDispatch()
  const [content, setContent] = useState('')
  const [selectedText, setSelectedText] = useState('')
  
  const { isGenerating, lastResponse, error } = useAppSelector(state => state.generation)
  const { currentDocument } = useAppSelector(state => state.documents)
  
  useEffect(() => {
    if (documentId) {
      dispatch(getDocument(documentId))
    }
  }, [documentId, dispatch])
  
  useEffect(() => {
    if (lastResponse?.generatedText) {
      // Append generated text to content
      setContent(prev => prev + '\n\n' + lastResponse.generatedText)
    }
  }, [lastResponse])
  
  useEffect(() => {
    if (error) {
      toast.error(error)
    }
  }, [error])
  
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
  
  return (
    <div className="h-full flex">
      <div className="flex-1 bg-background">
        <div className="editor-container">
          <div className="editor-content">
            <h1 className="text-2xl font-bold mb-4">
              {currentDocument?.title || 'Document Editor'}
            </h1>
            <p className="text-muted-foreground">
              {currentDocument?.description || `Editing document: ${documentId}`}
            </p>
            <div className="mt-8">
              <textarea 
                className="w-full h-96 p-4 border border-input rounded-md resize-none focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="Start writing your story..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
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