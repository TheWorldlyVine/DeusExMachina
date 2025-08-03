import { useState } from 'react'
import { 
  Sparkles, 
  BookOpen, 
  Users, 
  MapPin, 
  Lightbulb,
  PenTool,
  RefreshCw,
  ChevronRight,
  Settings2,
  Brain,
  Target,
} from 'lucide-react'

interface AIAssistantPanelProps {
  selectedText: string
  isGenerating: boolean
  onGenerateScene: (options?: Record<string, unknown>) => void
  onContinueWriting: (options?: Record<string, unknown>) => void
  onGenerateIdeas: (type: string) => void
  onAnalyzeText: () => void
  characters: Array<{ characterId: string; name: string; role: string }>
  plots: Array<{ plotId: string; title: string; status: string }>
  locations: Array<{ locationId: string; name: string; type: string }>
}

export function AIAssistantPanel({
  selectedText,
  isGenerating,
  onGenerateScene,
  onContinueWriting,
  onGenerateIdeas,
  onAnalyzeText,
  characters = [],
  plots = [],
  locations = [],
}: AIAssistantPanelProps) {
  const [expandedSection, setExpandedSection] = useState<string | null>('quick-actions')
  const [generationOptions, setGenerationOptions] = useState({
    tone: 'neutral',
    length: 'medium',
    style: 'descriptive',
  })

  const toggleSection = (section: string) => {
    setExpandedSection(expandedSection === section ? null : section)
  }

  return (
    <div className="h-full flex flex-col bg-muted/30 border-l border-border">
      <div className="p-4 border-b border-border">
        <h2 className="text-lg font-semibold flex items-center space-x-2">
          <Sparkles className="h-5 w-5 text-primary" />
          <span>AI Assistant</span>
        </h2>
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* Quick Actions */}
        <div className="border-b border-border">
          <button
            onClick={() => toggleSection('quick-actions')}
            className="w-full px-4 py-3 flex items-center justify-between hover:bg-muted/50 transition-colors"
          >
            <span className="font-medium">Quick Actions</span>
            <ChevronRight 
              className={`h-4 w-4 transition-transform ${
                expandedSection === 'quick-actions' ? 'rotate-90' : ''
              }`} 
            />
          </button>
          
          {expandedSection === 'quick-actions' && (
            <div className="px-4 pb-4 space-y-2">
              <button
                onClick={() => onGenerateScene()}
                disabled={isGenerating}
                className="w-full px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
              >
                <BookOpen className="h-4 w-4" />
                <span>{isGenerating ? 'Generating...' : 'Generate Scene'}</span>
              </button>
              
              <button
                onClick={() => onContinueWriting()}
                disabled={isGenerating}
                className="w-full px-4 py-2 bg-secondary text-secondary-foreground rounded-md hover:bg-secondary/90 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
              >
                <PenTool className="h-4 w-4" />
                <span>{isGenerating ? 'Generating...' : 'Continue Writing'}</span>
              </button>
              
              <button
                onClick={() => onGenerateIdeas('plot')}
                disabled={isGenerating}
                className="w-full px-4 py-2 border border-border rounded-md hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
              >
                <Lightbulb className="h-4 w-4" />
                <span>Generate Ideas</span>
              </button>
              
              <button
                onClick={onAnalyzeText}
                disabled={isGenerating || !selectedText}
                className="w-full px-4 py-2 border border-border rounded-md hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
              >
                <Brain className="h-4 w-4" />
                <span>Analyze Selection</span>
              </button>
            </div>
          )}
        </div>

        {/* Generation Options */}
        <div className="border-b border-border">
          <button
            onClick={() => toggleSection('options')}
            className="w-full px-4 py-3 flex items-center justify-between hover:bg-muted/50 transition-colors"
          >
            <span className="font-medium">Generation Options</span>
            <ChevronRight 
              className={`h-4 w-4 transition-transform ${
                expandedSection === 'options' ? 'rotate-90' : ''
              }`} 
            />
          </button>
          
          {expandedSection === 'options' && (
            <div className="px-4 pb-4 space-y-3">
              <div>
                <label className="text-sm font-medium mb-1 block">Tone</label>
                <select 
                  value={generationOptions.tone}
                  onChange={(e) => setGenerationOptions({ ...generationOptions, tone: e.target.value })}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                >
                  <option value="neutral">Neutral</option>
                  <option value="dramatic">Dramatic</option>
                  <option value="humorous">Humorous</option>
                  <option value="dark">Dark</option>
                  <option value="romantic">Romantic</option>
                </select>
              </div>
              
              <div>
                <label className="text-sm font-medium mb-1 block">Length</label>
                <select 
                  value={generationOptions.length}
                  onChange={(e) => setGenerationOptions({ ...generationOptions, length: e.target.value })}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                >
                  <option value="short">Short (100-200 words)</option>
                  <option value="medium">Medium (300-500 words)</option>
                  <option value="long">Long (800-1000 words)</option>
                </select>
              </div>
              
              <div>
                <label className="text-sm font-medium mb-1 block">Style</label>
                <select 
                  value={generationOptions.style}
                  onChange={(e) => setGenerationOptions({ ...generationOptions, style: e.target.value })}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                >
                  <option value="descriptive">Descriptive</option>
                  <option value="dialogue-heavy">Dialogue Heavy</option>
                  <option value="action-packed">Action Packed</option>
                  <option value="introspective">Introspective</option>
                </select>
              </div>
            </div>
          )}
        </div>

        {/* Story Elements */}
        <div className="border-b border-border">
          <button
            onClick={() => toggleSection('elements')}
            className="w-full px-4 py-3 flex items-center justify-between hover:bg-muted/50 transition-colors"
          >
            <span className="font-medium">Story Elements</span>
            <ChevronRight 
              className={`h-4 w-4 transition-transform ${
                expandedSection === 'elements' ? 'rotate-90' : ''
              }`} 
            />
          </button>
          
          {expandedSection === 'elements' && (
            <div className="px-4 pb-4 space-y-3">
              {/* Characters */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium flex items-center space-x-1">
                    <Users className="h-4 w-4" />
                    <span>Characters ({characters.length})</span>
                  </span>
                  <button className="text-xs text-primary hover:underline">Add</button>
                </div>
                <div className="space-y-1 max-h-32 overflow-y-auto">
                  {characters.length > 0 ? (
                    characters.map((char) => (
                      <div key={char.characterId} className="text-sm p-2 bg-muted rounded-md">
                        {char.name}
                      </div>
                    ))
                  ) : (
                    <p className="text-sm text-muted-foreground">No characters yet</p>
                  )}
                </div>
              </div>

              {/* Plot Threads */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium flex items-center space-x-1">
                    <Target className="h-4 w-4" />
                    <span>Plot Threads ({plots.length})</span>
                  </span>
                  <button className="text-xs text-primary hover:underline">Add</button>
                </div>
                <div className="space-y-1 max-h-32 overflow-y-auto">
                  {plots.length > 0 ? (
                    plots.map((plot) => (
                      <div key={plot.plotId} className="text-sm p-2 bg-muted rounded-md">
                        {plot.title}
                      </div>
                    ))
                  ) : (
                    <p className="text-sm text-muted-foreground">No plot threads yet</p>
                  )}
                </div>
              </div>

              {/* Locations */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium flex items-center space-x-1">
                    <MapPin className="h-4 w-4" />
                    <span>Locations ({locations.length})</span>
                  </span>
                  <button className="text-xs text-primary hover:underline">Add</button>
                </div>
                <div className="space-y-1 max-h-32 overflow-y-auto">
                  {locations.length > 0 ? (
                    locations.map((loc) => (
                      <div key={loc.locationId} className="text-sm p-2 bg-muted rounded-md">
                        {loc.name}
                      </div>
                    ))
                  ) : (
                    <p className="text-sm text-muted-foreground">No locations yet</p>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Selected Text */}
        {selectedText && (
          <div className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">Selected Text</span>
              <button
                onClick={() => onGenerateIdeas('selection')}
                className="text-xs text-primary hover:underline flex items-center space-x-1"
              >
                <RefreshCw className="h-3 w-3" />
                <span>Improve</span>
              </button>
            </div>
            <div className="p-3 bg-muted rounded-md">
              <p className="text-sm line-clamp-4">{selectedText}</p>
            </div>
          </div>
        )}
      </div>

      {/* Settings */}
      <div className="p-4 border-t border-border">
        <button className="w-full px-4 py-2 border border-border rounded-md hover:bg-muted flex items-center justify-center space-x-2">
          <Settings2 className="h-4 w-4" />
          <span>AI Settings</span>
        </button>
      </div>
    </div>
  )
}