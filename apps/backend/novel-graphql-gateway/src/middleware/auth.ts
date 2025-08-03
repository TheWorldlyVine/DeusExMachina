import { Request, Response, NextFunction } from 'express';

export function authMiddleware(req: Request, res: Response, next: NextFunction) {
  console.log('Auth Middleware - Method:', req.method);
  console.log('Auth Middleware - Headers:', Object.keys(req.headers));
  
  // Extract auth token from headers
  const authHeader = req.headers.authorization;
  
  console.log('Auth Middleware - Auth header:', authHeader ? 'Present' : 'Missing');
  
  if (authHeader) {
    // Validate format
    if (!authHeader.startsWith('Bearer ')) {
      console.log('Auth Middleware - Invalid format, not Bearer');
      return res.status(401).json({ error: 'Invalid authorization header format' });
    }
    
    // Token validation will be handled in context
    // This middleware just ensures proper header format
  }
  
  // Extract project ID if provided
  const projectId = req.headers['x-project-id'];
  if (projectId && typeof projectId !== 'string') {
    return res.status(400).json({ error: 'Invalid X-Project-ID header' });
  }
  
  console.log('Auth Middleware - Passing to next');
  next();
}