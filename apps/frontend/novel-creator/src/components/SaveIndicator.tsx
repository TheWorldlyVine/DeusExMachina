import { formatDistanceToNow } from 'date-fns'
import { Check, X, AlertCircle, RefreshCw } from 'lucide-react'

interface SaveIndicatorProps {
  isSaving: boolean
  lastSaved: Date | null
  error: string | null
  isDirty: boolean
}

export function SaveIndicator({ isSaving, lastSaved, error, isDirty }: SaveIndicatorProps) {
  if (isSaving) {
    return (
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <RefreshCw className="h-4 w-4 animate-spin" />
        <span>Saving...</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center gap-2 text-sm text-destructive">
        <AlertCircle className="h-4 w-4" />
        <span>Save failed</span>
      </div>
    )
  }

  if (isDirty) {
    return (
      <div className="flex items-center gap-2 text-sm text-warning">
        <X className="h-4 w-4" />
        <span>Unsaved changes</span>
      </div>
    )
  }

  if (lastSaved) {
    return (
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Check className="h-4 w-4 text-green-500" />
        <span>Saved {formatDistanceToNow(lastSaved, { addSuffix: true })}</span>
      </div>
    )
  }

  return null
}