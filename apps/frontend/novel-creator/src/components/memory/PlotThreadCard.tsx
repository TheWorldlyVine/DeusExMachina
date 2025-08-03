import { useState } from 'react'
import { 
  GitBranch, 
  Edit2, 
  Trash2, 
  ChevronDown, 
  ChevronRight,
  AlertCircle,
  CheckCircle,
  Clock,
  TrendingUp,
  Calendar,
  Flag,
} from 'lucide-react'
import { PlotMemory } from '@/types/memory'

interface PlotThreadCardProps {
  plot: PlotMemory
  onEdit: (plot: PlotMemory) => void
  onDelete: (plotId: string) => void
  onSelect?: (plotId: string) => void
  isSelected?: boolean
  compact?: boolean
}

export function PlotThreadCard({
  plot,
  onEdit,
  onDelete,
  onSelect,
  isSelected = false,
  compact = false,
}: PlotThreadCardProps) {
  const [isExpanded, setIsExpanded] = useState(!compact)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <TrendingUp className="h-4 w-4 text-green-600" />
      case 'RESOLVED':
        return <CheckCircle className="h-4 w-4 text-blue-600" />
      case 'ABANDONED':
        return <AlertCircle className="h-4 w-4 text-red-600" />
      case 'PLANNED':
        return <Clock className="h-4 w-4 text-yellow-600" />
      default:
        return <Clock className="h-4 w-4 text-gray-600" />
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'text-green-600 bg-green-100 dark:text-green-400 dark:bg-green-900/30'
      case 'RESOLVED':
        return 'text-blue-600 bg-blue-100 dark:text-blue-400 dark:bg-blue-900/30'
      case 'ABANDONED':
        return 'text-red-600 bg-red-100 dark:text-red-400 dark:bg-red-900/30'
      case 'PLANNED':
        return 'text-yellow-600 bg-yellow-100 dark:text-yellow-400 dark:bg-yellow-900/30'
      default:
        return 'text-gray-600 bg-gray-100 dark:text-gray-400 dark:bg-gray-900/30'
    }
  }

  const getTensionLevel = (tension: number) => {
    if (tension >= 8) return { label: 'Critical', color: 'text-red-600' }
    if (tension >= 6) return { label: 'High', color: 'text-orange-600' }
    if (tension >= 4) return { label: 'Moderate', color: 'text-yellow-600' }
    return { label: 'Low', color: 'text-green-600' }
  }

  const handleDelete = () => {
    if (showDeleteConfirm) {
      onDelete(plot.plotId)
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
      onClick={() => onSelect?.(plot.plotId)}
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
              <GitBranch className="h-5 w-5 text-muted-foreground" />
              <h3 className="font-semibold text-lg">{plot.title}</h3>
              <span className={`text-xs px-2 py-0.5 rounded-full flex items-center space-x-1 ${getStatusColor(plot.currentState.status)}`}>
                {getStatusIcon(plot.currentState.status)}
                <span>{plot.currentState.status}</span>
              </span>
            </div>

            {plot.description && (
              <p className="text-sm text-muted-foreground mb-2">
                {plot.description}
              </p>
            )}

            <div className="flex items-center space-x-4 text-sm text-muted-foreground">
              <div className="flex items-center space-x-1">
                <Flag className="h-3 w-3" />
                <span>Arc: {plot.storyArc}</span>
              </div>
              {plot.currentState.tensionLevel !== undefined && (
                <div className={`flex items-center space-x-1 ${getTensionLevel(plot.currentState.tensionLevel).color}`}>
                  <span>Tension: {getTensionLevel(plot.currentState.tensionLevel).label}</span>
                  <span className="text-xs">({plot.currentState.tensionLevel}/10)</span>
                </div>
              )}
            </div>

            {isExpanded && (
              <div className="mt-4 space-y-4">
                {/* Key Moments */}
                {plot.keyMoments && plot.keyMoments.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2">Key Moments</h4>
                    <div className="space-y-2 pl-5">
                      {plot.keyMoments.map((moment, idx) => (
                        <div key={idx} className="text-sm border-l-2 border-muted pl-3">
                          <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                            <Calendar className="h-3 w-3" />
                            <span>Chapter {moment.chapterNumber}, Scene {moment.sceneNumber}</span>
                          </div>
                          <p className="mt-1 font-medium">{moment.momentType}: {moment.description}</p>
                          {moment.impact && (
                            <p className="text-xs text-muted-foreground mt-1">Impact: {moment.impact}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Involved Characters */}
                {plot.involvedCharacters && plot.involvedCharacters.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2">Involved Characters</h4>
                    <div className="flex flex-wrap gap-2 pl-5">
                      {plot.involvedCharacters.map((char, idx) => (
                        <span
                          key={idx}
                          className="text-xs px-2 py-1 bg-muted rounded-full"
                        >
                          {char.characterName} ({char.role})
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {/* Conflicts */}
                {plot.conflicts && plot.conflicts.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2">Conflicts</h4>
                    <div className="space-y-2 pl-5">
                      {plot.conflicts.map((conflict, idx) => (
                        <div key={idx} className="text-sm">
                          <div className="flex items-center space-x-2">
                            <span className="font-medium">{conflict.type}</span>
                            {conflict.resolved && (
                              <CheckCircle className="h-3 w-3 text-green-600" />
                            )}
                          </div>
                          <p className="text-muted-foreground">{conflict.description}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Related Subplots */}
                {plot.relatedSubplots && plot.relatedSubplots.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2">Related Subplots</h4>
                    <div className="flex flex-wrap gap-2 pl-5">
                      {plot.relatedSubplots.map((subplot, idx) => (
                        <span
                          key={idx}
                          className="text-xs px-2 py-1 border border-border rounded"
                        >
                          {subplot}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {/* Foreshadowing */}
                {plot.foreshadowing && plot.foreshadowing.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2">Foreshadowing</h4>
                    <ul className="list-disc list-inside pl-5 space-y-1">
                      {plot.foreshadowing.map((item, idx) => (
                        <li key={idx} className="text-sm text-muted-foreground">{item}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center space-x-2" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => onEdit(plot)}
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