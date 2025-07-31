import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Card } from './Card'

describe('Card', () => {
  it('renders with default props', () => {
    render(<Card>Card Content</Card>)
    const card = screen.getByText('Card Content').parentElement
    expect(card).toBeInTheDocument()
    expect(card).toHaveClass('card')
    expect(card).toHaveClass('default')
    expect(card).toHaveClass('padding-md')
  })

  it('renders different variants', () => {
    const { rerender } = render(<Card variant="outlined">Outlined</Card>)
    expect(screen.getByText('Outlined').parentElement).toHaveClass('outlined')

    rerender(<Card variant="elevated">Elevated</Card>)
    expect(screen.getByText('Elevated').parentElement).toHaveClass('elevated')
  })

  it('renders different padding sizes', () => {
    const { rerender } = render(<Card padding="none">No Padding</Card>)
    const noPaddingCard = screen.getByText('No Padding').parentElement
    expect(noPaddingCard).toHaveClass('padding-none')

    rerender(<Card padding="sm">Small Padding</Card>)
    const smPaddingCard = screen.getByText('Small Padding').parentElement
    expect(smPaddingCard).toHaveClass('padding-sm')

    rerender(<Card padding="lg">Large Padding</Card>)
    const lgPaddingCard = screen.getByText('Large Padding').parentElement
    expect(lgPaddingCard).toHaveClass('padding-lg')
  })

  it('renders interactive state', () => {
    render(<Card interactive>Interactive Card</Card>)
    const card = screen.getByText('Interactive Card').parentElement
    expect(card).toHaveClass('interactive')
  })

  it('accepts custom className', () => {
    render(<Card className="custom-card">Custom</Card>)
    const card = screen.getByText('Custom').parentElement
    expect(card).toHaveClass('custom-card')
  })

  it('forwards ref', () => {
    const ref = vi.fn()
    render(<Card ref={ref}>Ref Card</Card>)
    expect(ref).toHaveBeenCalled()
  })
})