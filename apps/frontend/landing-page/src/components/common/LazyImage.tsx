import { useState } from 'react'
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver'
import styles from './LazyImage.module.css'

interface LazyImageProps {
  src: string
  alt: string
  placeholder?: string
  className?: string
  onLoad?: () => void
  onError?: (e: React.SyntheticEvent<HTMLImageElement>) => void
}

export function LazyImage({ 
  src, 
  alt, 
  placeholder = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1 1"%3E%3Crect width="1" height="1" fill="%23374151"/%3E%3C/svg%3E',
  className,
  onLoad,
  onError
}: LazyImageProps) {
  const [ref, isVisible] = useIntersectionObserver<HTMLDivElement>({
    threshold: 0.1,
    rootMargin: '50px'
  })
  const [isLoaded, setIsLoaded] = useState(false)
  const [hasError, setHasError] = useState(false)

  const handleLoad = () => {
    setIsLoaded(true)
    onLoad?.()
  }

  const handleError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    setHasError(true)
    onError?.(e)
  }

  // Convert to WebP if supported (you'd need server-side support for this)
  const imageSrc = src.endsWith('.webp') ? src : src

  return (
    <div ref={ref} className={`${styles.container} ${className || ''}`}>
      {/* Placeholder */}
      <img
        src={placeholder}
        alt=""
        className={`${styles.placeholder} ${isLoaded ? styles.hidden : ''}`}
        aria-hidden="true"
      />
      
      {/* Actual image */}
      {isVisible && !hasError && (
        <img
          src={imageSrc}
          alt={alt}
          onLoad={handleLoad}
          onError={handleError}
          className={`${styles.image} ${isLoaded ? styles.loaded : ''}`}
        />
      )}
      
      {/* Error state */}
      {hasError && (
        <div className={styles.error}>
          <span>Failed to load image</span>
        </div>
      )}
    </div>
  )
}