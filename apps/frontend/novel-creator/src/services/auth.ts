import axios from 'axios'
import type { User, LoginCredentials, SignupCredentials, AuthResponse } from '@/types/auth'

const API_URL = import.meta.env.VITE_API_URL || 'https://auth-function-xkv3zhqrha-uw.a.run.app'

// Configure axios defaults for CORS
axios.defaults.withCredentials = true

class AuthService {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await axios.post(`${API_URL}/auth/login`, credentials)
    // Map backend response to frontend format
    return {
      token: response.data.access_token,
      user: {
        id: response.data.user_id,
        email: response.data.user.email,
        name: response.data.user.display_name || response.data.user.email,
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    }
  }

  async signup(credentials: SignupCredentials): Promise<AuthResponse> {
    const response = await axios.post(`${API_URL}/auth/register`, {
      email: credentials.email,
      password: credentials.password,
      displayName: credentials.name
    })
    // Map backend response to frontend format
    return {
      token: response.data.access_token,
      user: {
        id: response.data.user_id,
        email: response.data.user.email,
        name: response.data.user.display_name || response.data.user.email,
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    }
  }

  async logout(): Promise<void> {
    await axios.post(`${API_URL}/auth/logout`)
  }

  async validateToken(token: string): Promise<User> {
    const response = await axios.get(`${API_URL}/auth/validate`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    // Map backend response to frontend format
    return {
      id: response.data.user_id,
      email: response.data.email,
      name: response.data.display_name || response.data.email,
      createdAt: new Date(),
      updatedAt: new Date(),
    }
  }
}

export const authService = new AuthService()