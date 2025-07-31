# DeusExMachina

A production-ready monorepo containing Java-based GCP Cloud Functions, React frontend applications, and infrastructure as code using Terraform.

## Project Structure

```
DeusExMachina/
├── apps/                      # Deployable applications
│   ├── backend/              # Java GCP Cloud Functions
│   └── frontend/             # React applications
├── packages/                 # Shared libraries
│   ├── ui-components/       # React component library
│   ├── java-common/         # Shared Java utilities
│   └── utils/               # Cross-platform utilities
├── infrastructure/          # Terraform IaC
│   ├── environments/        # Environment-specific configs
│   └── modules/             # Reusable Terraform modules
├── docs/                    # Documentation
└── tools/                   # Build tools and scripts
```

## Prerequisites

- Node.js >= 20.0.0
- pnpm >= 8.0.0
- Java 21
- Gradle 8.x
- Terraform >= 1.6.0
- Google Cloud SDK

## Getting Started

### Initial Setup

1. Clone the repository:
```bash
git clone https://github.com/your-org/DeusExMachina.git
cd DeusExMachina
```

2. Install dependencies:
```bash
pnpm install
```

3. Set up environment variables:
```bash
cp .env.example .env
# Edit .env with your configuration
```

### Development

#### Frontend Development
```bash
# Start the web application
pnpm --filter web-app dev

# Run all frontend tests
pnpm run test

# Build all frontend applications
pnpm run build
```

#### Backend Development
```bash
# Run a specific function locally
./gradlew :apps:backend:auth-function:runFunction

# Run all backend tests
./gradlew test

# Build all functions
./gradlew build
```

#### Infrastructure
```bash
# Initialize Terraform
cd infrastructure/environments/dev
terraform init

# Plan changes
terraform plan

# Apply changes
terraform apply
```

## Architecture

### Backend Services
- **auth-function**: Authentication service with JWT token generation
- **api-function**: Main API service for business logic
- **processor-function**: Async processing service

### Frontend Applications
- **web-app**: Main user-facing application
- **admin-panel**: Administrative interface

### Shared Packages
- **ui-components**: Reusable React components with Storybook
- **java-common**: Shared Java utilities and base classes
- **utils**: Cross-platform utility functions

## Testing

### Unit Tests
```bash
# Frontend
pnpm test

# Backend
./gradlew test
```

### Integration Tests
```bash
# Run integration test suite
pnpm run test:integration
```

### E2E Tests
```bash
# Run Cypress tests
pnpm run test:e2e
```

## CI/CD

The project uses GitHub Actions for continuous integration and deployment:

- **Main Pipeline**: Runs on every push and PR
- **Security Scanning**: Daily vulnerability scans
- **Dependency Updates**: Weekly dependency updates

## Documentation

- [Technical Specifications](./docs/technical-specs/)
- [Product Requirements](./docs/prds/)
- [Architecture Decision Records](./docs/adrs/)
- [API Documentation](./docs/api/)

## Contributing

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.