import '@testing-library/jest-dom'
import { vi } from 'vitest'
import { configure } from '@testing-library/react'

// Configure React Testing Library to be less verbose
configure({
  getElementError: (message) => {
    const error = new Error(message?.split('\n')[0] || 'Element not found')
    error.name = 'TestingLibraryElementError'
    return error
  },
})

// CSS modules are handled by the vitest config css.modules.classNameStrategy
// which returns class names as-is for testing

// Mock framer-motion globally
vi.mock('framer-motion', async () => {
  const actual = await vi.importActual<typeof import('react')>('react')
  const React = actual
  
  interface MotionProps {
    children?: React.ReactNode
    [key: string]: unknown
  }
  
  return {
    motion: new Proxy({}, {
      get: (_target, prop) => {
        // Return a component that renders the HTML element
        const Component = React.forwardRef<HTMLElement, MotionProps>((props, ref) => {
          const { children, ...rest } = props
          return React.createElement(prop as string, { ...rest, ref }, children)
        })
        Component.displayName = `motion.${String(prop)}`
        return Component
      }
    }),
    AnimatePresence: ({ children }: { children?: React.ReactNode }) => children,
  }
})

// Suppress console output in tests to reduce verbosity
// Set DEBUG=true environment variable to see console output
if (!process.env.DEBUG) {
  global.console = {
    ...console,
    log: vi.fn(),
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
    trace: vi.fn(),
    group: vi.fn(),
    groupEnd: vi.fn(),
    groupCollapsed: vi.fn(),
    time: vi.fn(),
    timeEnd: vi.fn(),
    timeLog: vi.fn(),
    assert: vi.fn(),
    clear: vi.fn(),
    count: vi.fn(),
    countReset: vi.fn(),
    dir: vi.fn(),
    dirxml: vi.fn(),
    table: vi.fn(),
    profile: vi.fn(),
    profileEnd: vi.fn(),
  }
}

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // deprecated
    removeListener: vi.fn(), // deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

// Mock IntersectionObserver
class MockIntersectionObserver implements IntersectionObserver {
  readonly root: Element | null = null
  readonly rootMargin: string = ''
  readonly thresholds: ReadonlyArray<number> = []
  
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
  takeRecords(): IntersectionObserverEntry[] {
    return []
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).IntersectionObserver = MockIntersectionObserver

// Mock Image for testing image loading
Object.defineProperty(global, 'Image', {
  writable: true,
  value: class MockImage {
    onload: (() => void) | null = null
    onerror: (() => void) | null = null
    src = ''
    
    constructor() {
      setTimeout(() => {
        if (this.onload) this.onload()
      }, 0)
    }
  }
})