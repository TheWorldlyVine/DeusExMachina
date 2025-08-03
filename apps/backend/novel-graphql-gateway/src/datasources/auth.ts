import { BaseAPI } from './base';

export class AuthAPI extends BaseAPI {
  baseURL = process.env.AUTH_SERVICE_URL || 'http://localhost:8080';

  async register(input: {
    email: string;
    password: string;
    displayName: string;
  }) {
    try {
      return await this.post('/auth/register', { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async login(input: { email: string; password: string }) {
    try {
      return await this.post('/auth/login', { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async refreshToken(token: string) {
    try {
      return await this.post('/auth/refresh', { body: { token } });
    } catch (error) {
      this.handleError(error);
    }
  }

  async getUser(userId: string) {
    try {
      return await this.get(`/users/${userId}`);
    } catch (error) {
      this.handleError(error);
    }
  }

  async getCurrentUser() {
    try {
      return await this.get('/auth/me');
    } catch (error) {
      this.handleError(error);
    }
  }

  async logout() {
    try {
      await this.post('/auth/logout');
      return true;
    } catch (error) {
      this.handleError(error);
    }
  }
}