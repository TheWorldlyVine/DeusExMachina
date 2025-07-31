# Code Quality Guidelines

## Pre-Commit Checks

To ensure code quality and prevent CI failures, always run pre-commit checks before pushing code:

```bash
pnpm run pre-commit
```

This command will:
1. Run ESLint to check for code style and potential issues
2. Run TypeScript type checking to ensure type safety

## Quick Fixes

If you encounter linting errors, you can try to automatically fix them:

```bash
pnpm run lint:fix
```

Note: Not all errors can be automatically fixed. Manual intervention may be required.

## Common Linting Errors and Solutions

### 1. `@typescript-eslint/no-non-null-asserted-optional-chain`

**Error**: Using `!` after `?.` is unsafe because optional chaining can return undefined.

**Bad**:
```typescript
const value = obj?.property!
```

**Good**:
```typescript
if (obj?.property) {
  const value = obj.property
}
```

### 2. `@typescript-eslint/no-explicit-any`

**Error**: Using `any` type defeats the purpose of TypeScript.

**Bad**:
```typescript
const data: any = fetchData()
```

**Good**:
```typescript
const data: Record<string, unknown> = fetchData()
// or define a proper interface
```

### 3. Unescaped entities in JSX

**Error**: Characters like `'`, `"`, `&` need to be escaped in JSX.

**Bad**:
```jsx
<p>Don't use apostrophes</p>
```

**Good**:
```jsx
<p>Don&apos;t use apostrophes</p>
```

## Development Workflow

1. Make your changes
2. Run `pnpm run pre-commit` before committing
3. Fix any issues that arise
4. Commit your changes
5. Push to remote

This workflow ensures that CI will pass and maintains code quality across the project.