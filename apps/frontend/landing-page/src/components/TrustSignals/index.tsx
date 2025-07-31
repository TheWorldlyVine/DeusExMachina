import { motion } from 'framer-motion'
import { Section } from '@/components/common'
import styles from './TrustSignals.module.css'

const trustBadges = [
  { id: '1', name: 'SSL Secured', icon: '🔒' },
  { id: '2', name: 'GDPR Compliant', icon: '🛡️' },
  { id: '3', name: 'SOC 2 Type II', icon: '✓' },
  { id: '4', name: '99.9% Uptime', icon: '⚡' }
]

const awards = [
  { id: '1', name: 'Best Creative Tool 2024', org: 'TechCrunch' },
  { id: '2', name: 'Editor\'s Choice', org: 'ProductHunt' },
  { id: '3', name: 'Top RPG Tool', org: 'ENnie Awards' }
]

export function TrustSignals() {
  return (
    <Section className={styles.trustSignals}>
      <motion.div
        className={styles.header}
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6 }}
      >
        <h2 className={styles.title}>Trusted by Thousands</h2>
        <p className={styles.subtitle}>
          Join a growing community of creators who trust us with their worlds
        </p>
      </motion.div>

      <div className={styles.content}>
        <motion.div
          className={styles.statsGrid}
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <div className={styles.statCard}>
            <span className={styles.statIcon}>🌍</span>
            <span className={styles.statNumber}>1M+</span>
            <span className={styles.statLabel}>Worlds Created</span>
          </div>
          <div className={styles.statCard}>
            <span className={styles.statIcon}>👥</span>
            <span className={styles.statNumber}>50K+</span>
            <span className={styles.statLabel}>Active Creators</span>
          </div>
          <div className={styles.statCard}>
            <span className={styles.statIcon}>📚</span>
            <span className={styles.statNumber}>10M+</span>
            <span className={styles.statLabel}>Story Elements</span>
          </div>
          <div className={styles.statCard}>
            <span className={styles.statIcon}>🌟</span>
            <span className={styles.statNumber}>4.9/5</span>
            <span className={styles.statLabel}>User Rating</span>
          </div>
        </motion.div>

        <motion.div
          className={styles.security}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.3 }}
        >
          <h3 className={styles.sectionTitle}>Your Data is Safe</h3>
          <p className={styles.sectionSubtitle}>
            We take security and privacy seriously. Your worlds belong to you.
          </p>
          
          <div className={styles.badges}>
            {trustBadges.map((badge) => (
              <div key={badge.id} className={styles.badge}>
                <span className={styles.badgeIcon}>{badge.icon}</span>
                <span className={styles.badgeName}>{badge.name}</span>
              </div>
            ))}
          </div>

          <div className={styles.guarantees}>
            <div className={styles.guarantee}>
              <span className={styles.checkmark}>✓</span>
              <span>You own all your content - export anytime</span>
            </div>
            <div className={styles.guarantee}>
              <span className={styles.checkmark}>✓</span>
              <span>Bank-level encryption for all data</span>
            </div>
            <div className={styles.guarantee}>
              <span className={styles.checkmark}>✓</span>
              <span>No AI training on your creative work</span>
            </div>
            <div className={styles.guarantee}>
              <span className={styles.checkmark}>✓</span>
              <span>Regular automated backups</span>
            </div>
          </div>
        </motion.div>

        <motion.div
          className={styles.awards}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.4 }}
        >
          <h3 className={styles.sectionTitle}>Award-Winning Platform</h3>
          <div className={styles.awardsList}>
            {awards.map((award) => (
              <div key={award.id} className={styles.award}>
                <div className={styles.awardIcon}>🏆</div>
                <div className={styles.awardInfo}>
                  <h4 className={styles.awardName}>{award.name}</h4>
                  <p className={styles.awardOrg}>{award.org}</p>
                </div>
              </div>
            ))}
          </div>
        </motion.div>

        <motion.div
          className={styles.companies}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.5 }}
        >
          <p className={styles.companiesTitle}>Trusted by creators at</p>
          <div className={styles.companyLogos}>
            <div className={styles.companyLogo}>Penguin Random House</div>
            <div className={styles.companyLogo}>Wizards of the Coast</div>
            <div className={styles.companyLogo}>Netflix</div>
            <div className={styles.companyLogo}>Ubisoft</div>
            <div className={styles.companyLogo}>Marvel Studios</div>
          </div>
        </motion.div>
      </div>
    </Section>
  )
}