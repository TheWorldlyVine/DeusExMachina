import { Request, Response, NextFunction } from 'express';

interface RateLimitEntry {
  count: number;
  resetTime: number;
}

// Simple in-memory rate limiter
// In production, use Redis or similar
const rateLimitMap = new Map<string, RateLimitEntry>();

const WINDOW_MS = parseInt(process.env.RATE_LIMIT_WINDOW_MS || '900000'); // 15 minutes
const MAX_REQUESTS = parseInt(process.env.RATE_LIMIT_MAX_REQUESTS || '100');

export function rateLimitMiddleware(req: Request, res: Response, next: NextFunction) {
  // Skip rate limiting for health checks
  if (req.path === '/health') {
    return next();
  }

  // Use IP address as identifier
  const identifier = req.ip || 'unknown';
  const now = Date.now();

  // Get or create rate limit entry
  let entry = rateLimitMap.get(identifier);
  if (!entry || entry.resetTime < now) {
    entry = {
      count: 0,
      resetTime: now + WINDOW_MS,
    };
    rateLimitMap.set(identifier, entry);
  }

  // Increment count
  entry.count++;

  // Check if limit exceeded
  if (entry.count > MAX_REQUESTS) {
    const retryAfter = Math.ceil((entry.resetTime - now) / 1000);
    res.setHeader('Retry-After', retryAfter.toString());
    res.setHeader('X-RateLimit-Limit', MAX_REQUESTS.toString());
    res.setHeader('X-RateLimit-Remaining', '0');
    res.setHeader('X-RateLimit-Reset', entry.resetTime.toString());
    
    return res.status(429).json({
      error: 'Too many requests',
      retryAfter,
    });
  }

  // Set rate limit headers
  res.setHeader('X-RateLimit-Limit', MAX_REQUESTS.toString());
  res.setHeader('X-RateLimit-Remaining', (MAX_REQUESTS - entry.count).toString());
  res.setHeader('X-RateLimit-Reset', entry.resetTime.toString());

  next();
}

// Cleanup old entries periodically
setInterval(() => {
  const now = Date.now();
  for (const [key, entry] of rateLimitMap.entries()) {
    if (entry.resetTime < now) {
      rateLimitMap.delete(key);
    }
  }
}, WINDOW_MS);