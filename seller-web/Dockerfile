# Build stage - Install dependencies
FROM node:22-alpine AS deps

WORKDIR /app

# Install dependencies needed for node-gyp (if native modules are used)
RUN apk add --no-cache libc6-compat

# Copy package files for dependency caching
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci --only=production=false

# Build stage - Build the application
FROM node:22-alpine AS builder

WORKDIR /app

# Copy dependencies from deps stage
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Set Next.js to output standalone build
ENV NEXT_TELEMETRY_DISABLED=1

# Build the application
RUN npm run build

# Runtime stage - Production image
FROM node:22-alpine AS runner

WORKDIR /app

# Set to production environment
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# Add non-root user for security
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nextjs -u 1001

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy built assets from builder
# Copy public folder
COPY --from=builder /app/public ./public

# Copy standalone build output
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# Set ownership
RUN chown -R nextjs:nodejs /app

USER nextjs

# Expose port
EXPOSE 3000

# Set hostname to listen on all interfaces
ENV HOSTNAME="0.0.0.0"
ENV PORT=3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:3000/api/health || exit 1

# Start the application
CMD ["node", "server.js"]
