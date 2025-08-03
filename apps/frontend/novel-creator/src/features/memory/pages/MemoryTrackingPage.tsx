import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { 
  getCharacters, 
  getPlots, 
  getLocations,
  createCharacter,
  updateCharacter,
  deleteCharacter,
  createPlot,
  updatePlot,
  deletePlot,
} from '@/features/memory/enhancedMemorySlice'
import { CharacterCard } from '@/components/memory/CharacterCard'
import { CharacterModal } from '@/components/memory/CharacterModal'
import { PlotThreadCard } from '@/components/memory/PlotThreadCard'
import { PlotThreadModal } from '@/components/memory/PlotThreadModal'
import { 
  Users, 
  GitBranch, 
  Map, 
  Plus, 
  Search,
  Filter,
  ChevronDown,
} from 'lucide-react'
import { CharacterMemory, PlotMemory } from '@/types/memory'
import toast from 'react-hot-toast'

type TabType = 'characters' | 'plots' | 'world'

export function MemoryTrackingPage() {
  const { documentId } = useParams()
  const dispatch = useAppDispatch()
  
  const [activeTab, setActiveTab] = useState<TabType>('characters')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedRole, setSelectedRole] = useState<string>('all')
  const [selectedStatus, setSelectedStatus] = useState<string>('all')
  const [showCharacterModal, setShowCharacterModal] = useState(false)
  const [showPlotModal, setShowPlotModal] = useState(false)
  const [editingCharacter, setEditingCharacter] = useState<CharacterMemory | null>(null)
  const [editingPlot, setEditingPlot] = useState<PlotMemory | null>(null)
  
  const { characters, plots, locations, isLoading, error } = useAppSelector(state => state.memory)

  // Load data on mount
  useEffect(() => {
    if (documentId) {
      dispatch(getCharacters(documentId))
      dispatch(getPlots(documentId))
      dispatch(getLocations(documentId))
    }
  }, [documentId, dispatch])

  // Handle errors
  useEffect(() => {
    if (error) {
      toast.error(error)
    }
  }, [error])

  // Filter characters
  const filteredCharacters = characters.filter(char => {
    const matchesSearch = searchQuery === '' || 
      char.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      char.currentState?.description?.toLowerCase().includes(searchQuery.toLowerCase())
    
    const matchesRole = selectedRole === 'all' || char.role === selectedRole
    
    return matchesSearch && matchesRole
  })

  // Filter plots
  const filteredPlots = plots.filter(plot => {
    const matchesSearch = searchQuery === '' || 
      plot.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      plot.description?.toLowerCase().includes(searchQuery.toLowerCase())
    
    const matchesStatus = selectedStatus === 'all' || plot.currentState.status === selectedStatus
    
    return matchesSearch && matchesStatus
  })

  // Character handlers
  const handleCreateCharacter = async (character: Partial<CharacterMemory>) => {
    if (!documentId) return
    
    try {
      await dispatch(createCharacter({
        ...character,
        projectId: documentId,
      })).unwrap()
      toast.success('Character created successfully')
      setShowCharacterModal(false)
      setEditingCharacter(null)
    } catch (error) {
      toast.error('Failed to create character')
    }
  }

  const handleUpdateCharacter = async (character: Partial<CharacterMemory>) => {
    if (!documentId || !character.characterId) return
    
    try {
      await dispatch(updateCharacter({
        projectId: documentId,
        characterId: character.characterId,
        updates: character,
      })).unwrap()
      toast.success('Character updated successfully')
      setShowCharacterModal(false)
      setEditingCharacter(null)
    } catch (error) {
      toast.error('Failed to update character')
    }
  }

  const handleDeleteCharacter = async (characterId: string) => {
    if (!documentId) return
    
    try {
      await dispatch(deleteCharacter({
        projectId: documentId,
        characterId,
      })).unwrap()
      toast.success('Character deleted successfully')
    } catch (error) {
      toast.error('Failed to delete character')
    }
  }

  // Plot handlers
  const handleCreatePlot = async (plot: Partial<PlotMemory>) => {
    if (!documentId) return
    
    try {
      await dispatch(createPlot({
        ...plot,
        projectId: documentId,
      })).unwrap()
      toast.success('Plot thread created successfully')
      setShowPlotModal(false)
      setEditingPlot(null)
    } catch (error) {
      toast.error('Failed to create plot thread')
    }
  }

  const handleUpdatePlot = async (plot: Partial<PlotMemory>) => {
    if (!documentId || !plot.plotId) return
    
    try {
      await dispatch(updatePlot({
        projectId: documentId,
        plotId: plot.plotId,
        updates: plot,
      })).unwrap()
      toast.success('Plot thread updated successfully')
      setShowPlotModal(false)
      setEditingPlot(null)
    } catch (error) {
      toast.error('Failed to update plot thread')
    }
  }

  const handleDeletePlot = async (plotId: string) => {
    if (!documentId) return
    
    try {
      await dispatch(deletePlot({
        projectId: documentId,
        plotId,
      })).unwrap()
      toast.success('Plot thread deleted successfully')
    } catch (error) {
      toast.error('Failed to delete plot thread')
    }
  }

  const tabs = [
    { id: 'characters' as TabType, label: 'Characters', icon: Users, count: characters.length },
    { id: 'plots' as TabType, label: 'Plot Threads', icon: GitBranch, count: plots.length },
    { id: 'world' as TabType, label: 'World Building', icon: Map, count: locations.length },
  ]

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="border-b border-border bg-background/95 backdrop-blur">
        <div className="px-6 py-4">
          <h1 className="text-2xl font-bold">Story Memory</h1>
          <p className="text-muted-foreground">Track characters, plot threads, and world-building elements</p>
        </div>

        {/* Tabs */}
        <div className="flex space-x-6 px-6">
          {tabs.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`
                pb-3 px-1 border-b-2 transition-colors flex items-center space-x-2
                ${activeTab === tab.id 
                  ? 'border-primary text-foreground' 
                  : 'border-transparent text-muted-foreground hover:text-foreground'
                }
              `}
            >
              <tab.icon className="h-4 w-4" />
              <span>{tab.label}</span>
              <span className="text-xs bg-muted px-1.5 py-0.5 rounded-full">
                {tab.count}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Filters */}
      <div className="px-6 py-4 border-b border-border bg-muted/30">
        <div className="flex items-center space-x-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder={`Search ${activeTab}...`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 border border-input rounded-md bg-background"
            />
          </div>

          {activeTab === 'characters' && (
            <div className="relative">
              <select
                value={selectedRole}
                onChange={(e) => setSelectedRole(e.target.value)}
                className="appearance-none px-4 py-2 pr-8 border border-input rounded-md bg-background"
              >
                <option value="all">All Roles</option>
                <option value="protagonist">Protagonist</option>
                <option value="antagonist">Antagonist</option>
                <option value="supporting">Supporting</option>
                <option value="minor">Minor</option>
              </select>
              <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 pointer-events-none text-muted-foreground" />
            </div>
          )}

          {activeTab === 'plots' && (
            <div className="relative">
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="appearance-none px-4 py-2 pr-8 border border-input rounded-md bg-background"
              >
                <option value="all">All Status</option>
                <option value="PLANNED">Planned</option>
                <option value="ACTIVE">Active</option>
                <option value="RESOLVED">Resolved</option>
                <option value="ABANDONED">Abandoned</option>
              </select>
              <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 pointer-events-none text-muted-foreground" />
            </div>
          )}

          <button
            onClick={() => {
              if (activeTab === 'characters') {
                setEditingCharacter(null)
                setShowCharacterModal(true)
              } else if (activeTab === 'plots') {
                setEditingPlot(null)
                setShowPlotModal(true)
              }
            }}
            className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 flex items-center space-x-2"
          >
            <Plus className="h-4 w-4" />
            <span>
              Add {activeTab === 'characters' ? 'Character' : activeTab === 'plots' ? 'Plot Thread' : 'Location'}
            </span>
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-6">
        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <div className="text-muted-foreground">Loading...</div>
          </div>
        ) : (
          <>
            {activeTab === 'characters' && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {filteredCharacters.length === 0 ? (
                  <div className="col-span-full text-center py-12 text-muted-foreground">
                    {searchQuery || selectedRole !== 'all' 
                      ? 'No characters found matching your filters' 
                      : 'No characters yet. Create your first character!'}
                  </div>
                ) : (
                  filteredCharacters.map(character => (
                    <CharacterCard
                      key={character.characterId}
                      character={character}
                      onEdit={(char) => {
                        setEditingCharacter(char)
                        setShowCharacterModal(true)
                      }}
                      onDelete={handleDeleteCharacter}
                    />
                  ))
                )}
              </div>
            )}

            {activeTab === 'plots' && (
              <div className="space-y-4">
                {filteredPlots.length === 0 ? (
                  <div className="text-center py-12 text-muted-foreground">
                    {searchQuery || selectedStatus !== 'all' 
                      ? 'No plot threads found matching your filters' 
                      : 'No plot threads yet. Create your first plot thread!'}
                  </div>
                ) : (
                  filteredPlots.map(plot => (
                    <PlotThreadCard
                      key={plot.plotId}
                      plot={plot}
                      onEdit={(p) => {
                        setEditingPlot(p)
                        setShowPlotModal(true)
                      }}
                      onDelete={handleDeletePlot}
                    />
                  ))
                )}
              </div>
            )}

            {activeTab === 'world' && (
              <div className="text-center py-12 text-muted-foreground">
                World building features coming soon...
              </div>
            )}
          </>
        )}
      </div>

      {/* Modals */}
      <CharacterModal
        isOpen={showCharacterModal}
        onClose={() => {
          setShowCharacterModal(false)
          setEditingCharacter(null)
        }}
        onSave={editingCharacter ? handleUpdateCharacter : handleCreateCharacter}
        character={editingCharacter}
        projectId={documentId || ''}
      />

      <PlotThreadModal
        isOpen={showPlotModal}
        onClose={() => {
          setShowPlotModal(false)
          setEditingPlot(null)
        }}
        onSave={editingPlot ? handleUpdatePlot : handleCreatePlot}
        plot={editingPlot}
        projectId={documentId || ''}
        characters={characters.map(c => ({ characterId: c.characterId, name: c.name }))}
      />
    </div>
  )
}