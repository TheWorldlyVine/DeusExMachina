import { useParams } from 'react-router-dom'
import { useState, useEffect, useRef, useCallback } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { generateScene, continueGeneration } from '@/features/generation/generationSlice'
import { getDocument, updateScene } from '@/features/documents/documentSlice'
import { getCharacters } from '@/features/memory/enhancedMemorySlice'
import { MonacoEditor } from '@/components/editor/MonacoEditor'
import { EditorToolbar } from '@/components/editor/EditorToolbar'
import { AIAssistantPanel } from '@/components/editor/AIAssistantPanel'
import toast from 'react-hot-toast'

export function EnhancedEditorPage() {
  const { documentId } = useParams()
  const dispatch = useAppDispatch()
  const containerRef = useRef<HTMLDivElement>(null)
  // const monacoEditorRef = useRef<any>(null)
  
  const [content, setContent] = useState('')
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
      dispatch(getCharacters(documentId))
    }
  }, [documentId, dispatch])
  
  // Load current scene content
  useEffect(() => {
    if (currentScene?.content) {
      setContent(currentScene.content)
    }
  }, [currentScene])
  
  // Handle generated text
  useEffect(() => {
    if (lastResponse?.generatedText) {
      const newContent = content + '\n\n' + lastResponse.generatedText
      setContent(newContent)
      saveContent(newContent)
    }
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
  
  // Auto-save content
  const saveContent = useCallback((newContent: string) => {
    if (documentId && currentChapter && currentScene) {
      dispatch(updateScene({
        documentId,
        chapterNumber: currentChapter.chapterNumber,
        sceneNumber: currentScene.sceneNumber,
        content: newContent,
      }))
    }
  }, [documentId, currentChapter, currentScene, dispatch])
  
  const handleContentChange = (value: string) => {
    setContent(value)
    saveContent(value)
  }
  
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
        toast.info('Settings coming soon')
        break
    }
  }
  
  const handleGenerateScene = async (options?: Record<string, unknown>) => {
    if (!documentId) return
    
    const prompt = selectedText || 'Generate a new scene for this chapter'
    
    await dispatch(generateScene({
      documentId,
      chapterNumber: currentChapter?.chapterNumber || 1,
      sceneNumber: currentScene?.sceneNumber || 1,
      prompt,
      context: content.slice(-1000),
      ...options,
    }))
  }
  
  const handleContinueWriting = async (options?: Record<string, unknown>) => {
    if (!documentId || !content) return
    
    await dispatch(continueGeneration({
      documentId,
      chapterNumber: currentChapter?.chapterNumber || 1,
      sceneNumber: currentScene?.sceneNumber || 1,
      previousText: content.slice(-2000),
      ...options,
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
              plots={plots}
              locations={locations}
            />
          </div>
        )}
      </div>
    </div>
  )
}

