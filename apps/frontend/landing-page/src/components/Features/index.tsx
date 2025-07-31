import { motion } from 'framer-motion'
import { Section, Card } from '@/components/common'
import styles from './Features.module.css'

interface Feature {
  id: string
  title: string
  description: string
  icon: string
  category: 'create' | 'organize' | 'share'
}

const features: Feature[] = [
  {
    id: '1',
    title: 'Interactive World Maps',
    description: 'Create stunning visual maps with layers, regions, and clickable locations. Connect places with roads, rivers, and magical portals.',
    icon: '🗺️',
    category: 'create'
  },
  {
    id: '2',
    title: 'Character Networks',
    description: 'Track complex relationships, alliances, and conflicts. Visualize family trees, organizations, and character arcs.',
    icon: '🕸️',
    category: 'organize'
  },
  {
    id: '3',
    title: 'Timeline Management',
    description: 'Build chronological events across multiple timelines. Track history, prophecies, and parallel storylines.',
    icon: '📅',
    category: 'organize'
  },
  {
    id: '4',
    title: 'Smart Templates',
    description: 'Start with genre-specific templates for fantasy, sci-fi, horror, and more. Customize everything to fit your vision.',
    icon: '📋',
    category: 'create'
  },
  {
    id: '5',
    title: 'Real-time Collaboration',
    description: 'Work together with co-authors, game masters, and players. Control permissions and track changes.',
    icon: '👥',
    category: 'share'
  },
  {
    id: '6',
    title: 'Export Anywhere',
    description: 'Export to PDF, Word, JSON, or publish directly to the web. Your world, your way.',
    icon: '📤',
    category: 'share'
  }
]

const categories = [
  { id: 'create', label: 'Create', icon: '✨' },
  { id: 'organize', label: 'Organize', icon: '📚' },
  { id: 'share', label: 'Share', icon: '🌐' }
]

export function Features() {
  return (
    <Section className={styles.features}>
      <motion.div
        className={styles.header}
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6 }}
      >
        <h2 className={styles.title}>Everything You Need to Build Worlds</h2>
        <p className={styles.subtitle}>
          Powerful tools designed for creators, by creators
        </p>
      </motion.div>

      <div className={styles.categories}>
        {categories.map((category) => (
          <motion.div
            key={category.id}
            className={styles.category}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.1 }}
          >
            <div className={styles.categoryHeader}>
              <span className={styles.categoryIcon}>{category.icon}</span>
              <h3 className={styles.categoryTitle}>{category.label}</h3>
            </div>
            
            <div className={styles.featureList}>
              {features
                .filter(f => f.category === category.id)
                .map((feature, index) => (
                  <motion.div
                    key={feature.id}
                    initial={{ opacity: 0, x: -20 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.4, delay: 0.1 * index }}
                  >
                    <Card className={styles.featureCard} interactive>
                      <div className={styles.featureIcon}>{feature.icon}</div>
                      <div className={styles.featureContent}>
                        <h4 className={styles.featureTitle}>{feature.title}</h4>
                        <p className={styles.featureDescription}>{feature.description}</p>
                      </div>
                    </Card>
                  </motion.div>
                ))}
            </div>
          </motion.div>
        ))}
      </div>

      <motion.div
        className={styles.cta}
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.4 }}
      >
        <p className={styles.ctaText}>And so much more...</p>
        <button className={styles.ctaButton}>
          Explore All Features
        </button>
      </motion.div>
    </Section>
  )
}