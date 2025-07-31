import { useEffect, lazy, Suspense } from 'react'
import { useThemeStore } from '@/store/themeStore'
import { Hero } from '@/components/Hero'
import { ThemeToggle } from '@/components/common/ThemeToggle'

// Lazy load components below the fold
const Features = lazy(() => import('@/components/Features').then(m => ({ default: m.Features })))
const CommunityShowcase = lazy(() => import('@/components/CommunityShowcase').then(m => ({ default: m.CommunityShowcase })))
const Testimonials = lazy(() => import('@/components/Testimonials').then(m => ({ default: m.Testimonials })))
const TrustSignals = lazy(() => import('@/components/TrustSignals').then(m => ({ default: m.TrustSignals })))
const Pricing = lazy(() => import('@/components/Pricing').then(m => ({ default: m.Pricing })))
const Footer = lazy(() => import('@/components/Footer').then(m => ({ default: m.Footer })))

function App() {
  const { theme, initTheme } = useThemeStore()

  useEffect(() => {
    initTheme()
  }, [initTheme])

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  return (
    <div className="app">
      <ThemeToggle />
      <Hero />
      <Suspense fallback={<div style={{ height: '100vh' }} />}>
        <Features />
        <CommunityShowcase />
        <Testimonials />
        <TrustSignals />
        <Pricing />
        <Footer />
      </Suspense>
    </div>
  )
}

export default App