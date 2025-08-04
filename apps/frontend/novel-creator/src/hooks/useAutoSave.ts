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
  const retryTimeoutRef = useRef<NodeJS.Timeout>() // Separate timeout for retries
  const lastSavedContentRef = useRef<string>('')
  const saveToastRef = useRef<string | null>(null)
  const retryCountRef = useRef<number>(0)
  const maxRetries = 5
  const isRetryingRef = useRef<boolean>(false) // Track if we're in retry mode

  // Debounced save function
  const debouncedSave = useCallback(async () => {
    console.log('[AutoSave] Save triggered', { documentId, enabled, contentLength: content.length })
    
    if (!documentId || !enabled) {
      console.log('[AutoSave] Save skipped - missing documentId or not enabled')
      return
    }
    
    // Don't save if content hasn't changed
    if (content === lastSavedContentRef.current) {
      console.log('[AutoSave] Save skipped - content unchanged')
      return
    }
    
    try {
      console.log('[AutoSave] Starting save...')
      
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
      
      console.log('[AutoSave] Save successful')
      
      // Show success toast
      toast.success('Saved', {
        id: saveToastRef.current,
        duration: 2000
      })
      
      onSaveSuccess?.()
      // Reset retry count on success
      retryCountRef.current = 0
      isRetryingRef.current = false
    } catch (err) {
      console.error('[AutoSave] Save failed:', err)
      
      // Show error toast
      toast.error('Failed to save. Will retry...', {
        id: saveToastRef.current || undefined,
        duration: 4000
      })
      
      onSaveError?.(err as Error)
      
      // Implement exponential backoff for retries
      retryCountRef.current += 1
      if (retryCountRef.current <= maxRetries) {
        // Start at 30 seconds, then 60, 120 (capped at 60)
        const baseDelay = 30000 // 30 seconds initial delay
        const backoffDelay = Math.min(baseDelay * Math.pow(2, retryCountRef.current - 1), 60000) // Max 1 minute
        console.log(`[AutoSave] Retrying in ${backoffDelay / 1000} seconds (attempt ${retryCountRef.current}/${maxRetries})`)
        
        isRetryingRef.current = true
        
        // Clear any existing retry timeout
        if (retryTimeoutRef.current) {
          clearTimeout(retryTimeoutRef.current)
        }
        
        // Set retry timeout (separate from the main debounce timeout)
        retryTimeoutRef.current = setTimeout(() => {
          isRetryingRef.current = false
          debouncedSave()
        }, backoffDelay)
      } else {
        console.error('[AutoSave] Max retries reached, giving up')
        toast.error('Auto-save failed. Please save manually or refresh the page.', {
          duration: 10000
        })
        isRetryingRef.current = false
      }
    } finally {
      saveToastRef.current = null
    }
  }, [documentId, content, enabled, dispatch, onSaveStart, onSaveSuccess, onSaveError])

  // Effect to handle debounced saving
  useEffect(() => {
    if (!enabled || !documentId) return
    
    // Don't set a new timeout if we're in retry mode
    if (isRetryingRef.current) {
      console.log('[AutoSave] Skipping new save timeout - retry in progress')
      return
    }

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
      // Don't clear retry timeout on cleanup
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