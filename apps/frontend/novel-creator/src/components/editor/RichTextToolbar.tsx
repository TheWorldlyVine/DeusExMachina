import { useState, useEffect } from 'react'
import { Editor } from '@tiptap/react'
import { 
  Bold, 
  Italic, 
  List, 
  ListOrdered, 
  Quote,
  Heading1,
  Heading2,
  Heading3,
  Minus,
  Eye,
  EyeOff,
  FileText,
  Download,
  Settings,
  Maximize2,
  Minimize2,
  Sun,
  Moon,
  Underline,
  Palette,
  AlignLeft,
  AlignCenter,  
  AlignRight,
  AlignJustify,
  Undo,
  Redo,
} from 'lucide-react'

interface RichTextToolbarProps {
  editor: Editor | null
  onThemeToggle: () => void
  onFullscreenToggle: () => void
  onExport: (format: 'pdf' | 'docx' | 'txt') => void
  isFullscreen: boolean
  isDarkMode: boolean
  wordCount: number
  characterCount: number
  onSettingsClick?: () => void
}

export function RichTextToolbar({
  editor,
  onThemeToggle,
  onFullscreenToggle,
  onExport,
  isFullscreen,
  isDarkMode,
  wordCount,
  characterCount,
  onSettingsClick,
}: RichTextToolbarProps) {
  const [showStats, setShowStats] = useState(true)
  const [showExportMenu, setShowExportMenu] = useState(false)

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (showExportMenu && !(event.target as Element).closest('.export-menu-container')) {
        setShowExportMenu(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [showExportMenu])

  if (!editor) {
    return null
  }

  const formatButtons = [
    { 
      icon: Bold, 
      action: () => editor.chain().focus().toggleBold().run(),
      isActive: editor.isActive('bold'),
      title: 'Bold (Ctrl+B)' 
    },
    { 
      icon: Italic, 
      action: () => editor.chain().focus().toggleItalic().run(),
      isActive: editor.isActive('italic'),
      title: 'Italic (Ctrl+I)' 
    },
    { 
      icon: Underline, 
      action: () => editor.chain().focus().toggleUnderline().run(),
      isActive: editor.isActive('underline'),
      title: 'Underline (Ctrl+U)' 
    },
    { 
      icon: Palette, 
      action: () => editor.chain().focus().toggleHighlight().run(),
      isActive: editor.isActive('highlight'),
      title: 'Highlight' 
    },
    { separator: true },
    { 
      icon: Heading1, 
      action: () => editor.chain().focus().toggleHeading({ level: 1 }).run(),
      isActive: editor.isActive('heading', { level: 1 }),
      title: 'Heading 1' 
    },
    { 
      icon: Heading2, 
      action: () => editor.chain().focus().toggleHeading({ level: 2 }).run(),
      isActive: editor.isActive('heading', { level: 2 }),
      title: 'Heading 2' 
    },
    { 
      icon: Heading3, 
      action: () => editor.chain().focus().toggleHeading({ level: 3 }).run(),
      isActive: editor.isActive('heading', { level: 3 }),
      title: 'Heading 3' 
    },
    { separator: true },
    { 
      icon: List, 
      action: () => editor.chain().focus().toggleBulletList().run(),
      isActive: editor.isActive('bulletList'),
      title: 'Bullet List' 
    },
    { 
      icon: ListOrdered, 
      action: () => editor.chain().focus().toggleOrderedList().run(),
      isActive: editor.isActive('orderedList'),
      title: 'Ordered List' 
    },
    { 
      icon: Quote, 
      action: () => editor.chain().focus().toggleBlockquote().run(),
      isActive: editor.isActive('blockquote'),
      title: 'Blockquote' 
    },
    { 
      icon: Minus, 
      action: () => editor.chain().focus().setHorizontalRule().run(),
      isActive: false,
      title: 'Horizontal Rule' 
    },
    { separator: true },
    { 
      icon: AlignLeft, 
      action: () => editor.chain().focus().setTextAlign('left').run(),
      isActive: editor.isActive({ textAlign: 'left' }),
      title: 'Align Left' 
    },
    { 
      icon: AlignCenter, 
      action: () => editor.chain().focus().setTextAlign('center').run(),
      isActive: editor.isActive({ textAlign: 'center' }),
      title: 'Align Center' 
    },
    { 
      icon: AlignRight, 
      action: () => editor.chain().focus().setTextAlign('right').run(),
      isActive: editor.isActive({ textAlign: 'right' }),
      title: 'Align Right' 
    },
    { 
      icon: AlignJustify, 
      action: () => editor.chain().focus().setTextAlign('justify').run(),
      isActive: editor.isActive({ textAlign: 'justify' }),
      title: 'Align Justify' 
    },
    { separator: true },
    { 
      icon: Undo, 
      action: () => editor.chain().focus().undo().run(),
      isActive: false,
      disabled: !editor.can().undo(),
      title: 'Undo (Ctrl+Z)' 
    },
    { 
      icon: Redo, 
      action: () => editor.chain().focus().redo().run(),
      isActive: false,
      disabled: !editor.can().redo(),
      title: 'Redo (Ctrl+Y)' 
    },
  ]

  return (
    <div className="border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 sticky top-0 z-10">
      <div className="flex items-center justify-between px-4 py-2">
        <div className="flex items-center space-x-1 flex-wrap">
          {formatButtons.map((button, index) => {
            if (button.separator) {
              return <div key={index} className="w-px h-6 bg-border mx-1" />
            }
            const Icon = button.icon!
            return (
              <button
                key={button.title}
                onClick={button.action}
                disabled={button.disabled}
                className={`p-2 rounded transition-colors ${
                  button.isActive 
                    ? 'bg-primary text-primary-foreground' 
                    : 'hover:bg-muted'
                } ${button.disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
                title={button.title}
              >
                <Icon className="h-4 w-4" />
              </button>
            )
          })}
        </div>

        <div className="flex items-center space-x-2">
          {showStats && (
            <div className="text-sm text-muted-foreground px-3">
              <span>{wordCount.toLocaleString()} words</span>
              <span className="mx-2">•</span>
              <span>{characterCount.toLocaleString()} characters</span>
            </div>
          )}

          <button
            onClick={() => setShowStats(!showStats)}
            className="p-2 rounded hover:bg-muted transition-colors"
            title={showStats ? 'Hide stats' : 'Show stats'}
          >
            {showStats ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>

          <div className="relative export-menu-container">
            <button
              onClick={() => setShowExportMenu(!showExportMenu)}
              className="p-2 rounded hover:bg-muted transition-colors"
              title="Export document"
            >
              <Download className="h-4 w-4" />
            </button>
            
            {showExportMenu && (
              <div className="absolute right-0 mt-2 w-48 bg-popover border border-border rounded-md shadow-lg z-50">
                <button
                  onClick={() => {
                    onExport('pdf')
                    setShowExportMenu(false)
                  }}
                  className="w-full px-4 py-2 text-left hover:bg-muted transition-colors flex items-center space-x-2"
                >
                  <FileText className="h-4 w-4" />
                  <span>Export as PDF</span>
                </button>
                <button
                  onClick={() => {
                    onExport('docx')
                    setShowExportMenu(false)
                  }}
                  className="w-full px-4 py-2 text-left hover:bg-muted transition-colors flex items-center space-x-2"
                >
                  <FileText className="h-4 w-4" />
                  <span>Export as DOCX</span>
                </button>
                <button
                  onClick={() => {
                    onExport('txt')
                    setShowExportMenu(false)
                  }}
                  className="w-full px-4 py-2 text-left hover:bg-muted transition-colors flex items-center space-x-2"
                >
                  <FileText className="h-4 w-4" />
                  <span>Export as TXT</span>
                </button>
              </div>
            )}
          </div>

          <button
            onClick={onThemeToggle}
            className="p-2 rounded hover:bg-muted transition-colors"
            title={isDarkMode ? 'Light mode' : 'Dark mode'}
          >
            {isDarkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>

          <button
            onClick={onFullscreenToggle}
            className="p-2 rounded hover:bg-muted transition-colors"
            title={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
          >
            {isFullscreen ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
          </button>

          {onSettingsClick && (
            <button
              onClick={onSettingsClick}
              className="p-2 rounded hover:bg-muted transition-colors"
              title="Editor settings"
            >
              <Settings className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}