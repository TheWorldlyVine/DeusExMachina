import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Section } from './Section'

describe('Section', () => {
  it('renders with default props', () => {
    render(
      <Section>
        <h2>Test Content</h2>
      </Section>
    )
    
    const section = screen.getByRole('region')
    expect(section).toBeInTheDocument()
    expect(section).toHaveClass('section', 'size-md', 'bg-default')
    expect(screen.getByText('Test Content')).toBeInTheDocument()
  })

  it('renders different sizes', () => {
    const { rerender } = render(<Section size="sm">Small</Section>)
    expect(screen.getByRole('region')).toHaveClass('size-sm')

    rerender(<Section size="lg">Large</Section>)
    expect(screen.getByRole('region')).toHaveClass('size-lg')

    rerender(<Section size="xl">Extra Large</Section>)
    expect(screen.getByRole('region')).toHaveClass('size-xl')
  })

  it('renders different backgrounds', () => {
    const { rerender } = render(<Section background="surface">Surface</Section>)
    expect(screen.getByRole('region')).toHaveClass('bg-surface')

    rerender(<Section background="primary">Primary</Section>)
    expect(screen.getByRole('region')).toHaveClass('bg-primary')
  })

  it('renders centered content', () => {
    render(<Section centered>Centered</Section>)
    expect(screen.getByRole('region')).toHaveClass('centered')
  })

  it('wraps content in container', () => {
    render(
      <Section>
        <p>Container content</p>
      </Section>
    )
    
    const container = screen.getByText('Container content').parentElement
    expect(container).toHaveClass('container')
  })

  it('accepts custom className', () => {
    render(<Section className="custom-section">Custom</Section>)
    expect(screen.getByRole('region')).toHaveClass('custom-section')
  })

  it('forwards ref', () => {
    const ref = vi.fn()
    render(<Section ref={ref}>Ref Section</Section>)
    expect(ref).toHaveBeenCalled()
  })
})