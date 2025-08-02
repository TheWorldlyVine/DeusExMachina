// API Configuration for different environments

// Get environment variables with fallbacks
const getEnvVar = (key: string, fallback: string): string => {
  return import.meta.env[key] || fallback;
};

// Use Vite environment variables if available, otherwise use defaults
export const AUTH_API_URL = getEnvVar(
  'VITE_AUTH_API_URL',
  import.meta.env.PROD
    ? 'https://auth-function-tbmcifixdq-uc.a.run.app'
    : 'http://localhost:8080'
);

export const API_URL = getEnvVar(
  'VITE_API_URL',
  import.meta.env.PROD
    ? 'https://api-function-tbmcifixdq-uc.a.run.app'
    : 'http://localhost:8081'
);

export const PROCESSOR_URL = getEnvVar(
  'VITE_PROCESSOR_URL',
  import.meta.env.PROD
    ? 'https://processor-function-tbmcifixdq-uc.a.run.app'
    : 'http://localhost:8082'
);

// Helper function for backward compatibility
export const getApiUrl = (service: 'auth' | 'api' | 'processor') => {
  switch (service) {
    case 'auth':
      return AUTH_API_URL;
    case 'api':
      return API_URL;
    case 'processor':
      return PROCESSOR_URL;
  }
};