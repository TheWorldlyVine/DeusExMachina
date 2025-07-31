import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Card } from './Card'

describe('Card', () => {
  it('renders with default props', () => {
    const { container } = render(<Card>Card Content</Card>)
    const card = container.firstChild as HTMLElement
    expect(card).toBeInTheDocument()
    expect(card).toHaveTextContent('Card Content')
    // CSS modules transform class names, so we just check the element exists
    expect(card.tagName).toBe('DIV')
  })

  it('renders different variants', () => {
    const { container, rerender } = render(<Card variant="outlined">Outlined</Card>)
    let card = container.firstChild as HTMLElement
    expect(card).toHaveTextContent('Outlined')

    rerender(<Card variant="elevated">Elevated</Card>)
    card = container.firstChild as HTMLElement
    expect(card).toHaveTextContent('Elevated')
  })

  it('renders different padding sizes', () => {
    const { container, rerender } = render(<Card padding="none">No Padding</Card>)
    let card = container.firstChild as HTMLElement
    expect(card).toHaveTextContent('No Padding')

    rerender(<Card padding="sm">Small Padding</Card>)
    card = container.firstChild as HTMLElement
    expect(card).toHaveTextContent('Small Padding')

    rerender(<Card padding="lg">Large Padding</Card>)
    card = container.firstChild as HTMLElement
    expect(card).toHaveTextContent('Large Padding')
  })

  it('renders interactive state', () => {
    const { container } = render(<Card interactive>Interactive Card</Card>)
    const card = container.firstChild as HTMLElement
    expect(card).toHaveTextContent('Interactive Card')
  })

  it('accepts custom className', () => {
    const { container } = render(<Card className="custom-card">Custom</Card>)
    const card = container.firstChild as HTMLElement
    expect(card).toHaveTextContent('Custom')
    // With CSS modules, custom classes are still applied
    expect(card.className).toContain('custom-card')
  })

  it('forwards ref', () => {
    const ref = vi.fn()
    render(<Card ref={ref}>Ref Card</Card>)
    expect(ref).toHaveBeenCalled()
  })
})