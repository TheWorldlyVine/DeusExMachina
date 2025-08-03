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
  userId: string;
  email: string;
  displayName?: string;
  role: 'FREE' | 'PREMIUM' | 'ADMIN';
}

export async function context({ req, connectionParams }: { req?: Request; connectionParams?: any }): Promise<Context> {
  let user: User | undefined;

  // Get auth token from request headers or connection params (for subscriptions)
  const authHeader = req?.headers.authorization || connectionParams?.authorization;
  
  if (authHeader) {
    const token = authHeader.replace('Bearer ', '');
    
    try {
      const decoded = jwt.verify(token, process.env.JWT_SECRET!) as JwtPayload;
      user = {
        id: decoded.userId,
        email: decoded.email,
        displayName: decoded.displayName,
        role: decoded.role,
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