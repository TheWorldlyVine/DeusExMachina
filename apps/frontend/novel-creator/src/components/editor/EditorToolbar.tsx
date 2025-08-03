import { useState } from 'react'
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
} from 'lucide-react'

interface EditorToolbarProps {
  onCommand: (command: string, value?: string | number) => void
  onThemeToggle: () => void
  onFullscreenToggle: () => void
  onExport: (format: 'pdf' | 'docx' | 'txt') => void
  isFullscreen: boolean
  isDarkMode: boolean
  wordCount: number
  characterCount: number
}

export function EditorToolbar({
  onCommand,
  onThemeToggle,
  onFullscreenToggle,
  onExport,
  isFullscreen,
  isDarkMode,
  wordCount,
  characterCount,
}: EditorToolbarProps) {
  const [showStats, setShowStats] = useState(true)
  const [showExportMenu, setShowExportMenu] = useState(false)

  const formatButtons = [
    { icon: Bold, command: 'bold', title: 'Bold (Ctrl+B)' },
    { icon: Italic, command: 'italic', title: 'Italic (Ctrl+I)' },
    { separator: true },
    { icon: Heading1, command: 'heading1', title: 'Heading 1' },
    { icon: Heading2, command: 'heading2', title: 'Heading 2' },
    { icon: Heading3, command: 'heading3', title: 'Heading 3' },
    { separator: true },
    { icon: List, command: 'bulletList', title: 'Bullet List' },
    { icon: ListOrdered, command: 'orderedList', title: 'Ordered List' },
    { icon: Quote, command: 'blockquote', title: 'Blockquote' },
    { icon: Minus, command: 'horizontalRule', title: 'Horizontal Rule' },
  ]

  return (
    <div className="border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="flex items-center justify-between px-4 py-2">
        <div className="flex items-center space-x-1">
          {formatButtons.map((button, index) => {
            if (button.separator) {
              return <div key={index} className="w-px h-6 bg-border mx-1" />
            }
            const Icon = button.icon
            return (
              <button
                key={button.command}
                onClick={() => onCommand(button.command!)}
                className="p-2 rounded hover:bg-muted transition-colors"
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

          <div className="relative">
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

          <button
            onClick={() => onCommand('settings')}
            className="p-2 rounded hover:bg-muted transition-colors"
            title="Editor settings"
          >
            <Settings className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  )
}