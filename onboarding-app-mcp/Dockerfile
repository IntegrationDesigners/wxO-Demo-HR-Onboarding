# ── Build stage ────────────────────────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY tsconfig.json ./
COPY src/ ./src/
RUN npm run build

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM node:22-alpine

LABEL org.opencontainers.image.source="https://github.com/IntegrationDesigners/wxO-Demo-HR-Onboarding" \
      org.opencontainers.image.description="Employee Onboarding MCP Server"

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev

COPY --from=builder /app/build ./build

ENV PORT=8888
ENV ONBOARDING_API_URL=http://host.docker.internal:8080

EXPOSE 8888

CMD ["node", "build/index.js"]
