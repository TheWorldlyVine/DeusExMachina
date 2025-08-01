import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ThemeToggle } from './ThemeToggle'
import { useThemeStore } from '@/store/themeStore'

describe('ThemeToggle', () => {
  beforeEach(() => {
    // Reset theme store
    useThemeStore.setState({ theme: 'light' })
  })

  it('renders with light theme icon', () => {
    render(<ThemeToggle />)
    const button = screen.getByRole('button', { name: /switch to dark theme/i })
    expect(button).toBeInTheDocument()
    expect(button.tagName).toBe('BUTTON')
  })

  it('renders with dark theme icon when theme is dark', () => {
    useThemeStore.setState({ theme: 'dark' })
    render(<ThemeToggle />)
    const button = screen.getByRole('button', { name: /switch to light theme/i })
    expect(button).toBeInTheDocument()
  })

  it('toggles theme on click', () => {
    render(<ThemeToggle />)
    const button = screen.getByRole('button')
    
    // Initial state is light
    expect(useThemeStore.getState().theme).toBe('light')
    
    // Click to toggle to dark
    fireEvent.click(button)
    expect(useThemeStore.getState().theme).toBe('dark')
    
    // Click to toggle back to light
    fireEvent.click(button)
    expect(useThemeStore.getState().theme).toBe('light')
  })

  it('has correct accessibility attributes', () => {
    render(<ThemeToggle />)
    const button = screen.getByRole('button')
    
    expect(button).toHaveAttribute('aria-label')
    expect(button).toHaveAttribute('title')
  })

  it('shows correct icon based on theme', () => {
    const { container, rerender } = render(<ThemeToggle />)
    
    // Light theme shows moon icon
    expect(container.querySelector('svg path')).toHaveAttribute('d', expect.stringContaining('17.293 13.293'))
    
    // Dark theme shows sun icon
    useThemeStore.setState({ theme: 'dark' })
    rerender(<ThemeToggle />)
    expect(container.querySelector('svg path')).toHaveAttribute('fill-rule', 'evenodd')
  })
})