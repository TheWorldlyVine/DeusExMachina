import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Section } from './Section'

describe('Section', () => {
  it('renders with default props', () => {
    render(
      <Section>
        <h2>Test Content</h2>
      </Section>
    )
    
    const section = screen.getByText('Test Content').closest('section')
    expect(section).toBeInTheDocument()
    expect(section).toHaveClass('section')
    expect(section).toHaveClass('size-md')
    expect(section).toHaveClass('bg-default')
    expect(screen.getByText('Test Content')).toBeInTheDocument()
  })

  it('renders different sizes', () => {
    const { rerender } = render(<Section size="sm">Small</Section>)
    const section = screen.getByText('Small').closest('section')
    expect(section).toHaveClass('size-sm')

    rerender(<Section size="lg">Large</Section>)
    const lgSection = screen.getByText('Large').closest('section')
    expect(lgSection).toHaveClass('size-lg')

    rerender(<Section size="xl">Extra Large</Section>)
    const xlSection = screen.getByText('Extra Large').closest('section')
    expect(xlSection).toHaveClass('size-xl')
  })

  it('renders different backgrounds', () => {
    const { rerender } = render(<Section background="surface">Surface</Section>)
    const surfaceSection = screen.getByText('Surface').closest('section')
    expect(surfaceSection).toHaveClass('bg-surface')

    rerender(<Section background="primary">Primary</Section>)
    const primarySection = screen.getByText('Primary').closest('section')
    expect(primarySection).toHaveClass('bg-primary')
  })

  it('renders centered content', () => {
    render(<Section centered>Centered</Section>)
    const section = screen.getByText('Centered').closest('section')
    expect(section).toHaveClass('centered')
  })

  it('wraps content in container', () => {
    render(
      <Section>
        <p>Container content</p>
      </Section>
    )
    
    const container = screen.getByText('Container content').parentElement
    expect(container).toBeInTheDocument()
    expect(container?.className).toContain('container')
  })

  it('accepts custom className', () => {
    render(<Section className="custom-section">Custom</Section>)
    const section = screen.getByText('Custom').closest('section')
    expect(section).toBeInTheDocument()
    expect(section?.className).toContain('custom-section')
  })

  it('forwards ref', () => {
    const ref = vi.fn()
    render(<Section ref={ref}>Ref Section</Section>)
    expect(ref).toHaveBeenCalled()
  })
})