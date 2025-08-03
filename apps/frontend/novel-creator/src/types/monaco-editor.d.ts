declare module 'monaco-editor' {
  export namespace editor {
    export interface ITextModel {
      getValue(): string
      getValueInRange(range: IRange): string
    }

    export interface IRange {
      startLineNumber: number
      startColumn: number
      endLineNumber: number
      endColumn: number
    }

    export interface ISelection extends IRange {
      selectionStartLineNumber: number
      selectionStartColumn: number
      positionLineNumber: number
      positionColumn: number
    }

    export interface ICursorSelectionChangedEvent {
      selection: ISelection
      secondarySelections: ISelection[]
      source: string
      reason: number
    }

    export interface IIdentifiedSingleEditOperation {
      range: IRange
      text: string
      forceMoveMarkers?: boolean
    }

    export interface IStandaloneCodeEditor {
      updateOptions(options: IEditorOptions): void
      getValue(): string
      setValue(value: string): void
      getSelection(): ISelection
      onDidChangeModelContent(listener: () => void): void
      onDidChangeCursorSelection(listener: (e: ICursorSelectionChangedEvent) => void): void
      getModel(): ITextModel | null
      addCommand(keybinding: number, handler: () => void): string | null
      executeEdits(source: string, edits: IIdentifiedSingleEditOperation[]): void
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

    export interface ITokenThemeRule {
      token: string
      foreground?: string
      background?: string
      fontStyle?: string
    }

    export interface IThemeData {
      base: 'vs' | 'vs-dark' | 'hc-black'
      inherit: boolean
      rules: ITokenThemeRule[]
      colors: Record<string, string>
    }
    
    export function defineTheme(name: string, theme: IThemeData): void
  }

  export enum KeyMod {
    None = 0,
    CtrlCmd = 2048,
    Shift = 1024,
    Alt = 512,
    WinCtrl = 256,
  }

  export enum KeyCode {
    KeyB = 32,
    KeyI = 39,
  }
}