import axios from 'axios'
import type { User, LoginCredentials, SignupCredentials, AuthResponse } from '@/types/auth'

const API_URL = import.meta.env.VITE_API_URL || '/api'

class AuthService {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await axios.post(`${API_URL}/auth/login`, credentials)
    return response.data
  }

  async signup(credentials: SignupCredentials): Promise<AuthResponse> {
    const response = await axios.post(`${API_URL}/auth/signup`, credentials)
    return response.data
  }

  async logout(): Promise<void> {
    await axios.post(`${API_URL}/auth/logout`)
  }

  async validateToken(token: string): Promise<User> {
    const response = await axios.get(`${API_URL}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    return response.data
  }
}

export const authService = new AuthService()