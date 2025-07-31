import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useThemeStore } from './themeStore'

describe('themeStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    useThemeStore.setState({ theme: 'light' })
    // Clear localStorage
    localStorage.clear()
    // Reset matchMedia mock
    vi.clearAllMocks()
  })

  it('should have light theme by default', () => {
    const { theme } = useThemeStore.getState()
    expect(theme).toBe('light')
  })

  it('should toggle theme', () => {
    const { toggleTheme } = useThemeStore.getState()
    
    toggleTheme()
    expect(useThemeStore.getState().theme).toBe('dark')
    
    toggleTheme()
    expect(useThemeStore.getState().theme).toBe('light')
  })

  it('should set theme directly', () => {
    const { setTheme } = useThemeStore.getState()
    
    setTheme('dark')
    expect(useThemeStore.getState().theme).toBe('dark')
    
    setTheme('light')
    expect(useThemeStore.getState().theme).toBe('light')
  })

  it('should detect system dark mode preference', () => {
    // Mock matchMedia to return dark mode preference
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })

    const { initTheme } = useThemeStore.getState()
    initTheme()
    
    expect(useThemeStore.getState().theme).toBe('dark')
  })

  it('should respect stored preference over system preference', () => {
    // Set a stored preference
    localStorage.setItem('theme-storage', JSON.stringify({ state: { theme: 'light' }, version: 0 }))
    
    // Mock matchMedia to return dark mode preference
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })

    const { initTheme } = useThemeStore.getState()
    initTheme()
    
    // Should keep the stored preference
    expect(useThemeStore.getState().theme).toBe('light')
  })
})