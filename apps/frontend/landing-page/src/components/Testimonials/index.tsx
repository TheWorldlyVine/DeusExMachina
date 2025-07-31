import { useState, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Section, Card } from '@/components/common'
import styles from './Testimonials.module.css'

interface Testimonial {
  id: string
  name: string
  role: string
  avatar: string
  quote: string
  videoUrl?: string
  rating: number
}

const testimonials: Testimonial[] = [
  {
    id: '1',
    name: 'Sarah Chen',
    role: 'Fantasy Novelist',
    avatar: '/images/avatars/sarah-chen.webp',
    quote: "WorldBuilder transformed my writing process. I used to lose track of character relationships and plot points across my 7-book series. Now everything is interconnected and searchable. It's like having a dedicated world bible that updates itself.",
    videoUrl: '/videos/testimonial-sarah.mp4',
    rating: 5
  },
  {
    id: '2',
    name: 'Marcus Rodriguez',
    role: 'D&D Game Master',
    avatar: '/images/avatars/marcus-rodriguez.webp',
    quote: "My players are blown away by the depth of our campaigns now. I can pull up any NPC, location, or lore detail instantly during sessions. The collaborative features let my players contribute to the world between games. Absolute game-changer!",
    videoUrl: '/videos/testimonial-marcus.mp4',
    rating: 5
  },
  {
    id: '3',
    name: 'Elena Volkov',
    role: 'Concept Artist',
    avatar: '/images/avatars/elena-volkov.webp',
    quote: "As a visual creator, I love how WorldBuilder lets me attach artwork to every element. The mood boards and visual references keep my art consistent across projects. Plus, the export features make it easy to share with clients.",
    rating: 5
  }
]

export function Testimonials() {
  const [activeTestimonial, setActiveTestimonial] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const videoRef = useRef<HTMLVideoElement>(null)

  const currentTestimonial = testimonials[activeTestimonial]

  const handleVideoToggle = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause()
      } else {
        videoRef.current.play()
      }
      setIsPlaying(!isPlaying)
    }
  }

  return (
    <Section background="surface" className={styles.testimonials}>
      <div className={styles.header}>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <h2 className={styles.title}>What Creators Say</h2>
          <p className={styles.subtitle}>
            Hear from writers, game masters, and artists who&apos;ve transformed their creative process
          </p>
        </motion.div>
      </div>

      <div className={styles.content}>
        <motion.div 
          className={styles.mainTestimonial}
          initial={{ opacity: 0, x: -20 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <Card variant="elevated" className={styles.testimonialCard}>
            <AnimatePresence mode="wait">
              <motion.div
                key={currentTestimonial.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.3 }}
              >
                {currentTestimonial.videoUrl ? (
                  <div className={styles.videoContainer}>
                    <video
                      ref={videoRef}
                      className={styles.video}
                      src={currentTestimonial.videoUrl}
                      poster={currentTestimonial.avatar}
                      onClick={handleVideoToggle}
                    />
                    <button 
                      className={styles.playButton}
                      onClick={handleVideoToggle}
                      aria-label={isPlaying ? 'Pause video' : 'Play video'}
                    >
                      {isPlaying ? '⏸️' : '▶️'}
                    </button>
                  </div>
                ) : (
                  <div className={styles.quoteContainer}>
                    <div className={styles.quoteIcon}>&ldquo;</div>
                    <blockquote className={styles.quote}>
                      {currentTestimonial.quote}
                    </blockquote>
                  </div>
                )}

                <div className={styles.author}>
                  <img 
                    src={currentTestimonial.avatar}
                    alt={currentTestimonial.name}
                    className={styles.avatar}
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = `https://ui-avatars.com/api/?name=${currentTestimonial.name}&background=4f46e5&color=fff`
                    }}
                  />
                  <div className={styles.authorInfo}>
                    <h4 className={styles.authorName}>{currentTestimonial.name}</h4>
                    <p className={styles.authorRole}>{currentTestimonial.role}</p>
                    <div className={styles.rating}>
                      {[...Array(5)].map((_, i) => (
                        <span key={i} className={styles.star}>⭐</span>
                      ))}
                    </div>
                  </div>
                </div>
              </motion.div>
            </AnimatePresence>
          </Card>
        </motion.div>

        <motion.div 
          className={styles.testimonialList}
          initial={{ opacity: 0, x: 20 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.3 }}
        >
          {testimonials.map((testimonial, index) => (
            <motion.button
              key={testimonial.id}
              className={`${styles.testimonialItem} ${
                index === activeTestimonial ? styles.active : ''
              }`}
              onClick={() => {
                setActiveTestimonial(index)
                setIsPlaying(false)
              }}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <img 
                src={testimonial.avatar}
                alt={testimonial.name}
                className={styles.itemAvatar}
                onError={(e) => {
                  (e.target as HTMLImageElement).src = `https://ui-avatars.com/api/?name=${testimonial.name}&background=4f46e5&color=fff`
                }}
              />
              <div className={styles.itemInfo}>
                <h5 className={styles.itemName}>{testimonial.name}</h5>
                <p className={styles.itemRole}>{testimonial.role}</p>
              </div>
              {testimonial.videoUrl && (
                <span className={styles.videoIndicator}>📹</span>
              )}
            </motion.button>
          ))}
        </motion.div>
      </div>

      <motion.div 
        className={styles.stats}
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.4 }}
      >
        <div className={styles.statItem}>
          <span className={styles.statNumber}>4.9/5</span>
          <span className={styles.statLabel}>Average Rating</span>
        </div>
        <div className={styles.statItem}>
          <span className={styles.statNumber}>50K+</span>
          <span className={styles.statLabel}>Happy Creators</span>
        </div>
        <div className={styles.statItem}>
          <span className={styles.statNumber}>95%</span>
          <span className={styles.statLabel}>Would Recommend</span>
        </div>
      </motion.div>
    </Section>
  )
}