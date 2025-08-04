import { useEffect, useRef, useCallback } from 'react'
import { useAppDispatch, useAppSelector } from './redux'
import { saveContent } from '@/features/editor/editorSlice'
import toast from 'react-hot-toast'

interface UseAutoSaveOptions {
  documentId: string | undefined
  content: string
  delay?: number // milliseconds to wait after user stops typing
  enabled?: boolean
  onSaveStart?: () => void
  onSaveSuccess?: () => void
  onSaveError?: (error: Error) => void
}

export function useAutoSave({
  documentId,
  content,
  delay = 2000, // 2 seconds default
  enabled = true,
  onSaveStart,
  onSaveSuccess,
  onSaveError
}: UseAutoSaveOptions) {
  const dispatch = useAppDispatch()
  const { isSaving, lastSaved, error } = useAppSelector(state => state.editor)
  const timeoutRef = useRef<NodeJS.Timeout>()
  const lastSavedContentRef = useRef<string>('')
  const saveToastRef = useRef<string | null>(null)

  // Debounced save function
  const debouncedSave = useCallback(async () => {
    if (!documentId || !enabled) return
    
    // Don't save if content hasn't changed
    if (content === lastSavedContentRef.current) return
    
    try {
      // Dismiss any existing save toast
      if (saveToastRef.current) {
        toast.dismiss(saveToastRef.current)
      }
      
      // Show saving toast
      saveToastRef.current = toast.loading('Saving...', {
        duration: Infinity
      })
      
      onSaveStart?.()
      
      // Dispatch save action
      await dispatch(saveContent({ documentId, content })).unwrap()
      
      // Update last saved content
      lastSavedContentRef.current = content
      
      // Show success toast
      toast.success('Saved', {
        id: saveToastRef.current,
        duration: 2000
      })
      
      onSaveSuccess?.()
    } catch (err) {
      // Show error toast
      toast.error('Failed to save. Will retry...', {
        id: saveToastRef.current || undefined,
        duration: 4000
      })
      
      onSaveError?.(err as Error)
      
      // Retry after 5 seconds
      setTimeout(() => {
        debouncedSave()
      }, 5000)
    } finally {
      saveToastRef.current = null
    }
  }, [documentId, content, enabled, dispatch, onSaveStart, onSaveSuccess, onSaveError])

  // Effect to handle debounced saving
  useEffect(() => {
    if (!enabled || !documentId) return

    // Clear existing timeout
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }

    // Set new timeout
    timeoutRef.current = setTimeout(() => {
      debouncedSave()
    }, delay)

    // Cleanup
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [content, delay, enabled, documentId, debouncedSave])

  // Save on window blur or before unload
  useEffect(() => {
    if (!enabled || !documentId) return

    const handleBlur = () => {
      // Save immediately when window loses focus
      if (content !== lastSavedContentRef.current) {
        debouncedSave()
      }
    }

    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      // Warn user if there are unsaved changes
      if (content !== lastSavedContentRef.current) {
        e.preventDefault()
        e.returnValue = 'You have unsaved changes. Are you sure you want to leave?'
        // Try to save before leaving
        debouncedSave()
      }
    }

    window.addEventListener('blur', handleBlur)
    window.addEventListener('beforeunload', handleBeforeUnload)

    return () => {
      window.removeEventListener('blur', handleBlur)
      window.removeEventListener('beforeunload', handleBeforeUnload)
    }
  }, [content, documentId, enabled, debouncedSave])

  return {
    isSaving,
    lastSaved,
    error,
    saveNow: debouncedSave
  }
}