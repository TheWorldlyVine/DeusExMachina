import { useState } from 'react'
import { motion } from 'framer-motion'
import { Section, Card, Button } from '@/components/common'
import styles from './Pricing.module.css'

interface PricingTier {
  id: string
  name: string
  price: number
  period: string
  description: string
  features: string[]
  highlighted?: boolean
  cta: string
}

const pricingTiers: PricingTier[] = [
  {
    id: 'free',
    name: 'Creator',
    price: 0,
    period: 'forever',
    description: 'Perfect for getting started with your first world',
    features: [
      '1 world with unlimited depth',
      '100MB storage',
      'Basic templates',
      'Export to PDF & JSON',
      'Community support',
      'Mobile access'
    ],
    cta: 'Start Free'
  },
  {
    id: 'pro',
    name: 'Professional',
    price: 12,
    period: 'month',
    description: 'For serious creators and game masters',
    features: [
      'Unlimited worlds',
      '10GB storage',
      'Premium templates',
      'All export formats',
      'Real-time collaboration',
      'Priority support',
      'API access',
      'Custom themes'
    ],
    highlighted: true,
    cta: 'Start Free Trial'
  },
  {
    id: 'team',
    name: 'Studio',
    price: 39,
    period: 'month',
    description: 'For teams and professional studios',
    features: [
      'Everything in Professional',
      '100GB storage per user',
      'Up to 20 team members',
      'Advanced permissions',
      'Custom branding',
      'Dedicated support',
      'Training sessions',
      'SLA guarantee'
    ],
    cta: 'Contact Sales'
  }
]

export function Pricing() {
  const [billingPeriod, setBillingPeriod] = useState<'monthly' | 'yearly'>('monthly')

  const getPrice = (tier: PricingTier) => {
    if (tier.price === 0) return tier.price
    return billingPeriod === 'yearly' ? Math.floor(tier.price * 0.8) : tier.price
  }

  return (
    <Section background="surface" className={styles.pricing}>
      <motion.div
        className={styles.header}
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6 }}
      >
        <h2 className={styles.title}>Simple, Transparent Pricing</h2>
        <p className={styles.subtitle}>
          Start free, upgrade when you need more. No hidden fees, cancel anytime.
        </p>

        <div className={styles.billingToggle}>
          <button
            className={`${styles.toggleOption} ${
              billingPeriod === 'monthly' ? styles.active : ''
            }`}
            onClick={() => setBillingPeriod('monthly')}
          >
            Monthly
          </button>
          <button
            className={`${styles.toggleOption} ${
              billingPeriod === 'yearly' ? styles.active : ''
            }`}
            onClick={() => setBillingPeriod('yearly')}
          >
            Yearly
            <span className={styles.discount}>Save 20%</span>
          </button>
        </div>
      </motion.div>

      <motion.div
        className={styles.tiers}
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.2 }}
      >
        {pricingTiers.map((tier, index) => (
          <motion.div
            key={tier.id}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.4, delay: 0.1 * index }}
          >
            <Card
              variant={tier.highlighted ? 'elevated' : 'default'}
              className={`${styles.tierCard} ${
                tier.highlighted ? styles.highlighted : ''
              }`}
            >
              {tier.highlighted && (
                <div className={styles.popularBadge}>Most Popular</div>
              )}
              
              <div className={styles.tierHeader}>
                <h3 className={styles.tierName}>{tier.name}</h3>
                <p className={styles.tierDescription}>{tier.description}</p>
                
                <div className={styles.priceContainer}>
                  <span className={styles.currency}>$</span>
                  <span className={styles.price}>{getPrice(tier)}</span>
                  <span className={styles.period}>
                    {tier.price === 0 ? 'forever' : `/${tier.period}`}
                  </span>
                </div>
                
                {billingPeriod === 'yearly' && tier.price > 0 && (
                  <p className={styles.yearlyPrice}>
                    ${tier.price * 12 * 0.8} billed annually
                  </p>
                )}
              </div>

              <ul className={styles.features}>
                {tier.features.map((feature, i) => (
                  <li key={i} className={styles.feature}>
                    <span className={styles.featureIcon}>✓</span>
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>

              <div className={styles.tierFooter}>
                <Button
                  variant={tier.highlighted ? 'primary' : 'outline'}
                  size="lg"
                  fullWidth
                  className={styles.ctaButton}
                >
                  {tier.cta}
                </Button>
                
                {tier.id === 'pro' && (
                  <p className={styles.trialText}>14-day free trial, no credit card required</p>
                )}
              </div>
            </Card>
          </motion.div>
        ))}
      </motion.div>

      <motion.div
        className={styles.faq}
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.4 }}
      >
        <h3 className={styles.faqTitle}>Frequently Asked Questions</h3>
        
        <div className={styles.faqGrid}>
          <div className={styles.faqItem}>
            <h4>Can I change plans anytime?</h4>
            <p>Yes! Upgrade, downgrade, or cancel anytime. No questions asked.</p>
          </div>
          <div className={styles.faqItem}>
            <h4>What happens to my data if I downgrade?</h4>
            <p>Your worlds remain safe. You&apos;ll have read-only access to worlds that exceed your plan limits.</p>
          </div>
          <div className={styles.faqItem}>
            <h4>Do you offer educational discounts?</h4>
            <p>Yes! Students and educators get 50% off all paid plans. Contact us for details.</p>
          </div>
          <div className={styles.faqItem}>
            <h4>Is there an API or integration options?</h4>
            <p>Professional and Studio plans include API access for custom integrations.</p>
          </div>
        </div>
      </motion.div>

      <motion.div
        className={styles.enterprise}
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.5 }}
      >
        <Card variant="elevated" className={styles.enterpriseCard}>
          <h3>Need something custom?</h3>
          <p>For large teams, custom features, or on-premise deployment</p>
          <Button variant="primary" size="lg">
            Contact Enterprise Sales
          </Button>
        </Card>
      </motion.div>
    </Section>
  )
}