import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Button } from '@/components/common'
import styles from './SignupModal.module.css'

interface SignupModalProps {
  isOpen: boolean
  onClose: () => void
}

export function SignupModal({ isOpen, onClose }: SignupModalProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setIsLoading(true)

    // Simulate API call
    setTimeout(() => {
      setIsLoading(false)
      // In a real app, this would redirect to the app
      alert(`Welcome! Check ${email} for confirmation.`)
      onClose()
    }, 1500)
  }

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            className={styles.backdrop}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.div
            className={styles.modal}
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ duration: 0.2 }}
          >
            <button
              className={styles.closeButton}
              onClick={onClose}
              aria-label="Close modal"
            >
              ✕
            </button>

            <div className={styles.content}>
              <h2 className={styles.title}>Start Building Worlds</h2>
              <p className={styles.subtitle}>
                Join 50,000+ creators. Free forever, no credit card required.
              </p>

              <form onSubmit={handleSubmit} className={styles.form}>
                <div className={styles.inputGroup}>
                  <label htmlFor="email" className={styles.label}>
                    Email
                  </label>
                  <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className={styles.input}
                    placeholder="you@example.com"
                    required
                  />
                </div>

                <div className={styles.inputGroup}>
                  <label htmlFor="password" className={styles.label}>
                    Password
                  </label>
                  <input
                    id="password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className={styles.input}
                    placeholder="At least 8 characters"
                    minLength={8}
                    required
                  />
                </div>

                {error && (
                  <div className={styles.error}>{error}</div>
                )}

                <Button
                  type="submit"
                  size="lg"
                  fullWidth
                  disabled={isLoading}
                  className={styles.submitButton}
                >
                  {isLoading ? 'Creating account...' : 'Create Free Account'}
                </Button>

                <div className={styles.divider}>
                  <span>or</span>
                </div>

                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  fullWidth
                  className={styles.googleButton}
                >
                  <span className={styles.googleIcon}>G</span>
                  Continue with Google
                </Button>

                <p className={styles.terms}>
                  By signing up, you agree to our{' '}
                  <a href="/terms" className={styles.link}>Terms of Service</a>{' '}
                  and{' '}
                  <a href="/privacy" className={styles.link}>Privacy Policy</a>
                </p>
              </form>

              <div className={styles.features}>
                <div className={styles.feature}>
                  <span className={styles.featureIcon}>✓</span>
                  <span>Free forever for your first world</span>
                </div>
                <div className={styles.feature}>
                  <span className={styles.featureIcon}>✓</span>
                  <span>No credit card required</span>
                </div>
                <div className={styles.feature}>
                  <span className={styles.featureIcon}>✓</span>
                  <span>Export your data anytime</span>
                </div>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}