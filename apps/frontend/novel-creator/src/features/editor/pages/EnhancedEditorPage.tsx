import { useParams } from 'react-router-dom'
import { useState, useEffect, useRef, useCallback } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { generateScene, continueGeneration } from '@/features/generation/generationSlice'
import { getDocument } from '@/features/documents/documentSlice'
import { loadDocument, updateContent as updateEditorContent, clearEditor } from '@/features/editor/editorSlice'
import { getCharacters } from '@/features/memory/enhancedMemorySlice'
import { useAutoSave } from '@/hooks/useAutoSave'
import { SaveIndicator } from '@/components/SaveIndicator'
import { MonacoEditor } from '@/components/editor/MonacoEditor'
import { EditorToolbar } from '@/components/editor/EditorToolbar'
import { AIAssistantPanel } from '@/components/editor/AIAssistantPanel'
import toast from 'react-hot-toast'

export function EnhancedEditorPage() {
  const { documentId } = useParams()
  const dispatch = useAppDispatch()
  const containerRef = useRef<HTMLDivElement>(null)
  // const monacoEditorRef = useRef<any>(null)
  
  // Get editor state from Redux
  const { content: editorContent, isLoading: editorLoading, isSaving, lastSaved, isDirty, error: editorError } = useAppSelector(state => state.editor)
  const content = editorContent?.content || ''
  const [selectedText, setSelectedText] = useState('')
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [isDarkMode, setIsDarkMode] = useState(() => {
    return localStorage.getItem('editor-theme') === 'dark'
  })
  const [wordCount, setWordCount] = useState(0)
  const [characterCount, setCharacterCount] = useState(0)
  const [showAIPanel] = useState(true)
  
  const { isGenerating, lastResponse, error } = useAppSelector(state => state.generation)
  const { currentDocument, currentChapter, currentScene } = useAppSelector(state => state.documents)
  const { characters, plots, locations } = useAppSelector(state => state.memory)
  
  // Load document and related data
  useEffect(() => {
    if (documentId) {
      dispatch(getDocument(documentId))
      dispatch(loadDocument(documentId))
      dispatch(getCharacters(documentId))
    }
    
    // Cleanup when unmounting
    return () => {
      dispatch(clearEditor())
    }
  }, [documentId, dispatch])
  
  // Auto-save hook
  const { saveNow } = useAutoSave({
    documentId,
    content,
    delay: 2000, // Save 2 seconds after user stops typing
    enabled: !editorLoading && !!documentId && !!currentScene,
    onSaveError: (error) => {
      console.error('Auto-save failed:', error)
    }
  })
  
  // Handle generated text
  useEffect(() => {
    if (lastResponse?.generatedText) {
      const newContent = content + '\n\n' + lastResponse.generatedText
      dispatch(updateEditorContent(newContent))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lastResponse])
  
  // Handle errors
  useEffect(() => {
    if (error) {
      toast.error(error)
    }
  }, [error])
  
  // Calculate word and character counts
  useEffect(() => {
    const words = content.trim().split(/\s+/).filter(word => word.length > 0).length
    const characters = content.length
    setWordCount(words)
    setCharacterCount(characters)
  }, [content])
  
  // Handle errors
  useEffect(() => {
    if (error) {
      toast.error(error)
    }
    if (editorError) {
      toast.error(`Save error: ${editorError}`)
    }
  }, [error, editorError])
  
  const handleContentChange = useCallback((value: string) => {
    dispatch(updateEditorContent(value))
  }, [dispatch])
  
  const handleEditorCommand = (command: string) => {
    // TODO: Implement editor commands with Monaco editor ref
    // For now, we'll use simple text manipulation
    
    switch (command) {
      case 'bold':
        if (selectedText) {
          const newContent = content.replace(selectedText, `**${selectedText}**`)
          setContent(newContent)
        }
        break
      case 'italic':
        if (selectedText) {
          const newContent = content.replace(selectedText, `*${selectedText}*`)
          setContent(newContent)
        }
        break
      case 'heading1':
        setContent(content + '\n# ')
        break
      case 'heading2':
        setContent(content + '\n## ')
        break
      case 'heading3':
        setContent(content + '\n### ')
        break
      case 'bulletList':
        setContent(content + '\n- ')
        break
      case 'orderedList':
        setContent(content + '\n1. ')
        break
      case 'blockquote':
        setContent(content + '\n> ')
        break
      case 'horizontalRule':
        setContent(content + '\n\n---\n\n')
        break
      case 'settings':
        // TODO: Open settings modal
        toast('Settings coming soon')
        break
    }
  }
  
  const handleGenerateScene = async (options?: Record<string, unknown>) => {
    if (!documentId) return
    
    const prompt = selectedText || 'Generate a new scene for this chapter'
    
    await dispatch(generateScene({
      documentId,
      chapterId: currentChapter ? `chapter-${currentChapter.chapterNumber}` : 'chapter-1',
      prompt,
      context: content.slice(-1000),
      ...options,
    }))
  }
  
  const handleContinueWriting = async () => {
    if (!documentId || !content) return
    
    await dispatch(continueGeneration({
      documentId,
      previousText: content.slice(-2000),
    }))
  }
  
  const handleGenerateIdeas = (type: string) => {
    // TODO: Implement idea generation
    toast.success(`Generating ${type} ideas...`)
  }
  
  const handleAnalyzeText = () => {
    // TODO: Implement text analysis
    toast.success('Analyzing selected text...')
  }
  
  const handleExport = (format: 'pdf' | 'docx' | 'txt') => {
    // TODO: Implement export functionality
    toast.success(`Exporting as ${format.toUpperCase()}...`)
  }
  
  const toggleTheme = () => {
    const newTheme = !isDarkMode
    setIsDarkMode(newTheme)
    localStorage.setItem('editor-theme', newTheme ? 'dark' : 'light')
  }
  
  const toggleFullscreen = () => {
    if (!containerRef.current) return
    
    if (!isFullscreen) {
      containerRef.current.requestFullscreen()
    } else {
      document.exitFullscreen()
    }
    setIsFullscreen(!isFullscreen)
  }
  
  return (
    <div 
      ref={containerRef}
      className={`h-full flex flex-col ${isDarkMode ? 'dark' : ''}`}
    >
      <EditorToolbar
        onCommand={handleEditorCommand}
        onThemeToggle={toggleTheme}
        onFullscreenToggle={toggleFullscreen}
        onExport={handleExport}
        isFullscreen={isFullscreen}
        isDarkMode={isDarkMode}
        wordCount={wordCount}
        characterCount={characterCount}
      />
      
      <div className="flex-1 flex overflow-hidden">
        <div className="flex-1 flex flex-col">
          <div className="px-4 py-2 border-b border-border bg-background/95 backdrop-blur">
            <h1 className="text-xl font-semibold">
              {currentDocument?.title || 'Untitled Document'}
            </h1>
            <div className="text-sm text-muted-foreground">
              Chapter {currentChapter?.chapterNumber || 1} • Scene {currentScene?.sceneNumber || 1}
            </div>
          </div>
          
          <div className="flex-1">
            <MonacoEditor
              value={content}
              onChange={handleContentChange}
              onSelectionChange={setSelectedText}
              disabled={isGenerating}
              theme={isDarkMode ? 'dark' : 'light'}
              wordWrap="on"
              fontSize={16}
              lineNumbers="off"
              minimap={false}
              scrollBeyondLastLine={true}
              automaticLayout={true}
            />
          </div>
        </div>
        
        {showAIPanel && (
          <div className="w-80">
            <AIAssistantPanel
              selectedText={selectedText}
              isGenerating={isGenerating}
              onGenerateScene={handleGenerateScene}
              onContinueWriting={handleContinueWriting}
              onGenerateIdeas={handleGenerateIdeas}
              onAnalyzeText={handleAnalyzeText}
              characters={characters}
              plots={plots.map(p => ({ plotId: p.plotId, title: p.title, status: p.currentState?.status || 'active' }))}
              locations={locations}
              footer={
                <div className="p-4 border-t border-border space-y-2">
                  <SaveIndicator 
                    isSaving={isSaving}
                    lastSaved={lastSaved}
                    error={editorError}
                    isDirty={isDirty}
                  />
                  <button
                    onClick={saveNow}
                    disabled={isSaving || !isDirty}
                    className="w-full px-3 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed text-sm"
                  >
                    Save Now
                  </button>
                  <div className="text-xs text-muted-foreground text-center">
                    Auto-save enabled • Saves 2s after you stop typing
                  </div>
                </div>
              }
            />
          </div>
        )}
      </div>
    </div>
  )
}

