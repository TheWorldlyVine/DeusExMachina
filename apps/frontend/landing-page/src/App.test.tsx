import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import App from './App'

describe('App', () => {
  it('renders without crashing', () => {
    render(<App />)
    expect(document.querySelector('.app')).toBeInTheDocument()
  })

  it('renders hero section', () => {
    render(<App />)
    const heading = screen.getByText('Build Immersive Worlds')
    expect(heading).toBeInTheDocument()
  })

  it('renders all main sections', () => {
    render(<App />)
    
    // Check for main section headings
    expect(screen.getByText('Features')).toBeInTheDocument()
    expect(screen.getByText('Community Showcase')).toBeInTheDocument()
    expect(screen.getByText('What Creators Say')).toBeInTheDocument()
    expect(screen.getByText('Trusted by Thousands')).toBeInTheDocument()
    expect(screen.getByText('Simple, Transparent Pricing')).toBeInTheDocument()
  })
})