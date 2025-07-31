import { useEffect, useRef, useState } from 'react'

interface UseIntersectionObserverOptions {
  threshold?: number | number[]
  rootMargin?: string
  triggerOnce?: boolean
}

export function useIntersectionObserver<T extends HTMLElement>(
  options: UseIntersectionObserverOptions = {}
): [React.RefObject<T>, boolean] {
  const {
    threshold = 0,
    rootMargin = '0px',
    triggerOnce = true
  } = options

  const ref = useRef<T>(null)
  const [isIntersecting, setIsIntersecting] = useState(false)
  const [hasTriggered, setHasTriggered] = useState(false)

  useEffect(() => {
    const element = ref.current
    if (!element || (triggerOnce && hasTriggered)) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        const intersecting = entry.isIntersecting
        setIsIntersecting(intersecting)
        
        if (intersecting && triggerOnce) {
          setHasTriggered(true)
        }
      },
      {
        threshold,
        rootMargin
      }
    )

    observer.observe(element)

    return () => {
      if (element) {
        observer.unobserve(element)
      }
    }
  }, [threshold, rootMargin, triggerOnce, hasTriggered])

  return [ref, triggerOnce ? hasTriggered : isIntersecting]
}