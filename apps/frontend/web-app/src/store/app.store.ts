import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';

interface User {
  id: string;
  username: string;
  email: string;
}

interface AppState {
  // Auth state
  user: User | null;
  isAuthenticated: boolean;
  token: string | null;
  
  // Actions
  login: (user: User, token: string) => void;
  logout: () => void;
  updateUser: (user: Partial<User>) => void;
  
  // UI state
  sidebarOpen: boolean;
  toggleSidebar: () => void;
  theme: 'light' | 'dark';
  setTheme: (theme: 'light' | 'dark') => void;
}

export const useAppStore = create<AppState>()(
  devtools(
    persist(
      (set) => ({
        // Auth state
        user: null,
        isAuthenticated: false,
        token: null,
        
        // Actions
        login: (user, token) => 
          set({ user, token, isAuthenticated: true }, false, 'auth/login'),
        
        logout: () => 
          set({ user: null, token: null, isAuthenticated: false }, false, 'auth/logout'),
        
        updateUser: (userData) =>
          set(
            (state) => ({
              user: state.user ? { ...state.user, ...userData } : null,
            }),
            false,
            'user/update'
          ),
        
        // UI state
        sidebarOpen: true,
        toggleSidebar: () =>
          set((state) => ({ sidebarOpen: !state.sidebarOpen }), false, 'ui/toggleSidebar'),
        
        theme: 'light',
        setTheme: (theme) => set({ theme }, false, 'ui/setTheme'),
      }),
      {
        name: 'deus-ex-machina-storage',
        partialize: (state) => ({
          user: state.user,
          isAuthenticated: state.isAuthenticated,
          token: state.token,
          theme: state.theme,
        }),
      }
    ),
    {
      name: 'DeusExMachina',
    }
  )
);