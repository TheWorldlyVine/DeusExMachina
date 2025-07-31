import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { InteractiveDemo } from './InteractiveDemo'

// Mock framer-motion
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: any) => <div {...props}>{children}</div>,
    line: ({ children, ...props }: any) => <line {...props}>{children}</line>,
  },
  AnimatePresence: ({ children }: any) => children,
}))

describe('InteractiveDemo', () => {
  it('renders demo header', () => {
    render(<InteractiveDemo isActive={false} />)
    
    expect(screen.getByText('Interactive World Map')).toBeInTheDocument()
    expect(screen.getByText('Click nodes to explore connections')).toBeInTheDocument()
  })

  it('renders all demo nodes', () => {
    render(<InteractiveDemo isActive={false} />)
    
    expect(screen.getByText('The Crystal Citadel')).toBeInTheDocument()
    expect(screen.getByText('Elara Moonshadow')).toBeInTheDocument()
    expect(screen.getByText("Dragon's Awakening")).toBeInTheDocument()
    expect(screen.getByText('The Whispering Woods')).toBeInTheDocument()
  })

  it('selects first node when activated', async () => {
    const { rerender } = render(<InteractiveDemo isActive={false} />)
    
    // Activate the demo
    rerender(<InteractiveDemo isActive={true} />)
    
    await waitFor(() => {
      // Should show details for the first node (The Crystal Citadel)
      const detailPanel = screen.getByText('A mystical place of power and ancient secrets.')
      expect(detailPanel).toBeInTheDocument()
    })
  })

  it('shows node details on click', async () => {
    render(<InteractiveDemo isActive={true} />)
    
    // Click on Elara Moonshadow
    const characterNode = screen.getByText('Elara Moonshadow')
    fireEvent.click(characterNode.parentElement!)
    
    await waitFor(() => {
      expect(screen.getByText('A key figure in the unfolding narrative.')).toBeInTheDocument()
      expect(screen.getByText('character')).toBeInTheDocument()
    })
  })

  it('displays correct icons for node types', () => {
    render(<InteractiveDemo isActive={false} />)
    
    // Check for emoji icons
    expect(screen.getByText('🏰')).toBeInTheDocument() // location
    expect(screen.getByText('👤')).toBeInTheDocument() // character
    expect(screen.getByText('⚡')).toBeInTheDocument() // event
  })

  it('shows connected nodes in detail panel', async () => {
    render(<InteractiveDemo isActive={true} />)
    
    // Click on The Crystal Citadel
    const locationNode = screen.getByText('The Crystal Citadel')
    fireEvent.click(locationNode.parentElement!)
    
    await waitFor(() => {
      expect(screen.getByText('Connected to:')).toBeInTheDocument()
      // Check if connected nodes are listed
      const detailPanel = screen.getByText('Connected to:').parentElement
      expect(detailPanel).toHaveTextContent('Elara Moonshadow')
      expect(detailPanel).toHaveTextContent("Dragon's Awakening")
    })
  })

  it('renders SVG connections between nodes', () => {
    const { container } = render(<InteractiveDemo isActive={false} />)
    
    const svg = container.querySelector('svg.connections')
    expect(svg).toBeInTheDocument()
    
    // Should have connection lines
    const lines = svg?.querySelectorAll('line')
    expect(lines?.length).toBeGreaterThan(0)
  })

  it('shows footer message', () => {
    render(<InteractiveDemo isActive={false} />)
    
    expect(screen.getByText('This is just a glimpse. Create unlimited worlds with infinite possibilities.')).toBeInTheDocument()
  })
})