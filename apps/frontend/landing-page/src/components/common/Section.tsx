import { HTMLAttributes, forwardRef } from 'react'
import { clsx } from 'clsx'
import styles from './Section.module.css'

export interface SectionProps extends HTMLAttributes<HTMLElement> {
  size?: 'sm' | 'md' | 'lg' | 'xl'
  background?: 'default' | 'surface' | 'primary'
  centered?: boolean
}

export const Section = forwardRef<HTMLElement, SectionProps>(
  ({ 
    className, 
    size = 'md', 
    background = 'default',
    centered = false,
    children, 
    ...props 
  }, ref) => {
    return (
      <section
        ref={ref}
        className={clsx(
          styles.section,
          styles[`size-${size}`],
          styles[`bg-${background}`],
          centered && styles.centered,
          className
        )}
        {...props}
      >
        <div className="container">
          {children}
        </div>
      </section>
    )
  }
)

Section.displayName = 'Section'