import { Request } from 'express';
import jwt from 'jsonwebtoken';
import { AuthenticationError } from './utils/errors';

export interface User {
  id: string;
  email: string;
  displayName?: string;
  role: 'FREE' | 'PREMIUM' | 'ADMIN';
}

export interface Context {
  user?: User;
  req: Request;
  projectId?: string;
}

interface JwtPayload {
  sub: string;  // user ID
  email: string;
  displayName?: string;
  email_verified?: boolean;
  auth_provider?: string;
  roles?: string[];  // Changed from role to roles array
  iss: string;
  aud: string;
  exp: number;
  iat: number;
}

export async function context({ req, connectionParams }: { req?: Request; connectionParams?: any }): Promise<Context> {
  let user: User | undefined;

  // Get auth token from request headers or connection params (for subscriptions)
  const authHeader = req?.headers.authorization || connectionParams?.authorization;
  
  if (authHeader) {
    const token = authHeader.replace('Bearer ', '');
    
    try {
      // Verify JWT token from our auth service
      // In production, the JWT_SECRET should be the same one used by the auth service
      const decoded = jwt.verify(token, process.env.JWT_SECRET || 'development-secret', {
        issuer: 'deusexmachina-auth',
        audience: 'deusexmachina-client',
      }) as JwtPayload;
      
      user = {
        id: decoded.sub,
        email: decoded.email,
        displayName: decoded.displayName,
        // Map roles array to single role - check for admin/premium roles
        role: (decoded.roles?.includes('admin') ? 'ADMIN' : 
               decoded.roles?.includes('premium') ? 'PREMIUM' : 'FREE') as 'FREE' | 'PREMIUM' | 'ADMIN',
      };
    } catch (error) {
      // Invalid token, but don't throw - some queries might be public
      console.warn('Invalid auth token:', error);
    }
  }

  // Extract project ID from headers if provided
  const projectId = req?.headers['x-project-id'] as string | undefined;

  return {
    user,
    req: req!,
    projectId,
  };
}

// Helper function to require authentication
export function requireAuth(context: Context): User {
  if (!context.user) {
    throw new AuthenticationError('Authentication required');
  }
  return context.user;
}

// Helper function to require specific role
export function requireRole(context: Context, roles: string[]): User {
  const user = requireAuth(context);
  if (!roles.includes(user.role)) {
    throw new AuthenticationError('Insufficient permissions');
  }
  return user;
}

// Helper function to check project access
export async function checkProjectAccess(context: Context, projectId: string, minRole: 'VIEWER' | 'EDITOR' | 'OWNER' = 'VIEWER'): Promise<void> {
  const user = requireAuth(context);
  
  // Admin users have access to all projects
  if (user.role === 'ADMIN') {
    return;
  }

  // TODO: Implement actual project access check against database
  // For now, we'll assume the user has access if they're authenticated
  // In production, this should query the project collaborators
}