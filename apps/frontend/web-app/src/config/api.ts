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

// Use production URLs directly since environment variables aren't being picked up
export const AUTH_API_URL = 'https://auth-function-tbmcifixdq-uc.a.run.app';
export const API_URL = 'https://api-function-tbmcifixdq-uc.a.run.app';
export const PROCESSOR_URL = 'https://processor-function-tbmcifixdq-uc.a.run.app';