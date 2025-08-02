import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SignupPage } from './SignupPage'

// Mock useNavigate
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

// Mock auth service
vi.mock('@/services/auth', () => ({
  authService: {
    signup: vi.fn(),
  },
}))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
})

const renderSignupPage = () => {
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <SignupPage />
      </BrowserRouter>
    </QueryClientProvider>
  )
}

describe('SignupPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders signup form with all required fields', () => {
    renderSignupPage()
    
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign up/i })).toBeInTheDocument()
  })

  it('displays login link', () => {
    renderSignupPage()
    
    const loginLink = screen.getByRole('link', { name: /log in/i })
    expect(loginLink).toBeInTheDocument()
    expect(loginLink).toHaveAttribute('href', '/login')
  })

  it('validates required fields', async () => {
    const user = userEvent.setup()
    renderSignupPage()
    
    const emailInput = screen.getByLabelText(/email/i)
    const passwordInput = screen.getByLabelText(/^password$/i)
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i)
    
    // Type and clear to trigger validation
    await user.type(emailInput, 'a')
    await user.clear(emailInput)
    
    await user.type(passwordInput, 'a')
    await user.clear(passwordInput)
    
    await user.type(confirmPasswordInput, 'a')
    await user.clear(confirmPasswordInput)
    
    expect(screen.getByText(/email is required/i)).toBeInTheDocument()
    expect(screen.getByText(/password is required/i)).toBeInTheDocument()
    expect(screen.getByText(/please confirm your password/i)).toBeInTheDocument()
  })

  it('validates email format', async () => {
    const user = userEvent.setup()
    renderSignupPage()
    
    const emailInput = screen.getByLabelText(/email/i)
    await user.type(emailInput, 'invalid-email')
    await user.tab()
    
    expect(screen.getByText(/invalid email address/i)).toBeInTheDocument()
  })

  it('validates password strength', async () => {
    const user = userEvent.setup()
    renderSignupPage()
    
    const passwordInput = screen.getByLabelText(/^password$/i)
    await user.type(passwordInput, 'weak')
    await user.tab()
    
    expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument()
  })

  it('validates password match', async () => {
    const user = userEvent.setup()
    renderSignupPage()
    
    const passwordInput = screen.getByLabelText(/^password$/i)
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i)
    
    await user.type(passwordInput, 'StrongPass123!')
    await user.type(confirmPasswordInput, 'DifferentPass123!')
    await user.tab()
    
    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument()
  })

  it('shows password strength meter', async () => {
    const user = userEvent.setup()
    renderSignupPage()
    
    const passwordInput = screen.getByLabelText(/^password$/i)
    await user.type(passwordInput, 'weak')
    
    expect(screen.getByTestId('password-strength-meter')).toBeInTheDocument()
  })

  it('enables submit button only when form is valid', async () => {
    const user = userEvent.setup()
    renderSignupPage()
    
    const submitButton = screen.getByRole('button', { name: /sign up/i })
    expect(submitButton).toBeDisabled()
    
    const emailInput = screen.getByLabelText(/email/i)
    const passwordInput = screen.getByLabelText(/^password$/i)
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i)
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'StrongPass123!')
    await user.type(confirmPasswordInput, 'StrongPass123!')
    
    await waitFor(() => {
      expect(submitButton).not.toBeDisabled()
    })
  })

  it('submits form with valid data', async () => {
    const { authService } = await import('@/services/auth')
    const mockSignup = vi.mocked(authService.signup)
    mockSignup.mockResolvedValueOnce({
      user: { id: '1', email: 'test@example.com' },
      token: 'fake-token',
    })
    
    const user = userEvent.setup()
    renderSignupPage()
    
    const emailInput = screen.getByLabelText(/email/i)
    const passwordInput = screen.getByLabelText(/^password$/i)
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i)
    const submitButton = screen.getByRole('button', { name: /sign up/i })
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'StrongPass123!')
    await user.type(confirmPasswordInput, 'StrongPass123!')
    await user.click(submitButton)
    
    await waitFor(() => {
      expect(mockSignup).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'StrongPass123!',
      })
    })
  })

  it('redirects to verification page on successful signup', async () => {
    const { authService } = await import('@/services/auth')
    const mockSignup = vi.mocked(authService.signup)
    mockSignup.mockResolvedValueOnce({
      user: { id: '1', email: 'test@example.com' },
      token: 'fake-token',
    })
    
    const user = userEvent.setup()
    renderSignupPage()
    
    const emailInput = screen.getByLabelText(/email/i)
    const passwordInput = screen.getByLabelText(/^password$/i)
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i)
    const submitButton = screen.getByRole('button', { name: /sign up/i })
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'StrongPass123!')
    await user.type(confirmPasswordInput, 'StrongPass123!')
    await user.click(submitButton)
    
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/verify-email')
    })
  })

  it('displays error message on signup failure', async () => {
    const { authService } = await import('@/services/auth')
    const mockSignup = vi.mocked(authService.signup)
    mockSignup.mockRejectedValueOnce(new Error('Email already exists'))
    
    const user = userEvent.setup()
    renderSignupPage()
    
    const emailInput = screen.getByLabelText(/email/i)
    const passwordInput = screen.getByLabelText(/^password$/i)
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i)
    const submitButton = screen.getByRole('button', { name: /sign up/i })
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'StrongPass123!')
    await user.type(confirmPasswordInput, 'StrongPass123!')
    await user.click(submitButton)
    
    await waitFor(() => {
      expect(screen.getByText(/email already exists/i)).toBeInTheDocument()
    })
  })

  it('disables form during submission', async () => {
    const { authService } = await import('@/services/auth')
    const mockSignup = vi.mocked(authService.signup)
    mockSignup.mockImplementation(() => new Promise(() => {})) // Never resolves
    
    const user = userEvent.setup()
    renderSignupPage()
    
    const emailInput = screen.getByLabelText(/email/i)
    const passwordInput = screen.getByLabelText(/^password$/i)
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i)
    const submitButton = screen.getByRole('button', { name: /sign up/i })
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'StrongPass123!')
    await user.type(confirmPasswordInput, 'StrongPass123!')
    await user.click(submitButton)
    
    expect(emailInput).toBeDisabled()
    expect(passwordInput).toBeDisabled()
    expect(confirmPasswordInput).toBeDisabled()
    expect(submitButton).toBeDisabled()
    expect(screen.getByText(/creating account/i)).toBeInTheDocument()
  })

  it('clears password fields on error', async () => {
    const { authService } = await import('@/services/auth')
    const mockSignup = vi.mocked(authService.signup)
    mockSignup.mockRejectedValueOnce(new Error('Signup failed'))
    
    const user = userEvent.setup()
    renderSignupPage()
    
    const emailInput = screen.getByLabelText(/email/i)
    const passwordInput = screen.getByLabelText(/^password$/i)
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i)
    const submitButton = screen.getByRole('button', { name: /sign up/i })
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'StrongPass123!')
    await user.type(confirmPasswordInput, 'StrongPass123!')
    await user.click(submitButton)
    
    await waitFor(() => {
      expect(passwordInput).toHaveValue('')
      expect(confirmPasswordInput).toHaveValue('')
    })
  })
})