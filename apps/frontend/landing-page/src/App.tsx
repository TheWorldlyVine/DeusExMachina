import { useEffect } from 'react'
import { useThemeStore } from '@/store/themeStore'
import { Hero } from '@/components/Hero'
import { Features } from '@/components/Features'
import { CommunityShowcase } from '@/components/CommunityShowcase'
import { Testimonials } from '@/components/Testimonials'
import { TrustSignals } from '@/components/TrustSignals'
import { Pricing } from '@/components/Pricing'
import { Footer } from '@/components/Footer'
import { ThemeToggle } from '@/components/common/ThemeToggle'

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
      <Features />
      <CommunityShowcase />
      <Testimonials />
      <TrustSignals />
      <Pricing />
      <Footer />
    </div>
  )
}

export default App