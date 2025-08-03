import { Request, Response, NextFunction } from 'express';

export function authMiddleware(req: Request, res: Response, next: NextFunction) {
  // Extract auth token from headers
  const authHeader = req.headers.authorization;
  
  if (authHeader) {
    // Validate format
    if (!authHeader.startsWith('Bearer ')) {
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
  
  next();
}