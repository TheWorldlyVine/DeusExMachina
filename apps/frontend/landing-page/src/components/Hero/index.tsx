import { useState } from 'react'
import { motion } from 'framer-motion'
import { Section, Button } from '@/components/common'
import { InteractiveDemo } from './InteractiveDemo'
import { SignupModal } from '@/components/SignupModal'
import styles from './Hero.module.css'

export function Hero() {
  const [showDemo, setShowDemo] = useState(false)
  const [showSignup, setShowSignup] = useState(false)

  return (
    <Section size="xl" className={styles.hero}>
      <div className={styles.content}>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className={styles.textContent}
        >
          <h1 className={styles.title}>
            Build Immersive Worlds
            <span className={styles.titleGradient}> That Come Alive</span>
          </h1>
          <p className={styles.subtitle}>
            The ultimate world-building software for novelists, game masters, and creative professionals. 
            Organize complex narratives, visualize your creations, and bring your imagination to life.
          </p>
          
          <div className={styles.stats}>
            <div className={styles.stat}>
              <span className={styles.statNumber}>50K+</span>
              <span className={styles.statLabel}>Active Creators</span>
            </div>
            <div className={styles.stat}>
              <span className={styles.statNumber}>1M+</span>
              <span className={styles.statLabel}>Worlds Created</span>
            </div>
            <div className={styles.stat}>
              <span className={styles.statNumber}>4.9</span>
              <span className={styles.statLabel}>User Rating</span>
            </div>
          </div>

          <div className={styles.actions}>
            <Button 
              size="lg" 
              onClick={() => {
                setShowDemo(true)
                setShowSignup(true)
              }}
              className={styles.primaryCta}
            >
              Start Building Worlds
            </Button>
            <Button 
              size="lg" 
              variant="outline"
              className={styles.secondaryCta}
              onClick={() => setShowDemo(true)}
            >
              Watch Demo
            </Button>
          </div>

          <p className={styles.freeText}>
            ✨ Free forever for your first world • No credit card required
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className={styles.demoContainer}
        >
          <InteractiveDemo isActive={showDemo} />
        </motion.div>
      </div>
      
      <SignupModal isOpen={showSignup} onClose={() => setShowSignup(false)} />
    </Section>
  )
}