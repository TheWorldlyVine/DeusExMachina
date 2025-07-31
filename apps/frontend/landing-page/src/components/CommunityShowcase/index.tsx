import { useState } from 'react'
import { motion } from 'framer-motion'
import { Section, Card } from '@/components/common'
import styles from './CommunityShowcase.module.css'

interface ShowcaseItem {
  id: string
  title: string
  author: string
  type: 'map' | 'character' | 'story'
  genre: string
  imageUrl: string
  likes: number
  views: number
}

const showcaseData: ShowcaseItem[] = [
  {
    id: '1',
    title: 'The Shattered Realms',
    author: 'Elena Stormwind',
    type: 'map',
    genre: 'High Fantasy',
    imageUrl: '/images/showcase/shattered-realms.webp',
    likes: 1247,
    views: 5832
  },
  {
    id: '2',
    title: 'Captain Vex Shadowbane',
    author: 'Marcus Chen',
    type: 'character',
    genre: 'Space Opera',
    imageUrl: '/images/showcase/vex-shadowbane.webp',
    likes: 892,
    views: 3421
  },
  {
    id: '3',
    title: 'The Last Convergence',
    author: 'Sarah Mitchell',
    type: 'story',
    genre: 'Urban Fantasy',
    imageUrl: '/images/showcase/last-convergence.webp',
    likes: 2103,
    views: 8945
  },
  {
    id: '4',
    title: 'Kingdoms of Aethermoor',
    author: 'David Frost',
    type: 'map',
    genre: 'Epic Fantasy',
    imageUrl: '/images/showcase/aethermoor.webp',
    likes: 1576,
    views: 6234
  },
  {
    id: '5',
    title: 'Dr. Lyra Quantum',
    author: 'Alex Rivera',
    type: 'character',
    genre: 'Sci-Fi',
    imageUrl: '/images/showcase/dr-quantum.webp',
    likes: 734,
    views: 2890
  },
  {
    id: '6',
    title: 'Chronicles of the Void',
    author: 'Jamie Park',
    type: 'story',
    genre: 'Dark Fantasy',
    imageUrl: '/images/showcase/void-chronicles.webp',
    likes: 1892,
    views: 7123
  }
]

const filterOptions = [
  { value: 'all', label: 'All' },
  { value: 'map', label: 'Maps' },
  { value: 'character', label: 'Characters' },
  { value: 'story', label: 'Stories' }
]

export function CommunityShowcase() {
  const [selectedFilter, setSelectedFilter] = useState('all')
  const [hoveredItem, setHoveredItem] = useState<string | null>(null)

  const filteredItems = showcaseData.filter(
    item => selectedFilter === 'all' || item.type === selectedFilter
  )

  const getTypeIcon = (type: ShowcaseItem['type']) => {
    switch (type) {
      case 'map': return '🗺️'
      case 'character': return '👤'
      case 'story': return '📖'
    }
  }

  return (
    <Section className={styles.showcase}>
      <div className={styles.header}>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <h2 className={styles.title}>Community Showcase</h2>
          <p className={styles.subtitle}>
            Explore incredible worlds created by our vibrant community of creators
          </p>
        </motion.div>

        <motion.div
          className={styles.filters}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.1 }}
        >
          {filterOptions.map(option => (
            <button
              key={option.value}
              className={`${styles.filterButton} ${
                selectedFilter === option.value ? styles.active : ''
              }`}
              onClick={() => setSelectedFilter(option.value)}
            >
              {option.label}
            </button>
          ))}
        </motion.div>
      </div>

      <motion.div 
        className={styles.grid}
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.2 }}
      >
        {filteredItems.map((item, index) => (
          <motion.div
            key={item.id}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.4, delay: index * 0.1 }}
          >
            <Card
              interactive
              className={styles.showcaseCard}
              onMouseEnter={() => setHoveredItem(item.id)}
              onMouseLeave={() => setHoveredItem(null)}
            >
              <div className={styles.imageContainer}>
                <div 
                  className={styles.image}
                  style={{ 
                    backgroundImage: `url(${item.imageUrl})`,
                    backgroundColor: '#374151' // Fallback color
                  }}
                />
                <div className={styles.typeTag}>
                  <span className={styles.typeIcon}>{getTypeIcon(item.type)}</span>
                  <span>{item.type}</span>
                </div>
                {hoveredItem === item.id && (
                  <motion.div
                    className={styles.overlay}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                  >
                    <button className={styles.viewButton}>View Details</button>
                  </motion.div>
                )}
              </div>
              
              <div className={styles.cardContent}>
                <h3 className={styles.itemTitle}>{item.title}</h3>
                <p className={styles.author}>by {item.author}</p>
                <p className={styles.genre}>{item.genre}</p>
                
                <div className={styles.stats}>
                  <span className={styles.stat}>
                    <span className={styles.statIcon}>❤️</span>
                    {item.likes.toLocaleString()}
                  </span>
                  <span className={styles.stat}>
                    <span className={styles.statIcon}>👁️</span>
                    {item.views.toLocaleString()}
                  </span>
                </div>
              </div>
            </Card>
          </motion.div>
        ))}
      </motion.div>

      <motion.div
        className={styles.cta}
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.4 }}
      >
        <p className={styles.ctaText}>
          Join thousands of creators sharing their worlds
        </p>
        <button className={styles.ctaButton}>
          Explore All Showcases
        </button>
      </motion.div>
    </Section>
  )
}