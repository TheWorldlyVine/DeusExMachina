// API Configuration for different environments

const API_ENDPOINTS = {
  development: {
    auth: 'http://localhost:8080',
    api: 'http://localhost:8081',
    processor: 'http://localhost:8082',
  },
  production: {
    auth: 'https://auth-function-tbmcifixdq-uc.a.run.app',
    api: 'https://api-function-tbmcifixdq-uc.a.run.app',
    processor: 'https://processor-function-tbmcifixdq-uc.a.run.app',
  }
};

export const getApiUrl = (service: 'auth' | 'api' | 'processor') => {
  const env = import.meta.env.PROD ? 'production' : 'development';
  return API_ENDPOINTS[env][service];
};

export const AUTH_API_URL = import.meta.env.VITE_AUTH_API_URL || getApiUrl('auth');
export const API_URL = import.meta.env.VITE_API_URL || getApiUrl('api');
export const PROCESSOR_URL = import.meta.env.VITE_PROCESSOR_URL || getApiUrl('processor');