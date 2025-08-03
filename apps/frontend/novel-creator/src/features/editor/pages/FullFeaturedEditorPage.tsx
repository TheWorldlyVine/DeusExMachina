import { useParams, useNavigate } from 'react-router-dom'
import { useState, useEffect, useRef, useCallback } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { generateScene, continueGeneration } from '@/features/generation/generationSlice'
import { getDocument, updateScene, setCurrentChapter, setCurrentScene } from '@/features/documents/documentSlice'
import { getCharacters, getPlots, getLocations } from '@/features/memory/enhancedMemorySlice'
import { MonacoEditor } from '@/components/editor/MonacoEditor'
import { EditorToolbar } from '@/components/editor/EditorToolbar'
import { AIAssistantPanel } from '@/components/editor/AIAssistantPanel'
import { DocumentOutlinePanel } from '@/components/editor/DocumentOutlinePanel'
import toast from 'react-hot-toast'
import { debounce } from 'lodash'
import { 
  PanelLeftClose, 
  PanelLeftOpen, 
  PanelRightClose, 
  PanelRightOpen 
} from 'lucide-react'

export function FullFeaturedEditorPage() {
  const { documentId } = useParams()
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const containerRef = useRef<HTMLDivElement>(null)
  
  const [content, setContent] = useState('')
  const [selectedText, setSelectedText] = useState('')
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [isDarkMode, setIsDarkMode] = useState(() => {
    return localStorage.getItem('editor-theme') === 'dark'
  })
  const [wordCount, setWordCount] = useState(0)
  const [characterCount, setCharacterCount] = useState(0)
  const [showLeftPanel, setShowLeftPanel] = useState(true)
  const [showRightPanel, setShowRightPanel] = useState(true)
  
  const { isGenerating, lastResponse, error } = useAppSelector(state => state.generation)
  const { currentDocument, currentChapter, currentScene } = useAppSelector(state => state.documents)
  const { characters, plots, locations } = useAppSelector(state => state.memory)
  
  // Mock chapters data for demonstration
  const mockChapters = [
    {
      id: '1',
      number: 1,
      title: 'The Beginning',
      wordCount: 5432,
      scenes: [
        { id: '1-1', number: 1, title: 'Opening Scene', wordCount: 1234, summary: 'The protagonist awakens in a strange place...' },
        { id: '1-2', number: 2, title: 'First Encounter', wordCount: 2198, summary: 'Meeting the mysterious stranger...' },
        { id: '1-3', number: 3, title: 'The Discovery', wordCount: 2000, summary: 'Finding the hidden truth...' },
      ]
    },
    {
      id: '2',
      number: 2,
      title: 'Rising Action',
      wordCount: 6789,
      scenes: [
        { id: '2-1', number: 1, title: 'The Chase', wordCount: 3456, summary: 'Running from danger...' },
        { id: '2-2', number: 2, title: 'Safe Haven', wordCount: 3333, summary: 'Finding temporary refuge...' },
      ]
    },
    {
      id: '3',
      number: 3,
      title: 'The Turning Point',
      wordCount: 7890,
      scenes: [
        { id: '3-1', number: 1, title: 'Revelation', wordCount: 4000, summary: 'The truth is revealed...' },
        { id: '3-2', number: 2, title: 'Decision Time', wordCount: 3890, summary: 'Making the crucial choice...' },
      ]
    },
  ]
  
  // Load document and related data
  useEffect(() => {
    if (documentId) {
      dispatch(getDocument(documentId))
      dispatch(getCharacters(documentId))
      dispatch(getPlots(documentId))
      dispatch(getLocations(documentId))
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
  const saveContent = useCallback(
    debounce((newContent: string) => {
      if (documentId && currentChapter && currentScene) {
        dispatch(updateScene({
          documentId,
          chapterNumber: currentChapter.chapterNumber,
          sceneNumber: currentScene.sceneNumber,
          content: newContent,
        }))
      }
    }, 2000),
    [documentId, currentChapter, currentScene, dispatch]
  )
  
  const handleContentChange = (value: string) => {
    setContent(value)
    saveContent(value)
  }
  
  const handleChapterSelect = (chapterId: string) => {
    const chapter = mockChapters.find(ch => ch.id === chapterId)
    if (chapter) {
      dispatch(setCurrentChapter({
        chapterNumber: chapter.number,
        title: chapter.title,
        summary: '',
        scenes: [],
        wordCount: chapter.wordCount,
        status: 'DRAFT',
      }))
      
      // Select first scene in chapter
      if (chapter.scenes.length > 0) {
        handleSceneSelect(chapterId, chapter.scenes[0].id)
      }
    }
  }
  
  const handleSceneSelect = (chapterId: string, sceneId: string) => {
    const chapter = mockChapters.find(ch => ch.id === chapterId)
    const scene = chapter?.scenes.find(s => s.id === sceneId)
    if (scene) {
      dispatch(setCurrentScene({
        sceneNumber: scene.number,
        title: scene.title,
        content: scene.summary || '', // Would load actual content
        type: 'DESCRIPTION',
        wordCount: scene.wordCount,
      }))
    }
  }
  
  const handleEditorCommand = (command: string) => {
    // Same as before
    switch (command) {
      case 'bold':
        if (selectedText) {
          const newContent = content.replace(selectedText, `**${selectedText}**`)
          setContent(newContent)
        }
        break
      // ... other cases
    }
  }
  
  const handleGenerateScene = async (options?: any) => {
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
  
  const handleContinueWriting = async (options?: any) => {
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
    toast.success(`Generating ${type} ideas...`)
  }
  
  const handleAnalyzeText = () => {
    toast.success('Analyzing selected text...')
  }
  
  const handleExport = (format: 'pdf' | 'docx' | 'txt') => {
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
  
  // Chapter/Scene management handlers
  const handleAddChapter = () => {
    toast.success('Adding new chapter...')
  }
  
  const handleAddScene = (chapterId: string) => {
    toast.success(`Adding scene to chapter ${chapterId}...`)
  }
  
  const handleEditChapter = (chapterId: string) => {
    toast.info(`Editing chapter ${chapterId}...`)
  }
  
  const handleEditScene = (chapterId: string, sceneId: string) => {
    toast.info(`Editing scene ${sceneId}...`)
  }
  
  const handleDeleteChapter = (chapterId: string) => {
    if (confirm('Are you sure you want to delete this chapter?')) {
      toast.success(`Deleted chapter ${chapterId}`)
    }
  }
  
  const handleDeleteScene = (chapterId: string, sceneId: string) => {
    if (confirm('Are you sure you want to delete this scene?')) {
      toast.success(`Deleted scene ${sceneId}`)
    }
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
        {/* Left Panel - Document Outline */}
        {showLeftPanel && (
          <div className="w-80 flex-shrink-0">
            <DocumentOutlinePanel
              chapters={mockChapters}
              currentChapterId={currentChapter ? String(currentChapter.chapterNumber) : undefined}
              currentSceneId={currentScene ? `${currentChapter?.chapterNumber}-${currentScene.sceneNumber}` : undefined}
              onChapterSelect={handleChapterSelect}
              onSceneSelect={handleSceneSelect}
              onAddChapter={handleAddChapter}
              onAddScene={handleAddScene}
              onEditChapter={handleEditChapter}
              onEditScene={handleEditScene}
              onDeleteChapter={handleDeleteChapter}
              onDeleteScene={handleDeleteScene}
            />
          </div>
        )}
        
        {/* Toggle Left Panel Button */}
        <button
          onClick={() => setShowLeftPanel(!showLeftPanel)}
          className="absolute left-0 top-1/2 transform -translate-y-1/2 z-10 p-2 bg-background border border-border rounded-r-md hover:bg-muted"
          style={{ left: showLeftPanel ? '320px' : '0' }}
        >
          {showLeftPanel ? <PanelLeftClose className="h-4 w-4" /> : <PanelLeftOpen className="h-4 w-4" />}
        </button>
        
        {/* Main Editor */}
        <div className="flex-1 flex flex-col">
          <div className="px-4 py-2 border-b border-border bg-background/95 backdrop-blur">
            <h1 className="text-xl font-semibold">
              {currentDocument?.title || 'Untitled Document'}
            </h1>
            <div className="text-sm text-muted-foreground">
              {currentChapter && `Chapter ${currentChapter.chapterNumber}: ${currentChapter.title}`}
              {currentScene && ` • Scene ${currentScene.sceneNumber}`}
              {currentScene?.title && `: ${currentScene.title}`}
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
        
        {/* Toggle Right Panel Button */}
        <button
          onClick={() => setShowRightPanel(!showRightPanel)}
          className="absolute right-0 top-1/2 transform -translate-y-1/2 z-10 p-2 bg-background border border-border rounded-l-md hover:bg-muted"
          style={{ right: showRightPanel ? '320px' : '0' }}
        >
          {showRightPanel ? <PanelRightClose className="h-4 w-4" /> : <PanelRightOpen className="h-4 w-4" />}
        </button>
        
        {/* Right Panel - AI Assistant */}
        {showRightPanel && (
          <div className="w-80 flex-shrink-0">
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