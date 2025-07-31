#!/bin/bash
# Pre-commit check script to ensure code quality

echo "🔍 Running pre-commit checks..."

# Run linting
echo "📝 Running ESLint..."
pnpm run lint
if [ $? -ne 0 ]; then
    echo "❌ Linting failed! Please fix the errors before committing."
    exit 1
fi

# Run type checking
echo "🔧 Running TypeScript type check..."
pnpm run type-check
if [ $? -ne 0 ]; then
    echo "❌ Type checking failed! Please fix the errors before committing."
    exit 1
fi

echo "✅ All pre-commit checks passed!"
exit 0