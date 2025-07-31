import '@testing-library/jest-dom'
import { vi } from 'vitest'

// Mock CSS modules to return the class name as-is
const mockCssModule = new Proxy({}, {
  get: (target, prop) => {
    if (prop === 'default' || prop === '__esModule') {
      return mockCssModule
    }
    return prop
  }
})

// Replace the CSS module imports
vi.mock(/\.module\.css$/, () => {
  return { default: mockCssModule }
})

// Suppress console output in tests to reduce verbosity
// Set DEBUG=true environment variable to see console output
const originalConsole = { ...console }

if (!process.env.DEBUG) {
  console.log = vi.fn()
  console.debug = vi.fn()
  console.info = vi.fn()
  console.warn = vi.fn()
  // Keep error output for debugging test failures
  console.error = (...args) => {
    // Only show non-React error boundary errors
    const errorString = args.join(' ')
    if (!errorString.includes('Error boundary') && !errorString.includes('ReactDOMTestUtils')) {
      originalConsole.error(...args)
    }
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