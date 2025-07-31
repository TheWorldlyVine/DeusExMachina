# ADR-001: Monorepo Structure and Tooling

## Status
Accepted

## Context
We need to establish a scalable and maintainable repository structure for the DeusExMachina project that will contain multiple applications (Java backend functions, React frontend apps), shared libraries, infrastructure code, and comprehensive documentation.

## Decision
We will use a monorepo structure with Nx as the primary build system orchestrator, organizing code into:
- `apps/` - Deployable applications (backend functions, frontend apps)
- `packages/` - Shared libraries and components
- `infrastructure/` - Terraform IaC modules and environments
- `docs/` - All documentation (technical specs, PRDs, ADRs)

## Consequences
### Positive
- Single source of truth for all code and documentation
- Easier dependency management and version synchronization
- Atomic commits across multiple projects
- Shared tooling and configuration
- Better code reuse through shared packages
- Simplified CI/CD pipeline management

### Negative
- Larger repository size over time
- Requires team discipline to maintain structure
- More complex initial setup
- Potential for longer CI/CD runs without proper optimization

## Alternatives Considered
### Multi-repo Approach
- Pros: Smaller, focused repositories; independent versioning
- Cons: Complex dependency management; harder to maintain consistency

### Monolith
- Pros: Simpler initial setup
- Cons: Poor separation of concerns; difficult to scale teams

## References
- [Nx Documentation](https://nx.dev)
- [Google Monorepo Practices](https://research.google/pubs/pub45424/)
- [Monorepo Tools Comparison](https://monorepo.tools)