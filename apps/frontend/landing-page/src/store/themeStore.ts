import { create } from 'zustand'
import { persist } from 'zustand/middleware'

type Theme = 'light' | 'dark'

interface ThemeStore {
  theme: Theme
  setTheme: (theme: Theme) => void
  toggleTheme: () => void
  initTheme: () => void
}

export const useThemeStore = create<ThemeStore>()(
  persist(
    (set) => ({
      theme: 'light',
      setTheme: (theme) => set({ theme }),
      toggleTheme: () => set((state) => ({ 
        theme: state.theme === 'light' ? 'dark' : 'light' 
      })),
      initTheme: () => {
        // Check system preference if no stored preference
        const stored = localStorage.getItem('theme-storage')
        if (!stored) {
          const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
          set({ theme: prefersDark ? 'dark' : 'light' })
        }
      }
    }),
    {
      name: 'theme-storage',
      partialize: (state) => ({ theme: state.theme })
    }
  )
)