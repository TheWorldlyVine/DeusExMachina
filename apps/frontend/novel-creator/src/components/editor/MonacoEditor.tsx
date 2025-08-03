import { useRef, useEffect, useState } from 'react'
import Editor, { OnMount, Monaco } from '@monaco-editor/react'
import { editor } from 'monaco-editor'

interface MonacoEditorProps {
  value: string
  onChange: (value: string) => void
  onSelectionChange?: (selection: string) => void
  disabled?: boolean
  theme?: 'light' | 'dark'
  language?: string
  wordWrap?: 'on' | 'off' | 'wordWrapColumn' | 'bounded'
  fontSize?: number
  lineNumbers?: 'on' | 'off' | 'relative' | 'interval'
  minimap?: boolean
  scrollBeyondLastLine?: boolean
  automaticLayout?: boolean
}

export function MonacoEditor({
  value,
  onChange,
  onSelectionChange,
  disabled = false,
  theme = 'light',
  language = 'markdown',
  wordWrap = 'on',
  fontSize = 14,
  lineNumbers = 'off',
  minimap = false,
  scrollBeyondLastLine = false,
  automaticLayout = true,
}: MonacoEditorProps) {
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null)
  const monacoRef = useRef<Monaco | null>(null)
  const [isReady, setIsReady] = useState(false)

  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor
    monacoRef.current = monaco
    setIsReady(true)

    // Configure editor for novel writing
    editor.updateOptions({
      wordWrap,
      fontSize,
      lineNumbers,
      minimap: {
        enabled: minimap,
      },
      scrollBeyondLastLine,
      automaticLayout,
      padding: {
        top: 20,
        bottom: 20,
      },
      renderWhitespace: 'none',
      renderLineHighlight: 'none',
      occurrencesHighlight: 'off',
      selectionHighlight: true,
      scrollbar: {
        verticalScrollbarSize: 10,
        horizontalScrollbarSize: 10,
      },
      suggest: {
        showWords: true,
        showSnippets: false,
      },
      quickSuggestions: {
        other: true,
        comments: false,
        strings: true,
      },
    })

    // Define custom theme for novel writing
    monaco.editor.defineTheme('novel-light', {
      base: 'vs',
      inherit: true,
      rules: [
        { token: 'comment', foreground: '6a737d' },
        { token: 'string', foreground: '032f62' },
        { token: 'keyword', foreground: 'd73a49' },
      ],
      colors: {
        'editor.background': '#ffffff',
        'editor.foreground': '#24292e',
        'editor.lineHighlightBackground': '#f6f8fa',
        'editorCursor.foreground': '#24292e',
        'editor.selectionBackground': '#c8d1e0',
        'editor.inactiveSelectionBackground': '#e1e4e8',
      },
    })

    monaco.editor.defineTheme('novel-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'comment', foreground: '8b949e' },
        { token: 'string', foreground: '79c0ff' },
        { token: 'keyword', foreground: 'ff7b72' },
      ],
      colors: {
        'editor.background': '#0d1117',
        'editor.foreground': '#c9d1d9',
        'editor.lineHighlightBackground': '#161b22',
        'editorCursor.foreground': '#c9d1d9',
        'editor.selectionBackground': '#264f78',
        'editor.inactiveSelectionBackground': '#264f7888',
      },
    })

    // Set theme
    monaco.editor.setTheme(theme === 'dark' ? 'novel-dark' : 'novel-light')

    // Handle selection changes
    editor.onDidChangeCursorSelection((e) => {
      if (onSelectionChange) {
        const selection = editor.getModel()?.getValueInRange(e.selection) || ''
        onSelectionChange(selection)
      }
    })

    // Add custom keybindings
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyB, () => {
      // Bold text (Markdown)
      const selection = editor.getSelection()
      if (selection) {
        const selectedText = editor.getModel()?.getValueInRange(selection) || ''
        editor.executeEdits('', [
          {
            range: selection,
            text: `**${selectedText}**`,
          },
        ])
      }
    })

    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyI, () => {
      // Italic text (Markdown)
      const selection = editor.getSelection()
      if (selection) {
        const selectedText = editor.getModel()?.getValueInRange(selection) || ''
        editor.executeEdits('', [
          {
            range: selection,
            text: `*${selectedText}*`,
          },
        ])
      }
    })
  }

  // Update theme when prop changes
  useEffect(() => {
    if (monacoRef.current && isReady) {
      monacoRef.current.editor.setTheme(theme === 'dark' ? 'novel-dark' : 'novel-light')
    }
  }, [theme, isReady])

  // Update word wrap when prop changes
  useEffect(() => {
    if (editorRef.current && isReady) {
      editorRef.current.updateOptions({ wordWrap })
    }
  }, [wordWrap, isReady])

  return (
    <div className="h-full w-full">
      <Editor
        height="100%"
        defaultLanguage={language}
        value={value}
        onChange={(value) => onChange(value || '')}
        onMount={handleEditorDidMount}
        options={{
          readOnly: disabled,
          domReadOnly: disabled,
          cursorStyle: 'line',
          contextmenu: true,
          formatOnPaste: false,
          formatOnType: false,
          autoClosingBrackets: 'never',
          autoClosingQuotes: 'never',
          autoSurround: 'never',
          links: false,
        }}
        loading={
          <div className="flex items-center justify-center h-full">
            <div className="text-muted-foreground">Loading editor...</div>
          </div>
        }
      />
    </div>
  )
}