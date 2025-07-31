import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useAppStore } from './app.store';

describe('App Store', () => {
  beforeEach(() => {
    // Reset store to initial state
    useAppStore.setState({
      user: null,
      isAuthenticated: false,
      token: null,
      sidebarOpen: true,
      theme: 'light',
    });
  });

  describe('Authentication', () => {
    it('should login user', () => {
      const { result } = renderHook(() => useAppStore());
      
      const user = { id: '1', username: 'testuser', email: 'test@example.com' };
      const token = 'test-token';
      
      act(() => {
        result.current.login(user, token);
      });
      
      expect(result.current.user).toEqual(user);
      expect(result.current.token).toBe(token);
      expect(result.current.isAuthenticated).toBe(true);
    });
    
    it('should logout user', () => {
      const { result } = renderHook(() => useAppStore());
      
      // First login
      act(() => {
        result.current.login(
          { id: '1', username: 'testuser', email: 'test@example.com' },
          'test-token'
        );
      });
      
      // Then logout
      act(() => {
        result.current.logout();
      });
      
      expect(result.current.user).toBeNull();
      expect(result.current.token).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });
    
    it('should update user data', () => {
      const { result } = renderHook(() => useAppStore());
      
      // Login first
      act(() => {
        result.current.login(
          { id: '1', username: 'testuser', email: 'test@example.com' },
          'test-token'
        );
      });
      
      // Update user
      act(() => {
        result.current.updateUser({ email: 'newemail@example.com' });
      });
      
      expect(result.current.user?.email).toBe('newemail@example.com');
      expect(result.current.user?.username).toBe('testuser'); // Should remain unchanged
    });
  });

  describe('UI State', () => {
    it('should toggle sidebar', () => {
      const { result } = renderHook(() => useAppStore());
      
      expect(result.current.sidebarOpen).toBe(true);
      
      act(() => {
        result.current.toggleSidebar();
      });
      
      expect(result.current.sidebarOpen).toBe(false);
      
      act(() => {
        result.current.toggleSidebar();
      });
      
      expect(result.current.sidebarOpen).toBe(true);
    });
    
    it('should set theme', () => {
      const { result } = renderHook(() => useAppStore());
      
      expect(result.current.theme).toBe('light');
      
      act(() => {
        result.current.setTheme('dark');
      });
      
      expect(result.current.theme).toBe('dark');
    });
  });
});