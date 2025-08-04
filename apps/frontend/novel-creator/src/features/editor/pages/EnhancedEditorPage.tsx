import { useParams } from 'react-router-dom'
import { useState, useEffect, useRef, useCallback } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { generateScene, continueGeneration } from '@/features/generation/generationSlice'
import { getDocument } from '@/features/documents/documentSlice'
import { loadDocument, updateContent as updateEditorContent, clearEditor, createChapter, createScene, setCurrentChapter, setCurrentScene } from '@/features/editor/editorSlice'
import { getCharacters } from '@/features/memory/enhancedMemorySlice'
import { useAutoSave } from '@/hooks/useAutoSave'
import { SaveIndicator } from '@/components/SaveIndicator'
import { RichTextEditor } from '@/components/editor/RichTextEditor'
import { RichTextToolbar } from '@/components/editor/RichTextToolbar'
import { useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import Highlight from '@tiptap/extension-highlight'
import CharacterCount from '@tiptap/extension-character-count'
import Typography from '@tiptap/extension-typography'
import { AIAssistantPanel } from '@/components/editor/AIAssistantPanel'
import { DocumentOutlinePanel } from '@/components/editor/DocumentOutlinePanel'
import { ExportDialog } from '@/components/export/ExportDialog'
import toast from 'react-hot-toast'
import { 
  PanelLeftClose, 
  PanelLeftOpen, 
  PanelRightClose, 
  PanelRightOpen 
} from 'lucide-react'

export function EnhancedEditorPage() {
  const { documentId } = useParams()
  const dispatch = useAppDispatch()
  const containerRef = useRef<HTMLDivElement>(null)
  
  // Get editor state from Redux
  const { content: editorContent, chapters, currentChapter, currentScene, isLoading: editorLoading, isSaving, lastSaved, isDirty, error: editorError } = useAppSelector(state => state.editor)
  const content = editorContent?.content || ''
  const [selectedText, setSelectedText] = useState('')
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [isDarkMode, setIsDarkMode] = useState(() => {
    return localStorage.getItem('editor-theme') === 'dark'
  })
  const [wordCount, setWordCount] = useState(0)
  const [characterCount, setCharacterCount] = useState(0)
  const [showAIPanel, setShowAIPanel] = useState(true)
  const [showLeftPanel, setShowLeftPanel] = useState(true)
  const [showExportDialog, setShowExportDialog] = useState(false)
  
  const { isGenerating, lastResponse, error } = useAppSelector(state => state.generation)
  const { currentDocument } = useAppSelector(state => state.documents)
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
  
  // Initialize TipTap editor
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: {
          levels: [1, 2, 3],
        },
      }),
      Placeholder.configure({
        placeholder: 'Start writing your novel...',
        emptyEditorClass: 'is-editor-empty',
      }),
      Underline,
      TextAlign.configure({
        types: ['heading', 'paragraph'],
      }),
      Highlight,
      CharacterCount.configure({
        limit: null,
      }),
      Typography,
    ],
    content: content,
    onUpdate: ({ editor }) => {
      const html = editor.getHTML()
      handleContentChange(html)
    },
    onSelectionUpdate: ({ editor }) => {
      const { from, to } = editor.state.selection
      const selectedText = editor.state.doc.textBetween(from, to)
      setSelectedText(selectedText)
    },
  })

  // Update editor content when Redux content changes
  useEffect(() => {
    if (editor && content !== editor.getHTML()) {
      editor.commands.setContent(content)
    }
  }, [content, editor])

  // Update character and word count from editor
  useEffect(() => {
    if (editor) {
      const updateCount = () => {
        const chars = editor.storage.characterCount.characters()
        const words = editor.storage.characterCount.words()
        setCharacterCount(chars)
        setWordCount(words)
      }

      updateCount()
      editor.on('update', updateCount)

      return () => {
        editor.off('update', updateCount)
      }
    }
  }, [editor])
  
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
  
  const handleSettingsClick = () => {
    // TODO: Open settings modal
    toast('Settings coming soon')
  }

  // Chapter/Scene management handlers
  const handleAddChapter = async () => {
    if (!documentId) return
    
    const title = prompt('Enter chapter title:')
    if (!title) return
    
    try {
      await dispatch(createChapter({ documentId, title }))
      toast.success('Chapter created successfully')
    } catch (error) {
      toast.error('Failed to create chapter')
    }
  }
  
  const handleAddScene = async (chapterId: string) => {
    const title = prompt('Enter scene title:')
    if (!title) return
    
    try {
      await dispatch(createScene({ chapterId, title }))
      toast.success('Scene created successfully')
    } catch (error) {
      toast.error('Failed to create scene')
    }
  }
  
  const handleChapterSelect = (chapterId: string) => {
    const chapter = chapters.find(ch => ch.id === chapterId)
    if (chapter) {
      dispatch(setCurrentChapter(chapter))
      
      // Select first scene in chapter
      if (chapter.scenes.length > 0) {
        handleSceneSelect(chapterId, chapter.scenes[0].id)
      }
    }
  }
  
  const handleSceneSelect = (chapterId: string, sceneId: string) => {
    const chapter = chapters.find(ch => ch.id === chapterId)
    const scene = chapter?.scenes.find(s => s.id === sceneId)
    if (scene) {
      dispatch(setCurrentScene(scene))
    }
  }
  
  const handleEditChapter = (_chapterId: string) => {
    void _chapterId // Unused but required by interface
    // TODO: Implement edit chapter API
    toast('Edit chapter functionality coming soon')
  }
  
  const handleEditScene = (_chapterId: string, _sceneId: string) => {
    void _chapterId // Unused but required by interface
    void _sceneId // Unused but required by interface
    // TODO: Implement edit scene
    toast('Edit scene functionality coming soon')
  }
  
  const handleDeleteChapter = (_chapterId: string) => {
    void _chapterId // Unused but required by interface
    if (confirm('Are you sure you want to delete this chapter?')) {
      // TODO: Implement delete chapter API
      toast('Delete chapter functionality coming soon')
    }
  }
  
  const handleDeleteScene = (_chapterId: string, _sceneId: string) => {
    void _chapterId // Unused but required by interface
    void _sceneId // Unused but required by interface
    if (confirm('Are you sure you want to delete this scene?')) {
      // TODO: Implement delete scene API
      toast('Delete scene functionality coming soon')
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
  
  const handleExport = (_format: 'pdf' | 'docx' | 'txt') => {
    void _format // Unused but required by interface
    setShowExportDialog(true)
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
      <RichTextToolbar
        editor={editor}
        onThemeToggle={toggleTheme}
        onFullscreenToggle={toggleFullscreen}
        onExport={handleExport}
        isFullscreen={isFullscreen}
        isDarkMode={isDarkMode}
        wordCount={wordCount}
        characterCount={characterCount}
        onSettingsClick={handleSettingsClick}
      />
      
      <div className="flex-1 flex overflow-hidden relative">
        {/* Left Panel - Document Outline */}
        {showLeftPanel && (
          <div className="w-80 flex-shrink-0">
            <DocumentOutlinePanel
              chapters={chapters.map(ch => ({
                id: ch.id,
                number: ch.chapterNumber,
                title: ch.title,
                wordCount: ch.wordCount,
                scenes: ch.scenes.map(sc => ({
                  id: sc.id,
                  number: sc.sceneNumber,
                  title: sc.title,
                  wordCount: sc.wordCount,
                  summary: sc.summary,
                })),
              }))}
              currentChapterId={currentChapter?.id}
              currentSceneId={currentScene?.id}
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
          className="absolute left-0 top-1/2 transform -translate-y-1/2 z-10 p-2 bg-background border border-border rounded-r-md hover:bg-muted transition-colors"
          style={{ left: showLeftPanel ? '320px' : '0' }}
        >
          {showLeftPanel ? <PanelLeftClose className="h-4 w-4" /> : <PanelLeftOpen className="h-4 w-4" />}
        </button>
        
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
            <RichTextEditor
              value={content}
              onChange={handleContentChange}
              onSelectionChange={setSelectedText}
              disabled={isGenerating}
              placeholder="Start writing your novel..."
              className={isDarkMode ? 'dark' : ''}
            />
          </div>
        </div>
        
        {/* Toggle Right Panel Button */}
        <button
          onClick={() => setShowAIPanel(!showAIPanel)}
          className="absolute right-0 top-1/2 transform -translate-y-1/2 z-10 p-2 bg-background border border-border rounded-l-md hover:bg-muted transition-colors"
          style={{ right: showAIPanel ? '320px' : '0' }}
        >
          {showAIPanel ? <PanelRightClose className="h-4 w-4" /> : <PanelRightOpen className="h-4 w-4" />}
        </button>
        
        {showAIPanel && (
          <div className="w-80 flex-shrink-0">
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
      
      {/* Export Dialog */}
      {showExportDialog && currentDocument && (
        <ExportDialog
          isOpen={showExportDialog}
          onClose={() => setShowExportDialog(false)}
          document={currentDocument}
          chapters={chapters.map(ch => ({
            chapterNumber: ch.chapterNumber,
            title: ch.title,
            summary: ch.summary,
            scenes: ch.scenes.map(sc => ({
              sceneNumber: sc.sceneNumber,
              title: sc.title,
              content: sc.content,
              type: sc.type === 'NARRATIVE' ? 'DESCRIPTION' : (sc.type as any),
              wordCount: sc.wordCount,
              characters: sc.characterIds,
              location: sc.locationId,
              metadata: sc.metadata,
            })),
            wordCount: ch.wordCount,
            status: 'DRAFT' as const,
          }))}
        />
      )}
    </div>
  )
}

