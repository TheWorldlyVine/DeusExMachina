# WorldBuilder Landing Page

High-converting landing page for the WorldBuilder platform, designed to achieve 25%+ visitor-to-signup conversion rates.

## Overview

This landing page is built with React, TypeScript, and Vite, featuring:
- 🌓 Dark/light theme support with system preference detection
- 🎯 Interactive world-building demo
- 📱 Mobile-first responsive design
- ⚡ Optimized for Core Web Vitals
- 🎨 Smooth animations with Framer Motion
- 🔐 Privacy-compliant analytics ready

## Development

```bash
# Install dependencies
pnpm install

# Start development server
pnpm dev

# Build for production
pnpm build

# Run tests
pnpm test

# Type checking
pnpm type-check

# Linting
pnpm lint
```

## Project Structure

```
src/
├── components/
│   ├── common/          # Reusable UI components
│   ├── Hero/           # Hero section with interactive demo
│   ├── Features/       # Feature showcase
│   ├── CommunityShowcase/  # User-generated content gallery
│   ├── Testimonials/   # Video testimonials
│   ├── TrustSignals/   # Social proof elements
│   ├── Pricing/        # Pricing calculator
│   └── Footer/         # Site footer
├── hooks/              # Custom React hooks
├── store/              # Zustand state management
├── styles/             # Global styles and themes
├── types/              # TypeScript type definitions
├── utils/              # Utility functions
└── test/               # Test setup and utilities
```

## Key Features Implemented

### Theme System
- CSS variables for consistent theming
- Automatic dark/light mode detection
- Persistent theme preference
- Smooth transitions between themes

### Interactive Demo
- Live world-building preview without signup
- Interactive node-based map visualization
- Connection system between entities
- Animated transitions and hover effects

### Responsive Design
- Mobile-first approach
- Touch-friendly CTAs (48px minimum)
- Adaptive typography and spacing
- Optimized for all screen sizes

### Performance
- Code splitting for optimal bundle sizes
- Lazy loading for below-fold content
- Optimized images and assets
- Target metrics: LCP < 2.5s, INP < 200ms

## Deployment

Deploy to Google Cloud Storage:

```bash
# From project root
./deploy-landing-page.sh
```

The script will:
1. Build the production bundle
2. Create/update GCS bucket
3. Upload files with appropriate cache headers
4. Provide the public URL

## Testing

```bash
# Run unit tests
pnpm test

# Run tests with UI
pnpm test:ui

# Generate coverage report
pnpm test:coverage
```

## Performance Optimization

- Images: Use WebP format with JPEG fallbacks
- Fonts: System font stack for instant rendering
- Scripts: Minimal third-party dependencies
- Styles: CSS-in-JS avoided for better performance

## Future Enhancements

- [ ] A/B testing framework integration
- [ ] Advanced analytics implementation
- [ ] Service worker for offline support
- [ ] Integration with backend APIs
- [ ] Enhanced SEO with structured data
- [ ] Multi-language support

## Contributing

Follow the project's coding standards:
- Write tests for all new components
- Maintain 80%+ code coverage
- Use TypeScript strictly (no `any`)
- Follow responsive design principles
- Optimize for performance