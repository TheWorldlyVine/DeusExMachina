import axios from 'axios'
import { AUTH_API_URL } from '@/config/api'

export interface User {
  id: string
  email: string
}

export interface AuthResponse {
  user: User
  token: string
}

export interface SignupData {
  email: string
  password: string
  displayName: string
  acceptedTerms: boolean
}

export interface LoginData {
  email: string
  password: string
}

class AuthService {
  constructor() {
    // Bind methods to preserve 'this' context
    this.signup = this.signup.bind(this)
    this.login = this.login.bind(this)
    this.logout = this.logout.bind(this)
    this.verifyEmail = this.verifyEmail.bind(this)
    this.resendVerificationEmail = this.resendVerificationEmail.bind(this)
    this.requestPasswordReset = this.requestPasswordReset.bind(this)
    this.resetPassword = this.resetPassword.bind(this)
  }

  private getBaseURL() {
    return AUTH_API_URL
  }

  async signup(data: SignupData): Promise<AuthResponse> {
    const url = `${this.getBaseURL()}/auth/register`
    console.log('Signup URL:', url) // Debug logging
    const response = await axios.post<AuthResponse>(url, data)
    return response.data
  }

  async login(data: LoginData): Promise<AuthResponse> {
    const url = `${this.getBaseURL()}/auth/login`
    const response = await axios.post<AuthResponse>(url, data)
    return response.data
  }

  async logout(): Promise<void> {
    const url = `${this.getBaseURL()}/auth/logout`
    await axios.post(url)
  }

  async verifyEmail(token: string): Promise<void> {
    const url = `${this.getBaseURL()}/auth/verify-email`
    await axios.post(url, { token })
  }

  async resendVerificationEmail(): Promise<void> {
    const url = `${this.getBaseURL()}/auth/resend-verification`
    await axios.post(url)
  }

  async requestPasswordReset(email: string): Promise<void> {
    const url = `${this.getBaseURL()}/auth/reset-password`
    await axios.post(url, { email })
  }

  async resetPassword(token: string, password: string): Promise<void> {
    const url = `${this.getBaseURL()}/auth/confirm-reset`
    await axios.post(url, { token, newPassword: password })
  }
}

export const authService = new AuthService()