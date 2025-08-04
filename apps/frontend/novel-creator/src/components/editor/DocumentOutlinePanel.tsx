import { useState } from 'react'
import { 
  FileText, 
  ChevronRight, 
  ChevronDown,
  Search,
  Plus,
  MoreVertical,
  Edit2,
  Trash2,
} from 'lucide-react'
import { VirtualizedDocumentViewer } from './VirtualizedDocumentViewer'

interface Chapter {
  id: string
  number: number
  title: string
  wordCount: number
  scenes: Scene[]
  isExpanded?: boolean
}

interface Scene {
  id: string
  number: number
  title?: string
  wordCount: number
  summary?: string
}

interface DocumentOutlinePanelProps {
  chapters: Chapter[]
  currentChapterId?: string
  currentSceneId?: string
  onChapterSelect: (chapterId: string) => void
  onSceneSelect: (chapterId: string, sceneId: string) => void
  onAddChapter: () => void
  onAddScene: (chapterId: string) => void
  onEditChapter: (chapterId: string) => void
  onEditScene: (chapterId: string, sceneId: string) => void
  onDeleteChapter: (chapterId: string) => void
  onDeleteScene: (chapterId: string, sceneId: string) => void
}

export function DocumentOutlinePanel({
  chapters,
  currentChapterId,
  currentSceneId,
  onChapterSelect,
  onSceneSelect,
  onAddChapter,
  onAddScene,
  onEditChapter,
  onEditScene,
  onDeleteChapter,
  onDeleteScene,
}: DocumentOutlinePanelProps) {
  const [searchQuery, setSearchQuery] = useState('')
  const [expandedChapters, setExpandedChapters] = useState<Set<string>>(
    new Set(chapters.map(c => c.id))
  )
  const [showVirtualized, setShowVirtualized] = useState(false)

  const toggleChapter = (chapterId: string) => {
    const newExpanded = new Set(expandedChapters)
    if (newExpanded.has(chapterId)) {
      newExpanded.delete(chapterId)
    } else {
      newExpanded.add(chapterId)
    }
    setExpandedChapters(newExpanded)
  }

  const filteredChapters = chapters.filter(chapter => {
    if (!searchQuery) return true
    const query = searchQuery.toLowerCase()
    return (
      chapter.title.toLowerCase().includes(query) ||
      chapter.scenes.some(scene => 
        scene.title?.toLowerCase().includes(query) ||
        scene.summary?.toLowerCase().includes(query)
      )
    )
  })

  const totalWordCount = chapters.reduce((sum, ch) => sum + ch.wordCount, 0)

  return (
    <div className="h-full flex flex-col bg-muted/30 border-r border-border">
      <div className="p-4 border-b border-border">
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold flex items-center space-x-2">
            <FileText className="h-4 w-4" />
            <span>Document Outline</span>
          </h3>
          <button
            onClick={() => setShowVirtualized(!showVirtualized)}
            className="text-xs text-muted-foreground hover:text-foreground"
          >
            {showVirtualized ? 'List View' : 'Reader View'}
          </button>
        </div>
        
        <div className="relative mb-3">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search chapters and scenes..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-sm border border-input rounded-md bg-background"
          />
        </div>

        <div className="text-sm text-muted-foreground">
          {chapters.length} chapters • {totalWordCount.toLocaleString()} words
        </div>
      </div>

      <div className="flex-1 overflow-hidden">
        {showVirtualized ? (
          <VirtualizedDocumentViewer
            chapters={filteredChapters.map(ch => ({
              id: ch.id,
              title: `Chapter ${ch.number}: ${ch.title}`,
              content: ch.scenes.map(s => s.summary || '').join('\n\n'),
              wordCount: ch.wordCount,
              scenes: ch.scenes.map(s => ({
                id: s.id,
                content: s.summary || `Scene ${s.number}`,
                wordCount: s.wordCount,
              })),
            }))}
            onChapterClick={onChapterSelect}
            onSceneClick={onSceneSelect}
            searchQuery={searchQuery}
          />
        ) : (
          <div className="overflow-y-auto h-full">
            <div className="p-2">
              {filteredChapters.map((chapter) => (
                <div key={chapter.id} className="mb-2">
                  <div
                    className={`
                      flex items-center justify-between p-2 rounded-md cursor-pointer
                      hover:bg-muted/50 transition-colors
                      ${currentChapterId === chapter.id ? 'bg-muted' : ''}
                    `}
                  >
                    <div
                      className="flex items-center space-x-2 flex-1"
                      onClick={() => toggleChapter(chapter.id)}
                    >
                      {expandedChapters.has(chapter.id) ? (
                        <ChevronDown className="h-4 w-4" />
                      ) : (
                        <ChevronRight className="h-4 w-4" />
                      )}
                      <span
                        className="font-medium text-sm"
                        onClick={(e) => {
                          e.stopPropagation()
                          onChapterSelect(chapter.id)
                        }}
                      >
                        Chapter {chapter.number}: {chapter.title}
                        {chapter.number === 1 && (
                          <span className="ml-2 text-xs text-muted-foreground">(Default)</span>
                        )}
                      </span>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      <span className="text-xs text-muted-foreground">
                        {chapter.wordCount.toLocaleString()} words
                      </span>
                      <ChapterMenu
                        onEdit={() => onEditChapter(chapter.id)}
                        onDelete={() => onDeleteChapter(chapter.id)}
                        onAddScene={() => onAddScene(chapter.id)}
                        isFirstChapter={chapter.number === 1}
                      />
                    </div>
                  </div>
                  
                  {expandedChapters.has(chapter.id) && (
                    <div className="ml-6 mt-1">
                      {chapter.scenes.map((scene) => (
                        <div
                          key={scene.id}
                          className={`
                            flex items-center justify-between p-2 rounded-md cursor-pointer
                            hover:bg-muted/30 transition-colors
                            ${currentSceneId === scene.id ? 'bg-muted/50' : ''}
                          `}
                          onClick={() => onSceneSelect(chapter.id, scene.id)}
                        >
                          <div className="flex-1">
                            <div className="text-sm">
                              Scene {scene.number}
                              {scene.title && `: ${scene.title}`}
                            </div>
                            {scene.summary && (
                              <div className="text-xs text-muted-foreground line-clamp-1">
                                {scene.summary}
                              </div>
                            )}
                          </div>
                          
                          <div className="flex items-center space-x-2">
                            <span className="text-xs text-muted-foreground">
                              {scene.wordCount.toLocaleString()}
                            </span>
                            <SceneMenu
                              onEdit={() => onEditScene(chapter.id, scene.id)}
                              onDelete={() => onDeleteScene(chapter.id, scene.id)}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="p-4 border-t border-border">
        <button
          onClick={onAddChapter}
          className="w-full px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 flex items-center justify-center space-x-2"
        >
          <Plus className="h-4 w-4" />
          <span>Add Chapter</span>
        </button>
      </div>
    </div>
  )
}

// Chapter menu component
function ChapterMenu({ onEdit, onDelete, onAddScene, isFirstChapter = false }: {
  onEdit: () => void
  onDelete: () => void
  onAddScene: () => void
  isFirstChapter?: boolean
}) {
  const [showMenu, setShowMenu] = useState(false)

  return (
    <div className="relative">
      <button
        onClick={(e) => {
          e.stopPropagation()
          setShowMenu(!showMenu)
        }}
        className="p-1 rounded hover:bg-muted"
      >
        <MoreVertical className="h-4 w-4" />
      </button>
      
      {showMenu && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setShowMenu(false)}
          />
          <div className="absolute right-0 mt-1 w-40 bg-popover border border-border rounded-md shadow-lg z-20">
            <button
              onClick={(e) => {
                e.stopPropagation()
                onAddScene()
                setShowMenu(false)
              }}
              className="w-full px-3 py-2 text-left text-sm hover:bg-muted flex items-center space-x-2"
            >
              <Plus className="h-4 w-4" />
              <span>Add Scene</span>
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation()
                onEdit()
                setShowMenu(false)
              }}
              className="w-full px-3 py-2 text-left text-sm hover:bg-muted flex items-center space-x-2"
            >
              <Edit2 className="h-4 w-4" />
              <span>Edit Chapter</span>
            </button>
            {!isFirstChapter && (
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  onDelete()
                  setShowMenu(false)
                }}
                className="w-full px-3 py-2 text-left text-sm hover:bg-muted text-destructive flex items-center space-x-2"
              >
                <Trash2 className="h-4 w-4" />
                <span>Delete Chapter</span>
              </button>
            )}
          </div>
        </>
      )}
    </div>
  )
}

// Scene menu component
function SceneMenu({ onEdit, onDelete }: {
  onEdit: () => void
  onDelete: () => void
}) {
  const [showMenu, setShowMenu] = useState(false)

  return (
    <div className="relative">
      <button
        onClick={(e) => {
          e.stopPropagation()
          setShowMenu(!showMenu)
        }}
        className="p-1 rounded hover:bg-muted"
      >
        <MoreVertical className="h-4 w-4" />
      </button>
      
      {showMenu && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setShowMenu(false)}
          />
          <div className="absolute right-0 mt-1 w-40 bg-popover border border-border rounded-md shadow-lg z-20">
            <button
              onClick={(e) => {
                e.stopPropagation()
                onEdit()
                setShowMenu(false)
              }}
              className="w-full px-3 py-2 text-left text-sm hover:bg-muted flex items-center space-x-2"
            >
              <Edit2 className="h-4 w-4" />
              <span>Edit Scene</span>
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation()
                onDelete()
                setShowMenu(false)
              }}
              className="w-full px-3 py-2 text-left text-sm hover:bg-muted text-destructive flex items-center space-x-2"
            >
              <Trash2 className="h-4 w-4" />
              <span>Delete Scene</span>
            </button>
          </div>
        </>
      )}
    </div>
  )
}