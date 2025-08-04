import { RESTDataSource } from '@apollo/datasource-rest';
import { Context } from '../context';

type RequestOptions = {
  headers: Record<string, string>;
  params?: URLSearchParams;
  body?: any;
};

export abstract class BaseAPI extends RESTDataSource {
  abstract baseURL: string;
  protected context: Context;
  protected cache?: any;

  constructor(options: { cache?: any; context: Context }) {
    super();
    // Cache is optional in Apollo Server 4
    if (options.cache) {
      this.cache = options.cache;
    }
    this.context = options.context;
  }

  override willSendRequest(path: string, request: RequestOptions) {
    // Add common headers
    request.headers['Content-Type'] = 'application/json';
    
    // Forward user context if available
    const context = this.context;
    if (context?.user) {
      request.headers['X-User-ID'] = context.user.id;
      // Forward the original Firebase token to backend services
      const authHeader = context.req.headers.authorization;
      if (authHeader) {
        request.headers['Authorization'] = authHeader;
      }
    }
    
    if (context?.projectId) {
      request.headers['X-Project-ID'] = context.projectId;
    }
  }

  // Helper method to handle errors
  protected handleError(error: any): never {
    console.error('API Error:', error);
    
    if (error.extensions?.response) {
      const { status, body } = error.extensions.response;
      
      if (status === 401) {
        throw new Error('Unauthorized');
      } else if (status === 403) {
        throw new Error('Forbidden');
      } else if (status === 404) {
        throw new Error('Not found');
      } else if (body?.error) {
        throw new Error(body.error);
      }
    }
    
    throw error;
  }
}