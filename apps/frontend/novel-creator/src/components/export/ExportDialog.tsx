import { useState } from 'react'
import { X, FileText, Download } from 'lucide-react'
import { ExportService } from '@/services/export'
import { Document, Chapter } from '@/types/document'

interface ExportDialogProps {
  isOpen: boolean
  onClose: () => void
  document: Document
  chapters: Chapter[]
}

type ExportFormat = 'txt' | 'pdf' | 'docx' | 'epub'

interface ExportOptions {
  format: ExportFormat
  includeMetadata: boolean
  includeChapterNumbers: boolean
  includeSceneBreaks: boolean
}

export function ExportDialog({ isOpen, onClose, document, chapters }: ExportDialogProps) {
  const [options, setOptions] = useState<ExportOptions>({
    format: 'docx',
    includeMetadata: true,
    includeChapterNumbers: true,
    includeSceneBreaks: true,
  })
  const [isExporting, setIsExporting] = useState(false)

  if (!isOpen) return null

  const handleExport = async () => {
    setIsExporting(true)
    try {
      switch (options.format) {
        case 'txt':
          await ExportService.exportAsTxt(document, chapters, options)
          break
        case 'pdf':
          await ExportService.exportAsPdf(document, chapters, options)
          break
        case 'docx':
          await ExportService.exportAsDocx(document, chapters, options)
          break
        case 'epub':
          await ExportService.exportAsEpub(document, chapters)
          break
      }
      onClose()
    } catch (error) {
      console.error('Export failed:', error)
      // TODO: Show error toast
    } finally {
      setIsExporting(false)
    }
  }

  const formatInfo = {
    txt: {
      icon: FileText,
      name: 'Plain Text',
      description: 'Simple text file with basic formatting',
    },
    pdf: {
      icon: FileText,
      name: 'PDF',
      description: 'Portable document with preserved formatting',
    },
    docx: {
      icon: FileText,
      name: 'Word Document',
      description: 'Microsoft Word format for further editing',
    },
    epub: {
      icon: FileText,
      name: 'EPUB (Beta)',
      description: 'E-book format for e-readers (simplified)',
    },
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-2xl bg-background rounded-lg shadow-lg">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <h2 className="text-xl font-semibold">Export Document</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-muted rounded-md transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Format Selection */}
          <div>
            <h3 className="text-sm font-medium mb-3">Export Format</h3>
            <div className="grid grid-cols-2 gap-3">
              {(Object.entries(formatInfo) as [ExportFormat, typeof formatInfo['txt']][]).map(
                ([format, info]) => {
                  const Icon = info.icon
                  return (
                    <button
                      key={format}
                      onClick={() => setOptions({ ...options, format })}
                      className={`
                        p-4 rounded-lg border text-left transition-all
                        ${
                          options.format === format
                            ? 'border-primary bg-primary/10'
                            : 'border-border hover:border-muted-foreground'
                        }
                      `}
                    >
                      <div className="flex items-start space-x-3">
                        <Icon className="h-5 w-5 text-muted-foreground mt-0.5" />
                        <div>
                          <div className="font-medium">{info.name}</div>
                          <div className="text-sm text-muted-foreground mt-1">
                            {info.description}
                          </div>
                        </div>
                      </div>
                    </button>
                  )
                }
              )}
            </div>
          </div>

          {/* Options */}
          <div>
            <h3 className="text-sm font-medium mb-3">Export Options</h3>
            <div className="space-y-3">
              <label className="flex items-center space-x-3">
                <input
                  type="checkbox"
                  checked={options.includeMetadata}
                  onChange={(e) =>
                    setOptions({ ...options, includeMetadata: e.target.checked })
                  }
                  className="rounded border-input"
                />
                <span className="text-sm">Include document metadata (author, date, word count)</span>
              </label>
              <label className="flex items-center space-x-3">
                <input
                  type="checkbox"
                  checked={options.includeChapterNumbers}
                  onChange={(e) =>
                    setOptions({ ...options, includeChapterNumbers: e.target.checked })
                  }
                  className="rounded border-input"
                />
                <span className="text-sm">Include chapter numbers</span>
              </label>
              <label className="flex items-center space-x-3">
                <input
                  type="checkbox"
                  checked={options.includeSceneBreaks}
                  onChange={(e) =>
                    setOptions({ ...options, includeSceneBreaks: e.target.checked })
                  }
                  className="rounded border-input"
                />
                <span className="text-sm">Include scene break markers</span>
              </label>
            </div>
          </div>

          {/* Document Info */}
          <div className="p-4 bg-muted/50 rounded-lg">
            <div className="text-sm space-y-1">
              <div>
                <span className="text-muted-foreground">Title:</span> {document.title}
              </div>
              <div>
                <span className="text-muted-foreground">Chapters:</span> {chapters.length}
              </div>
              <div>
                <span className="text-muted-foreground">Word Count:</span>{' '}
                {document.wordCount.toLocaleString()}
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 p-6 border-t border-border">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium border border-input rounded-md hover:bg-muted transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleExport}
            disabled={isExporting}
            className="px-4 py-2 text-sm font-medium bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50 transition-colors flex items-center space-x-2"
          >
            <Download className="h-4 w-4" />
            <span>{isExporting ? 'Exporting...' : 'Export'}</span>
          </button>
        </div>
      </div>
    </div>
  )
}