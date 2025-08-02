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
}

export interface LoginData {
  email: string
  password: string
}

class AuthService {
  private baseURL = AUTH_API_URL

  async signup(data: SignupData): Promise<AuthResponse> {
    const response = await axios.post<AuthResponse>(`${this.baseURL}/auth/register`, data)
    return response.data
  }

  async login(data: LoginData): Promise<AuthResponse> {
    const response = await axios.post<AuthResponse>(`${this.baseURL}/auth/login`, data)
    return response.data
  }

  async logout(): Promise<void> {
    await axios.post(`${this.baseURL}/auth/logout`)
  }

  async verifyEmail(token: string): Promise<void> {
    await axios.post(`${this.baseURL}/auth/verify-email`, { token })
  }

  async resendVerificationEmail(): Promise<void> {
    await axios.post(`${this.baseURL}/auth/resend-verification`)
  }

  async requestPasswordReset(email: string): Promise<void> {
    await axios.post(`${this.baseURL}/auth/reset-password`, { email })
  }

  async resetPassword(token: string, password: string): Promise<void> {
    await axios.post(`${this.baseURL}/auth/confirm-reset`, { token, newPassword: password })
  }
}

export const authService = new AuthService()