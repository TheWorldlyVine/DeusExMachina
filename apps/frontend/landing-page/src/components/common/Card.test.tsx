import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Card } from './Card'

describe('Card', () => {
  it('renders with default props', () => {
    render(<Card>Card Content</Card>)
    const card = screen.getByText('Card Content').parentElement
    expect(card).toBeInTheDocument()
    expect(card).toHaveClass('card', 'default', 'padding-md')
  })

  it('renders different variants', () => {
    const { rerender } = render(<Card variant="outlined">Outlined</Card>)
    expect(screen.getByText('Outlined').parentElement).toHaveClass('outlined')

    rerender(<Card variant="elevated">Elevated</Card>)
    expect(screen.getByText('Elevated').parentElement).toHaveClass('elevated')
  })

  it('renders different padding sizes', () => {
    const { rerender } = render(<Card padding="none">No Padding</Card>)
    expect(screen.getByText('No Padding').parentElement).toHaveClass('padding-none')

    rerender(<Card padding="sm">Small Padding</Card>)
    expect(screen.getByText('Small Padding').parentElement).toHaveClass('padding-sm')

    rerender(<Card padding="lg">Large Padding</Card>)
    expect(screen.getByText('Large Padding').parentElement).toHaveClass('padding-lg')
  })

  it('renders interactive state', () => {
    render(<Card interactive>Interactive Card</Card>)
    expect(screen.getByText('Interactive Card').parentElement).toHaveClass('interactive')
  })

  it('accepts custom className', () => {
    render(<Card className="custom-card">Custom</Card>)
    expect(screen.getByText('Custom').parentElement).toHaveClass('custom-card')
  })

  it('forwards ref', () => {
    const ref = vi.fn()
    render(<Card ref={ref}>Ref Card</Card>)
    expect(ref).toHaveBeenCalled()
  })
})