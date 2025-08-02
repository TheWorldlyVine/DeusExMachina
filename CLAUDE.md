# Instructions for Claude

This document contains important guidelines and best practices that Claude should follow when working on the DeusExMachina project.

## Essential Guidelines

### 1. Always Consult Documentation First
- **Before making any changes**, always check the `/docs` directory for relevant documentation
- Review Architecture Decision Records (ADRs) in `/docs/adrs/` to understand past decisions
- Check technical specifications in `/docs/technical-specs/` for implementation details
- Review PRDs in `/docs/prds/` to understand product requirements
- **For frontend apps**: Follow `/docs/checklists/frontend-app-setup-checklist.md`
- If documentation is missing or unclear, ask for clarification before proceeding

### 2. Test-Driven Development (TDD)
- **Write tests first** before implementing any new functionality
- Follow the Red-Green-Refactor cycle:
  1. Write a failing test (Red)
  2. Write minimal code to make the test pass (Green)
  3. Refactor the code while keeping tests passing
- Aim for minimum 80% code coverage for new code
- Include unit tests, integration tests, and E2E tests where appropriate

### 3. Clean Code Principles
- **Write code for humans first**, compilers second
- Follow SOLID principles:
  - Single Responsibility Principle
  - Open/Closed Principle
  - Liskov Substitution Principle
  - Interface Segregation Principle
  - Dependency Inversion Principle
- Keep functions small and focused (ideally < 20 lines)
- Use descriptive variable and function names
- Avoid deep nesting (max 3 levels)
- Prefer composition over inheritance
- Write self-documenting code (minimize comments)

### 4. Code Style and Conventions
- **Java Backend**:
  - Follow Google Java Style Guide
  - Use meaningful package names: `com.deusexmachina.module.submodule`
  - Implement proper error handling with specific exceptions
  - Add JML specifications for formal verification
  - Use dependency injection patterns
  
- **React Frontend**:
  - Use functional components with hooks
  - Follow React best practices and patterns
  - Implement proper TypeScript types (avoid `any`)
  - Use named exports for better refactoring
  - Keep components pure and side-effect free

- **Infrastructure**:
  - Follow Terraform best practices
  - Use meaningful resource names
  - Implement proper tagging strategies
  - Always use variables for repeated values
  - Document all modules with examples

### 5. Security Best Practices
- Never commit secrets or credentials
- Always validate and sanitize user input
- Use parameterized queries for database operations
- Implement proper authentication and authorization
- Follow OWASP guidelines
- Use security headers in HTTP responses
- Implement rate limiting where appropriate

### 6. Performance Considerations
- Profile before optimizing
- Implement caching strategies where appropriate
- Use pagination for large data sets
- Optimize database queries (use indexes)
- Implement lazy loading for frontend resources
- Monitor and log performance metrics

### 7. Git Workflow
- Create feature branches from `develop` branch
- Use conventional commit messages:
  - `feat:` for new features
  - `fix:` for bug fixes
  - `refactor:` for code refactoring
  - `test:` for test additions/modifications
  - `docs:` for documentation updates
  - `chore:` for maintenance tasks
- Keep commits atomic and focused
- Write clear, descriptive commit messages
- Always run tests before committing

### 8. Code Review Checklist
Before submitting any code:
- [ ] All tests pass locally
- [ ] Code coverage meets minimum requirements (80%)
- [ ] No linting errors or warnings
- [ ] Documentation is updated if needed
- [ ] ADR created for significant architectural decisions
- [ ] Security implications considered
- [ ] Performance impact assessed
- [ ] Accessibility requirements met (for frontend)

### 9. Architecture Principles
- Maintain clear separation of concerns
- Follow domain-driven design principles
- Implement proper error boundaries
- Use event-driven architecture where appropriate
- Design for scalability and maintainability
- Implement proper logging and monitoring
- Use circuit breakers for external dependencies

### 10. Documentation Standards
- Update documentation as part of the implementation
- Include examples in all documentation
- Write clear API documentation
- Document design decisions in ADRs
- Keep README files up to date
- Include troubleshooting guides

## Project-Specific Commands

When working on this project, remember to run these commands:

### Linting and Type Checking
```bash
# Frontend
pnpm run lint
pnpm run type-check

# Backend
./gradlew spotbugsMain
./gradlew build
```

### Testing
```bash
# Frontend
pnpm test
pnpm run test:e2e

# Backend
./gradlew test
./tools/verify.sh  # Formal verification
```

### Building
```bash
# Frontend
pnpm run build

# Backend
./gradlew build
```

## Important Notes

1. **Always prioritize code quality over speed of delivery**
2. **When in doubt, refer to existing patterns in the codebase**
3. **Ask for clarification rather than making assumptions**
4. **Consider the long-term maintainability of any solution**
5. **Remember that this is a production-ready system - act accordingly**
6. **ALWAYS run `pnpm run pre-commit` before committing to avoid CI failures**
7. **Before creating new infrastructure, understand existing deployment patterns**:
   - Check `.github/workflows/` for CI/CD configuration
   - Review how similar components are deployed
   - Read the frontend static hosting specification
   - Understand that frontend apps deploy to subdirectories automatically

## Continuous Improvement

This document should be updated as the project evolves. If you identify new best practices or patterns that should be followed, document them here for future reference.