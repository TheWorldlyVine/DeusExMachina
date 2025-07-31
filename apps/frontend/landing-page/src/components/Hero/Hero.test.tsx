import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { Hero } from './index'

// Mock framer-motion to avoid animation issues in tests
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: React.PropsWithChildren<Record<string, unknown>>) => <div {...props}>{children}</div>,
  },
  AnimatePresence: ({ children }: React.PropsWithChildren) => children,
}))

describe('Hero', () => {
  it('renders main heading with gradient', () => {
    render(<Hero />)
    
    const heading = screen.getByRole('heading', { level: 1 })
    expect(heading).toHaveTextContent('Build Immersive Worlds That Come Alive')
    
    const gradientSpan = screen.getByText('That Come Alive')
    expect(gradientSpan).toHaveClass('titleGradient')
  })

  it('renders subtitle text', () => {
    render(<Hero />)
    
    const subtitle = screen.getByText(/The ultimate world-building software/)
    expect(subtitle).toBeInTheDocument()
    expect(subtitle).toHaveClass('subtitle')
  })

  it('displays statistics', () => {
    render(<Hero />)
    
    expect(screen.getByText('50K+')).toBeInTheDocument()
    expect(screen.getByText('Active Creators')).toBeInTheDocument()
    expect(screen.getByText('1M+')).toBeInTheDocument()
    expect(screen.getByText('Worlds Created')).toBeInTheDocument()
    expect(screen.getByText('4.9')).toBeInTheDocument()
    expect(screen.getByText('User Rating')).toBeInTheDocument()
  })

  it('renders CTA buttons', () => {
    render(<Hero />)
    
    const primaryCTA = screen.getByRole('button', { name: 'Start Building Worlds' })
    const secondaryCTA = screen.getByRole('button', { name: 'Watch Demo' })
    
    expect(primaryCTA).toBeInTheDocument()
    expect(primaryCTA).toHaveClass('primaryCta')
    expect(secondaryCTA).toBeInTheDocument()
    expect(secondaryCTA).toHaveClass('secondaryCta')
  })

  it('shows demo when primary CTA is clicked', async () => {
    render(<Hero />)
    
    const primaryCTA = screen.getByRole('button', { name: 'Start Building Worlds' })
    fireEvent.click(primaryCTA)
    
    // InteractiveDemo should be activated
    await waitFor(() => {
      const demoContainer = screen.getByText('Interactive World Map')
      expect(demoContainer).toBeInTheDocument()
    })
  })

  it('displays free tier message', () => {
    render(<Hero />)
    
    const freeText = screen.getByText(/Free forever for your first world/)
    expect(freeText).toBeInTheDocument()
    expect(freeText).toHaveClass('freeText')
  })

  it('renders InteractiveDemo component', () => {
    render(<Hero />)
    
    // The demo should be rendered but not necessarily visible initially
    expect(screen.getByText('Interactive World Map')).toBeInTheDocument()
  })
})