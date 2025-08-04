import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import Highlight from '@tiptap/extension-highlight'
import CharacterCount from '@tiptap/extension-character-count'
import Typography from '@tiptap/extension-typography'
import { useEffect } from 'react'

interface RichTextEditorProps {
  value: string
  onChange: (value: string) => void
  onSelectionChange?: (selection: string) => void
  disabled?: boolean
  placeholder?: string
  className?: string
  onCharacterCountChange?: (count: { characters: number; words: number }) => void
}

export function RichTextEditor({
  value,
  onChange,
  onSelectionChange,
  disabled = false,
  placeholder = 'Start writing your novel...',
  className = '',
  onCharacterCountChange,
}: RichTextEditorProps) {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: {
          levels: [1, 2, 3],
        },
      }),
      Placeholder.configure({
        placeholder,
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
    content: value,
    editable: !disabled,
    onUpdate: ({ editor }) => {
      const html = editor.getHTML()
      onChange(html)
    },
    onSelectionUpdate: ({ editor }) => {
      if (onSelectionChange) {
        const { from, to } = editor.state.selection
        const selectedText = editor.state.doc.textBetween(from, to)
        onSelectionChange(selectedText)
      }
    },
    editorProps: {
      attributes: {
        class: `prose prose-lg max-w-none focus:outline-none min-h-full p-8 ${className}`,
      },
    },
  })

  // Update editor content when value prop changes externally
  useEffect(() => {
    if (editor && value !== editor.getHTML()) {
      editor.commands.setContent(value)
    }
  }, [value, editor])

  // Update character count
  useEffect(() => {
    if (editor && onCharacterCountChange) {
      const updateCount = () => {
        const characters = editor.storage.characterCount.characters()
        const words = editor.storage.characterCount.words()
        onCharacterCountChange({ characters, words })
      }

      updateCount()
      editor.on('update', updateCount)

      return () => {
        editor.off('update', updateCount)
      }
    }
  }, [editor, onCharacterCountChange])

  // Expose editor instance methods via ref if needed
  useEffect(() => {
    if (editor) {
      // Store editor instance for toolbar commands
      // Using a global variable is not ideal but avoids prop drilling
      // In production, consider using context or a state management solution
      ;(window as Window & { __tiptapEditor?: typeof editor }).__tiptapEditor = editor
    }

    return () => {
      const win = window as Window & { __tiptapEditor?: typeof editor }
      if (win.__tiptapEditor === editor) {
        delete win.__tiptapEditor
      }
    }
  }, [editor])

  return (
    <div className="h-full w-full overflow-auto">
      <EditorContent 
        editor={editor} 
        className="h-full"
      />
    </div>
  )
}