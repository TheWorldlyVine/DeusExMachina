import { useEffect, useRef, useState, useCallback } from 'react'
import { VariableSizeList as List } from 'react-window'
import AutoSizer from 'react-virtualized-auto-sizer'
import { debounce } from 'lodash'

interface Chapter {
  id: string
  title: string
  content: string
  wordCount: number
  scenes: Scene[]
}

interface Scene {
  id: string
  content: string
  wordCount: number
}

interface VirtualizedDocumentViewerProps {
  chapters: Chapter[]
  onChapterClick?: (chapterId: string) => void
  onSceneClick?: (chapterId: string, sceneId: string) => void
  searchQuery?: string
  className?: string
}

interface ItemData {
  type: 'chapter-header' | 'scene'
  chapterId: string
  sceneId?: string
  content: string
  title?: string
  index: number
}

const ESTIMATED_LINE_HEIGHT = 24
const CHAPTER_HEADER_HEIGHT = 80
const SCENE_PADDING = 40
const WORDS_PER_LINE = 12

export function VirtualizedDocumentViewer({
  chapters,
  onChapterClick,
  onSceneClick,
  searchQuery,
  className = '',
}: VirtualizedDocumentViewerProps) {
  const listRef = useRef<List>(null)
  const itemHeights = useRef<{ [key: number]: number }>({})
  const [items, setItems] = useState<ItemData[]>([])
  const [highlightedIndex, setHighlightedIndex] = useState<number>(-1)

  // Build flat list of items from chapters and scenes
  useEffect(() => {
    const flatItems: ItemData[] = []
    let itemIndex = 0

    chapters.forEach((chapter) => {
      // Add chapter header
      flatItems.push({
        type: 'chapter-header',
        chapterId: chapter.id,
        content: chapter.content,
        title: chapter.title,
        index: itemIndex++,
      })

      // Add scenes
      chapter.scenes.forEach((scene) => {
        flatItems.push({
          type: 'scene',
          chapterId: chapter.id,
          sceneId: scene.id,
          content: scene.content,
          index: itemIndex++,
        })
      })
    })

    setItems(flatItems)
  }, [chapters])

  // Calculate item height based on content
  const getItemSize = useCallback((index: number) => {
    if (itemHeights.current[index]) {
      return itemHeights.current[index]
    }

    const item = items[index]
    if (!item) return CHAPTER_HEADER_HEIGHT

    if (item.type === 'chapter-header') {
      return CHAPTER_HEADER_HEIGHT
    }

    // Estimate height based on word count
    const wordCount = item.content.split(/\s+/).length
    const estimatedLines = Math.ceil(wordCount / WORDS_PER_LINE)
    const estimatedHeight = estimatedLines * ESTIMATED_LINE_HEIGHT + SCENE_PADDING

    return estimatedHeight
  }, [items])

  // Handle item measurement
  const setItemSize = useCallback((index: number, size: number) => {
    itemHeights.current[index] = size
    if (listRef.current) {
      listRef.current.resetAfterIndex(index)
    }
  }, [])

  // Search functionality
  useEffect(() => {
    if (!searchQuery) {
      setHighlightedIndex(-1)
      return
    }

    const query = searchQuery.toLowerCase()
    const foundIndex = items.findIndex(
      item => item.content.toLowerCase().includes(query)
    )

    if (foundIndex !== -1) {
      setHighlightedIndex(foundIndex)
      listRef.current?.scrollToItem(foundIndex, 'center')
    }
  }, [searchQuery, items])

  // Render individual item
  const renderItem = ({ index, style }: { index: number; style: React.CSSProperties }) => {
    const item = items[index]
    if (!item) return null

    const isHighlighted = index === highlightedIndex

    return (
      <div
        style={style}
        className={`px-6 ${isHighlighted ? 'bg-yellow-100 dark:bg-yellow-900/30' : ''}`}
      >
        <ItemMeasurer
          index={index}
          onMeasure={setItemSize}
        >
          {item.type === 'chapter-header' ? (
            <div
              className="py-4 cursor-pointer hover:bg-muted/50 transition-colors"
              onClick={() => onChapterClick?.(item.chapterId)}
            >
              <h2 className="text-2xl font-bold mb-2">{item.title}</h2>
              <div className="text-sm text-muted-foreground">
                {item.content.split(/\s+/).length} words
              </div>
            </div>
          ) : (
            <div
              className="py-4 pl-8 cursor-pointer hover:bg-muted/30 transition-colors border-l-2 border-border"
              onClick={() => onSceneClick?.(item.chapterId, item.sceneId!)}
            >
              <div className="prose prose-sm dark:prose-invert max-w-none">
                <p className="line-clamp-3">{item.content}</p>
              </div>
              <div className="text-xs text-muted-foreground mt-2">
                {item.content.split(/\s+/).length} words
              </div>
            </div>
          )}
        </ItemMeasurer>
      </div>
    )
  }

  return (
    <div className={`h-full ${className}`}>
      <AutoSizer>
        {({ height, width }) => (
          <List
            ref={listRef}
            height={height}
            itemCount={items.length}
            itemSize={getItemSize}
            width={width}
            overscanCount={5}
            estimatedItemSize={ESTIMATED_LINE_HEIGHT * 10}
          >
            {renderItem}
          </List>
        )}
      </AutoSizer>
    </div>
  )
}

// Component to measure item height after render
interface ItemMeasurerProps {
  index: number
  onMeasure: (index: number, height: number) => void
  children: React.ReactNode
}

function ItemMeasurer({ index, onMeasure, children }: ItemMeasurerProps) {
  const elementRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (elementRef.current) {
      const height = elementRef.current.getBoundingClientRect().height
      onMeasure(index, height)
    }
  }, [index, onMeasure, children])

  return <div ref={elementRef}>{children}</div>
}