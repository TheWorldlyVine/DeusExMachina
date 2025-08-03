# Build stage
FROM node:18-alpine AS builder

# Install pnpm with specific version
RUN corepack enable && corepack prepare pnpm@8.15.9 --activate

WORKDIR /app

# Copy workspace files
COPY pnpm-workspace.yaml ./
COPY pnpm-lock.yaml ./

# Copy package files for the GraphQL gateway
COPY apps/backend/novel-graphql-gateway/package.json ./apps/backend/novel-graphql-gateway/
COPY apps/backend/novel-graphql-gateway/tsconfig.json ./apps/backend/novel-graphql-gateway/

# Install dependencies using workspace
RUN pnpm install --frozen-lockfile --filter @deusexmachina/novel-graphql-gateway...

# Copy source code
COPY apps/backend/novel-graphql-gateway/src ./apps/backend/novel-graphql-gateway/src

# Build the application
WORKDIR /app/apps/backend/novel-graphql-gateway
RUN pnpm run build

# Production stage
FROM node:18-alpine

# Install pnpm with specific version
RUN corepack enable && corepack prepare pnpm@8.15.9 --activate

WORKDIR /app

# Copy workspace files
COPY pnpm-workspace.yaml ./
COPY pnpm-lock.yaml ./

# Copy package files
COPY apps/backend/novel-graphql-gateway/package.json ./apps/backend/novel-graphql-gateway/

# Install production dependencies only
RUN pnpm install --frozen-lockfile --filter @deusexmachina/novel-graphql-gateway... --prod

# Copy built application
COPY --from=builder /app/apps/backend/novel-graphql-gateway/dist ./apps/backend/novel-graphql-gateway/dist
COPY --from=builder /app/apps/backend/novel-graphql-gateway/src/schema ./apps/backend/novel-graphql-gateway/dist/schema

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

# Change ownership
RUN chown -R nodejs:nodejs /app

# Switch to non-root user
USER nodejs

# Set working directory to the service
WORKDIR /app/apps/backend/novel-graphql-gateway

# Expose port
EXPOSE 4000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node -e "require('http').get('http://localhost:4000/health', (res) => { process.exit(res.statusCode === 200 ? 0 : 1); })"

# Start the application
CMD ["node", "dist/index.js"]