import { useState } from 'react'
import { 
  User, 
  Edit2, 
  Trash2, 
  ChevronDown, 
  ChevronRight,
  Hash,
  Heart,
  Target,
  Calendar,
  MapPin,
  Users,
  Sparkles,
  Brain,
  Eye,
  Zap,
} from 'lucide-react'
import { CharacterMemory } from '@/types/memory'

interface CharacterCardProps {
  character: CharacterMemory
  onEdit: (character: CharacterMemory) => void
  onDelete: (characterId: string) => void
  onSelect?: (characterId: string) => void
  isSelected?: boolean
  compact?: boolean
}

export function CharacterCard({
  character,
  onEdit,
  onDelete,
  onSelect,
  isSelected = false,
  compact = false,
}: CharacterCardProps) {
  const [isExpanded, setIsExpanded] = useState(!compact)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const getRoleColor = (role: string) => {
    switch (role) {
      case 'protagonist':
        return 'text-blue-600 bg-blue-100 dark:text-blue-400 dark:bg-blue-900/30'
      case 'antagonist':
        return 'text-red-600 bg-red-100 dark:text-red-400 dark:bg-red-900/30'
      case 'supporting':
        return 'text-green-600 bg-green-100 dark:text-green-400 dark:bg-green-900/30'
      case 'minor':
        return 'text-gray-600 bg-gray-100 dark:text-gray-400 dark:bg-gray-900/30'
      default:
        return 'text-gray-600 bg-gray-100 dark:text-gray-400 dark:bg-gray-900/30'
    }
  }

  const handleDelete = () => {
    if (showDeleteConfirm) {
      onDelete(character.characterId)
      setShowDeleteConfirm(false)
    } else {
      setShowDeleteConfirm(true)
      setTimeout(() => setShowDeleteConfirm(false), 3000)
    }
  }

  return (
    <div
      className={`
        border border-border rounded-lg p-4 transition-all duration-200
        ${isSelected ? 'border-primary shadow-lg' : 'hover:border-muted-foreground/50 hover:shadow-md'}
        ${onSelect ? 'cursor-pointer' : ''}
      `}
      onClick={() => onSelect?.(character.characterId)}
    >
      <div className="flex items-start justify-between">
        <div className="flex items-start space-x-3 flex-1">
          <button
            onClick={(e) => {
              e.stopPropagation()
              setIsExpanded(!isExpanded)
            }}
            className="mt-1 p-0.5 hover:bg-muted rounded"
          >
            {isExpanded ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
          </button>

          <div className="flex-1">
            <div className="flex items-center space-x-2 mb-1">
              <User className="h-5 w-5 text-muted-foreground" />
              <h3 className="font-semibold text-lg">{character.name}</h3>
              <span className={`text-xs px-2 py-0.5 rounded-full ${getRoleColor(character.role)}`}>
                {character.role}
              </span>
            </div>

            {character.currentState?.description && (
              <p className="text-sm text-muted-foreground mb-2">
                {character.currentState.description}
              </p>
            )}

            {isExpanded && (
              <div className="mt-4 space-y-4">
                {/* Current State */}
                {character.currentState && (
                  <div>
                    <h4 className="text-sm font-medium mb-2 flex items-center space-x-1">
                      <Sparkles className="h-4 w-4" />
                      <span>Current State</span>
                    </h4>
                    <div className="space-y-2 pl-5">
                      {character.currentState.emotionalState && (
                        <div className="flex items-center space-x-2 text-sm">
                          <Heart className="h-3 w-3 text-muted-foreground" />
                          <span className="text-muted-foreground">Emotion:</span>
                          <span>{character.currentState.emotionalState}</span>
                        </div>
                      )}
                      {character.currentState.goals && character.currentState.goals.length > 0 && (
                        <div className="flex items-start space-x-2 text-sm">
                          <Target className="h-3 w-3 text-muted-foreground mt-1" />
                          <div>
                            <span className="text-muted-foreground">Goals:</span>
                            <ul className="list-disc list-inside ml-2">
                              {character.currentState.goals.map((goal, idx) => (
                                <li key={idx}>{goal}</li>
                              ))}
                            </ul>
                          </div>
                        </div>
                      )}
                      {character.currentState.location && (
                        <div className="flex items-center space-x-2 text-sm">
                          <MapPin className="h-3 w-3 text-muted-foreground" />
                          <span className="text-muted-foreground">Location:</span>
                          <span>{character.currentState.location}</span>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Relationships */}
                {character.relationships && character.relationships.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2 flex items-center space-x-1">
                      <Users className="h-4 w-4" />
                      <span>Relationships</span>
                    </h4>
                    <div className="space-y-1 pl-5">
                      {character.relationships.map((rel, idx) => (
                        <div key={idx} className="text-sm">
                          <span className="font-medium">{rel.targetCharacterName || rel.targetCharacterId}:</span>{' '}
                          <span className="text-muted-foreground">{rel.relationshipType}</span>
                          {rel.description && (
                            <p className="text-xs text-muted-foreground ml-2">{rel.description}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Recent Observations */}
                {character.observations && character.observations.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2 flex items-center space-x-1">
                      <Eye className="h-4 w-4" />
                      <span>Recent Observations</span>
                    </h4>
                    <div className="space-y-2 pl-5">
                      {character.observations.slice(0, 3).map((obs, idx) => (
                        <div key={idx} className="text-sm">
                          <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                            <Calendar className="h-3 w-3" />
                            <span>Chapter {obs.chapterNumber}, Scene {obs.sceneNumber}</span>
                          </div>
                          <p className="mt-1">{obs.observation}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Recent Reflections */}
                {character.reflections && character.reflections.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2 flex items-center space-x-1">
                      <Brain className="h-4 w-4" />
                      <span>Recent Reflections</span>
                    </h4>
                    <div className="space-y-2 pl-5">
                      {character.reflections.slice(0, 2).map((ref, idx) => (
                        <div key={idx} className="text-sm">
                          <p className="italic">"{ref.reflection}"</p>
                          {ref.emotionalImpact && (
                            <p className="text-xs text-muted-foreground mt-1">
                              Emotional impact: {ref.emotionalImpact}
                            </p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Actions */}
                {character.executedActions && character.executedActions.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2 flex items-center space-x-1">
                      <Zap className="h-4 w-4" />
                      <span>Recent Actions</span>
                    </h4>
                    <div className="space-y-1 pl-5">
                      {character.executedActions.slice(0, 3).map((action, idx) => (
                        <div key={idx} className="text-sm">
                          <span className="font-medium">{action.action}</span>
                          {action.result && (
                            <span className="text-muted-foreground"> → {action.result}</span>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Timeline Summary */}
                {character.timelineSummary && (
                  <div className="text-xs text-muted-foreground flex items-center space-x-2">
                    <Hash className="h-3 w-3" />
                    <span>
                      Appears in {character.timelineSummary.totalScenes || 0} scenes
                      {character.timelineSummary.firstAppearance && (
                        <> • First seen in Chapter {character.timelineSummary.firstAppearance.chapterNumber}</>
                      )}
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center space-x-2" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => onEdit(character)}
            className="p-2 hover:bg-muted rounded-md transition-colors"
          >
            <Edit2 className="h-4 w-4" />
          </button>
          <button
            onClick={handleDelete}
            className={`
              p-2 rounded-md transition-colors
              ${showDeleteConfirm 
                ? 'bg-destructive text-destructive-foreground hover:bg-destructive/90' 
                : 'hover:bg-muted'
              }
            `}
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  )
}