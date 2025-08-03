import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { 
  X, 
  GitBranch, 
  Save,
  Plus,
  Trash2,
  Users,
  AlertCircle,
} from 'lucide-react'
import { PlotMemory } from '@/types/memory'

const plotSchema = z.object({
  title: z.string().min(1, 'Title is required'),
  description: z.string().optional(),
  storyArc: z.string().min(1, 'Story arc is required'),
  status: z.enum(['PLANNED', 'ACTIVE', 'RESOLVED', 'ABANDONED']),
  tensionLevel: z.number().min(0).max(10).optional(),
  foreshadowing: z.array(z.string()).optional(),
})

type PlotFormData = z.infer<typeof plotSchema>

interface PlotThreadModalProps {
  isOpen: boolean
  onClose: () => void
  onSave: (plot: Partial<PlotMemory>) => void
  plot?: PlotMemory | null
  projectId: string
  characters?: Array<{ characterId: string; name: string }>
}

export function PlotThreadModal({
  isOpen,
  onClose,
  onSave,
  plot,
  projectId,
  characters = [],
}: PlotThreadModalProps) {
  const [involvedCharacters, setInvolvedCharacters] = useState<Array<{ characterId: string; characterName: string; role: string }>>([])
  const [foreshadowingItems, setForeshadowingItems] = useState<string[]>([])
  const [newForeshadowing, setNewForeshadowing] = useState('')
  const [selectedCharacter, setSelectedCharacter] = useState('')
  const [characterRole, setCharacterRole] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
    setValue,
    watch,
  } = useForm<PlotFormData>({
    resolver: zodResolver(plotSchema),
    defaultValues: {
      title: '',
      description: '',
      storyArc: '',
      status: 'PLANNED',
      tensionLevel: 5,
      foreshadowing: [],
    },
  })

  const tensionLevel = watch('tensionLevel')

  useEffect(() => {
    if (plot) {
      reset({
        title: plot.title,
        description: plot.description || '',
        storyArc: plot.storyArc,
        status: plot.currentState.status,
        tensionLevel: plot.currentState.tensionLevel || 5,
        foreshadowing: plot.foreshadowing || [],
      })
      setInvolvedCharacters(plot.involvedCharacters || [])
      setForeshadowingItems(plot.foreshadowing || [])
    } else {
      reset({
        title: '',
        description: '',
        storyArc: '',
        status: 'PLANNED',
        tensionLevel: 5,
        foreshadowing: [],
      })
      setInvolvedCharacters([])
      setForeshadowingItems([])
    }
  }, [plot, reset])

  const addCharacter = () => {
    if (selectedCharacter && characterRole) {
      const character = characters.find(c => c.characterId === selectedCharacter)
      if (character && !involvedCharacters.find(c => c.characterId === selectedCharacter)) {
        setInvolvedCharacters([...involvedCharacters, {
          characterId: selectedCharacter,
          characterName: character.name,
          role: characterRole,
        }])
        setSelectedCharacter('')
        setCharacterRole('')
      }
    }
  }

  const removeCharacter = (characterId: string) => {
    setInvolvedCharacters(involvedCharacters.filter(c => c.characterId !== characterId))
  }

  const addForeshadowing = () => {
    if (newForeshadowing.trim()) {
      const updated = [...foreshadowingItems, newForeshadowing.trim()]
      setForeshadowingItems(updated)
      setValue('foreshadowing', updated)
      setNewForeshadowing('')
    }
  }

  const removeForeshadowing = (index: number) => {
    const updated = foreshadowingItems.filter((_, i) => i !== index)
    setForeshadowingItems(updated)
    setValue('foreshadowing', updated)
  }

  const onSubmit = async (data: PlotFormData) => {
    const plotData: Partial<PlotMemory> = {
      plotId: plot?.plotId,
      projectId,
      title: data.title,
      description: data.description || '',
      storyArc: data.storyArc,
      currentState: {
        status: data.status,
        tensionLevel: data.tensionLevel || 5,
        lastUpdated: new Date().toISOString(),
      },
      involvedCharacters,
      foreshadowing: data.foreshadowing || [],
      keyMoments: plot?.keyMoments || [],
      conflicts: plot?.conflicts || [],
      relatedSubplots: plot?.relatedSubplots || [],
      metadata: plot?.metadata || {},
    }

    onSave(plotData)
    onClose()
  }

  if (!isOpen) return null

  const getTensionColor = (level: number) => {
    if (level >= 8) return 'text-red-600'
    if (level >= 6) return 'text-orange-600'
    if (level >= 4) return 'text-yellow-600'
    return 'text-green-600'
  }

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-full items-center justify-center p-4">
        <div className="fixed inset-0 bg-black/50" onClick={onClose} />
        
        <div className="relative w-full max-w-2xl bg-background rounded-lg shadow-xl">
          <div className="flex items-center justify-between p-6 border-b border-border">
            <h2 className="text-xl font-semibold flex items-center space-x-2">
              <GitBranch className="h-5 w-5" />
              <span>{plot ? 'Edit Plot Thread' : 'New Plot Thread'}</span>
            </h2>
            <button
              onClick={onClose}
              className="p-2 hover:bg-muted rounded-md transition-colors"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-6">
            {/* Basic Information */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Basic Information</h3>
              
              <div>
                <label className="block text-sm font-medium mb-2">
                  Plot Title
                </label>
                <input
                  {...register('title')}
                  type="text"
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                  placeholder="Enter plot thread title"
                />
                {errors.title && (
                  <p className="text-sm text-destructive mt-1">{errors.title.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">
                  Description
                </label>
                <textarea
                  {...register('description')}
                  rows={3}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background resize-none"
                  placeholder="Brief description of the plot thread"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">
                  Story Arc
                </label>
                <input
                  {...register('storyArc')}
                  type="text"
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                  placeholder="e.g., Main Plot, Romance Subplot, Mystery Arc"
                />
                {errors.storyArc && (
                  <p className="text-sm text-destructive mt-1">{errors.storyArc.message}</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">
                    Status
                  </label>
                  <select
                    {...register('status')}
                    className="w-full px-3 py-2 border border-input rounded-md bg-background"
                  >
                    <option value="PLANNED">Planned</option>
                    <option value="ACTIVE">Active</option>
                    <option value="RESOLVED">Resolved</option>
                    <option value="ABANDONED">Abandoned</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">
                    Tension Level
                  </label>
                  <div className="flex items-center space-x-2">
                    <input
                      {...register('tensionLevel', { valueAsNumber: true })}
                      type="range"
                      min="0"
                      max="10"
                      className="flex-1"
                    />
                    <span className={`font-medium ${getTensionColor(tensionLevel || 5)}`}>
                      {tensionLevel || 5}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Involved Characters */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium flex items-center space-x-2">
                <Users className="h-4 w-4" />
                <span>Involved Characters</span>
              </h3>
              
              <div className="space-y-2">
                {involvedCharacters.map((char) => (
                  <div key={char.characterId} className="flex items-center justify-between p-2 border border-border rounded-md">
                    <div>
                      <span className="font-medium">{char.characterName}</span>
                      <span className="text-sm text-muted-foreground ml-2">({char.role})</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeCharacter(char.characterId)}
                      className="p-1 hover:bg-muted rounded"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                ))}
                
                <div className="flex items-center space-x-2">
                  <select
                    value={selectedCharacter}
                    onChange={(e) => setSelectedCharacter(e.target.value)}
                    className="flex-1 px-3 py-2 border border-input rounded-md bg-background"
                  >
                    <option value="">Select character</option>
                    {characters.map((char) => (
                      <option key={char.characterId} value={char.characterId}>
                        {char.name}
                      </option>
                    ))}
                  </select>
                  <input
                    type="text"
                    value={characterRole}
                    onChange={(e) => setCharacterRole(e.target.value)}
                    placeholder="Role in plot"
                    className="flex-1 px-3 py-2 border border-input rounded-md bg-background"
                  />
                  <button
                    type="button"
                    onClick={addCharacter}
                    disabled={!selectedCharacter || !characterRole}
                    className="p-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>

            {/* Foreshadowing */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium flex items-center space-x-2">
                <AlertCircle className="h-4 w-4" />
                <span>Foreshadowing Elements</span>
              </h3>
              
              <div className="space-y-2">
                {foreshadowingItems.map((item, index) => (
                  <div key={index} className="flex items-center space-x-2">
                    <span className="flex-1 px-3 py-2 border border-input rounded-md bg-muted">
                      {item}
                    </span>
                    <button
                      type="button"
                      onClick={() => removeForeshadowing(index)}
                      className="p-2 hover:bg-muted rounded-md transition-colors"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                ))}
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={newForeshadowing}
                    onChange={(e) => setNewForeshadowing(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addForeshadowing())}
                    className="flex-1 px-3 py-2 border border-input rounded-md bg-background"
                    placeholder="Add foreshadowing element"
                  />
                  <button
                    type="button"
                    onClick={addForeshadowing}
                    className="p-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-end space-x-3 pt-6 border-t border-border">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 border border-input rounded-md hover:bg-muted"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 flex items-center space-x-2 disabled:opacity-50"
              >
                <Save className="h-4 w-4" />
                <span>{isSubmitting ? 'Saving...' : 'Save Plot Thread'}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}