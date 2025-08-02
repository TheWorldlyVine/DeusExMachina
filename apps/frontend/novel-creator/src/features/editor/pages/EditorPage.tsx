import { useParams } from 'react-router-dom'

export function EditorPage() {
  const { documentId } = useParams()
  
  return (
    <div className="h-full flex">
      <div className="flex-1 bg-background">
        <div className="editor-container">
          <div className="editor-content">
            <h1 className="text-2xl font-bold mb-4">Document Editor</h1>
            <p className="text-muted-foreground">Editing document: {documentId}</p>
            <div className="mt-8">
              <textarea 
                className="w-full h-96 p-4 border border-input rounded-md resize-none focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="Start writing your story..."
              />
            </div>
          </div>
        </div>
      </div>
      <div className="w-80 bg-muted/30 border-l border-border p-4">
        <h2 className="text-lg font-semibold mb-4">AI Assistant</h2>
        <button className="w-full px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 mb-2">
          Generate Scene
        </button>
        <button className="w-full px-4 py-2 bg-secondary text-secondary-foreground rounded-md hover:bg-secondary/90">
          Continue Writing
        </button>
      </div>
    </div>
  )
}