/* eslint-disable @typescript-eslint/no-explicit-any */
declare module 'monaco-editor' {
  export namespace editor {
    export interface IStandaloneCodeEditor {
      updateOptions(options: any): void
      getValue(): string
      setValue(value: string): void
      getSelection(): any
      onDidChangeModelContent(listener: () => void): void
      onDidChangeCursorSelection(listener: (e: any) => void): void
      getModel(): any
      addCommand(keybinding: number, handler: () => void): void
      executeEdits(source: string, edits: any[]): void
    }
    
    export interface IEditorOptions {
      wordWrap?: 'on' | 'off' | 'wordWrapColumn' | 'bounded'
      fontSize?: number
      lineNumbers?: 'on' | 'off' | 'relative' | 'interval'
      minimap?: {
        enabled?: boolean
      }
      scrollBeyondLastLine?: boolean
      automaticLayout?: boolean
      padding?: {
        top?: number
        bottom?: number
      }
      renderWhitespace?: 'none' | 'boundary' | 'selection' | 'trailing' | 'all'
      renderLineHighlight?: 'none' | 'gutter' | 'line' | 'all'
      occurrencesHighlight?: 'off' | 'singleFile' | 'multiFile'
      selectionHighlight?: boolean
      scrollbar?: {
        verticalScrollbarSize?: number
        horizontalScrollbarSize?: number
      }
      suggest?: {
        showWords?: boolean
        showSnippets?: boolean
      }
      quickSuggestions?: {
        other?: boolean
        comments?: boolean
        strings?: boolean
      }
    }
    
    export function defineTheme(name: string, theme: any): void
  }
}