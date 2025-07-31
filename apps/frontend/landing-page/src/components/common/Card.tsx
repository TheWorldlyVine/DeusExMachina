import { HTMLAttributes, forwardRef } from 'react'
import { clsx } from 'clsx'
import styles from './Card.module.css'

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'outlined' | 'elevated'
  padding?: 'none' | 'sm' | 'md' | 'lg'
  interactive?: boolean
}

export const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ 
    className, 
    variant = 'default',
    padding = 'md',
    interactive = false,
    children, 
    ...props 
  }, ref) => {
    return (
      <div
        ref={ref}
        className={clsx(
          styles.card,
          styles[variant],
          styles[`padding-${padding}`],
          interactive && styles.interactive,
          className
        )}
        {...props}
      >
        {children}
      </div>
    )
  }
)

Card.displayName = 'Card'