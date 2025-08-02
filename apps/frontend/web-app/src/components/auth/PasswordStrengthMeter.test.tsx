import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { PasswordStrengthMeter } from './PasswordStrengthMeter'

describe('PasswordStrengthMeter', () => {
  it('shows weak strength for short passwords', () => {
    render(<PasswordStrengthMeter password="abc" />)
    
    expect(screen.getByText(/weak/i)).toBeInTheDocument()
    expect(screen.getByTestId('strength-bar')).toHaveStyle({ width: '25%' })
    expect(screen.getByTestId('strength-bar')).toHaveClass('bg-red-500')
  })

  it('shows medium strength for moderate passwords', () => {
    render(<PasswordStrengthMeter password="password123" />)
    
    expect(screen.getByText(/medium/i)).toBeInTheDocument()
    expect(screen.getByTestId('strength-bar')).toHaveStyle({ width: '50%' })
    expect(screen.getByTestId('strength-bar')).toHaveClass('bg-yellow-500')
  })

  it('shows strong strength for complex passwords', () => {
    render(<PasswordStrengthMeter password="StrongPass123!" />)
    
    expect(screen.getByText(/strong/i)).toBeInTheDocument()
    expect(screen.getByTestId('strength-bar')).toHaveStyle({ width: '75%' })
    expect(screen.getByTestId('strength-bar')).toHaveClass('bg-green-500')
  })

  it('shows very strong for very complex passwords', () => {
    render(<PasswordStrengthMeter password="V3ry$tr0ng&P@ssw0rd!" />)
    
    expect(screen.getByText(/very strong/i)).toBeInTheDocument()
    expect(screen.getByTestId('strength-bar')).toHaveStyle({ width: '100%' })
    expect(screen.getByTestId('strength-bar')).toHaveClass('bg-green-600')
  })

  it('does not render when password is empty', () => {
    const { container } = render(<PasswordStrengthMeter password="" />)
    
    expect(container.firstChild).toBeNull()
  })

  it('shows requirements checklist', () => {
    render(<PasswordStrengthMeter password="weak" />)
    
    expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument()
    expect(screen.getByText(/uppercase letter/i)).toBeInTheDocument()
    expect(screen.getByText(/lowercase letter/i)).toBeInTheDocument()
    expect(screen.getByText(/number/i)).toBeInTheDocument()
    expect(screen.getByText(/special character/i)).toBeInTheDocument()
  })

  it('checks off requirements as they are met', () => {
    render(<PasswordStrengthMeter password="StrongPass123!" />)
    
    const lengthReq = screen.getByText(/at least 8 characters/i).closest('li')
    const uppercaseReq = screen.getByText(/uppercase letter/i).closest('li')
    const lowercaseReq = screen.getByText(/lowercase letter/i).closest('li')
    const numberReq = screen.getByText(/number/i).closest('li')
    const specialReq = screen.getByText(/special character/i).closest('li')
    
    expect(lengthReq).toHaveClass('text-green-600')
    expect(uppercaseReq).toHaveClass('text-green-600')
    expect(lowercaseReq).toHaveClass('text-green-600')
    expect(numberReq).toHaveClass('text-green-600')
    expect(specialReq).toHaveClass('text-green-600')
  })

  it('uses proper ARIA attributes for accessibility', () => {
    render(<PasswordStrengthMeter password="password123" />)
    
    const progressBar = screen.getByRole('progressbar')
    expect(progressBar).toHaveAttribute('aria-label', 'Password strength')
    expect(progressBar).toHaveAttribute('aria-valuenow', '50')
    expect(progressBar).toHaveAttribute('aria-valuemin', '0')
    expect(progressBar).toHaveAttribute('aria-valuemax', '100')
  })
})