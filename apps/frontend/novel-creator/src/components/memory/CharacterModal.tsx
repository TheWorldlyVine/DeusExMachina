import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { 
  X, 
  User, 
  Save,
  Plus,
  Trash2,
  Target,
  Heart,
  MapPin,
  Users,
} from 'lucide-react'
import { CharacterMemory } from '@/types/memory'

const characterSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  role: z.enum(['protagonist', 'antagonist', 'supporting', 'minor']),
  description: z.string().optional(),
  emotionalState: z.string().optional(),
  location: z.string().optional(),
  goals: z.array(z.string()).optional(),
})

type CharacterFormData = z.infer<typeof characterSchema>

interface CharacterModalProps {
  isOpen: boolean
  onClose: () => void
  onSave: (character: Partial<CharacterMemory>) => void
  character?: CharacterMemory | null
  projectId: string
}

export function CharacterModal({
  isOpen,
  onClose,
  onSave,
  character,
  projectId,
}: CharacterModalProps) {
  const [goals, setGoals] = useState<string[]>([])
  const [newGoal, setNewGoal] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
    setValue,
  } = useForm<CharacterFormData>({
    resolver: zodResolver(characterSchema),
    defaultValues: {
      name: '',
      role: 'supporting',
      description: '',
      emotionalState: '',
      location: '',
      goals: [],
    },
  })

  useEffect(() => {
    if (character) {
      reset({
        name: character.name,
        role: character.role,
        description: character.currentState?.description || '',
        emotionalState: character.currentState?.emotionalState || '',
        location: character.currentState?.location || '',
        goals: character.currentState?.goals || [],
      })
      setGoals(character.currentState?.goals || [])
    } else {
      reset({
        name: '',
        role: 'supporting',
        description: '',
        emotionalState: '',
        location: '',
        goals: [],
      })
      setGoals([])
    }
  }, [character, reset])

  const addGoal = () => {
    if (newGoal.trim()) {
      const updatedGoals = [...goals, newGoal.trim()]
      setGoals(updatedGoals)
      setValue('goals', updatedGoals)
      setNewGoal('')
    }
  }

  const removeGoal = (index: number) => {
    const updatedGoals = goals.filter((_, i) => i !== index)
    setGoals(updatedGoals)
    setValue('goals', updatedGoals)
  }

  const onSubmit = async (data: CharacterFormData) => {
    const characterData: Partial<CharacterMemory> = {
      characterId: character?.characterId,
      projectId,
      name: data.name,
      role: data.role,
      currentState: {
        description: data.description || '',
        emotionalState: data.emotionalState || '',
        goals: data.goals || [],
        location: data.location || '',
        lastUpdated: new Date().toISOString(),
      },
      observations: character?.observations || [],
      reflections: character?.reflections || [],
      executedActions: character?.executedActions || [],
      relationships: character?.relationships || [],
      metadata: character?.metadata || {},
    }

    onSave(characterData)
    onClose()
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-full items-center justify-center p-4">
        <div className="fixed inset-0 bg-black/50" onClick={onClose} />
        
        <div className="relative w-full max-w-2xl bg-background rounded-lg shadow-xl">
          <div className="flex items-center justify-between p-6 border-b border-border">
            <h2 className="text-xl font-semibold flex items-center space-x-2">
              <User className="h-5 w-5" />
              <span>{character ? 'Edit Character' : 'New Character'}</span>
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
                  Character Name
                </label>
                <input
                  {...register('name')}
                  type="text"
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                  placeholder="Enter character name"
                />
                {errors.name && (
                  <p className="text-sm text-destructive mt-1">{errors.name.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">
                  Role
                </label>
                <select
                  {...register('role')}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                >
                  <option value="protagonist">Protagonist</option>
                  <option value="antagonist">Antagonist</option>
                  <option value="supporting">Supporting Character</option>
                  <option value="minor">Minor Character</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">
                  Description
                </label>
                <textarea
                  {...register('description')}
                  rows={3}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background resize-none"
                  placeholder="Brief description of the character"
                />
              </div>
            </div>

            {/* Current State */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Current State</h3>
              
              <div>
                <label className="block text-sm font-medium mb-2 flex items-center space-x-2">
                  <Heart className="h-4 w-4" />
                  <span>Emotional State</span>
                </label>
                <input
                  {...register('emotionalState')}
                  type="text"
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                  placeholder="e.g., anxious, determined, conflicted"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2 flex items-center space-x-2">
                  <MapPin className="h-4 w-4" />
                  <span>Current Location</span>
                </label>
                <input
                  {...register('location')}
                  type="text"
                  className="w-full px-3 py-2 border border-input rounded-md bg-background"
                  placeholder="Where is the character now?"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2 flex items-center space-x-2">
                  <Target className="h-4 w-4" />
                  <span>Goals</span>
                </label>
                <div className="space-y-2">
                  {goals.map((goal, index) => (
                    <div key={index} className="flex items-center space-x-2">
                      <span className="flex-1 px-3 py-2 border border-input rounded-md bg-muted">
                        {goal}
                      </span>
                      <button
                        type="button"
                        onClick={() => removeGoal(index)}
                        className="p-2 hover:bg-muted rounded-md transition-colors"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                  <div className="flex items-center space-x-2">
                    <input
                      type="text"
                      value={newGoal}
                      onChange={(e) => setNewGoal(e.target.value)}
                      onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addGoal())}
                      className="flex-1 px-3 py-2 border border-input rounded-md bg-background"
                      placeholder="Add a goal"
                    />
                    <button
                      type="button"
                      onClick={addGoal}
                      className="p-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90"
                    >
                      <Plus className="h-4 w-4" />
                    </button>
                  </div>
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
                <span>{isSubmitting ? 'Saving...' : 'Save Character'}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}