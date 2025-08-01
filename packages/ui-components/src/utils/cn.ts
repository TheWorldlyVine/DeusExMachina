import clsx, { ClassValue } from 'clsx';

/**
 * Utility function to merge class names using clsx.
 * Combines multiple class values into a single string.
 */
export function cn(...inputs: ClassValue[]): string {
  return clsx(inputs);
}