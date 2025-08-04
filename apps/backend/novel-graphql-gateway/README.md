# Novel GraphQL Gateway

GraphQL API Gateway for the Novel Creator Platform, built with Apollo Server and Express.

## Overview

This service provides a unified GraphQL API that federates requests to various backend microservices:
- Authentication Service
- Document Service  
- Memory Service (SCORE pattern implementation)
- AI Generation Service

## Features

- GraphQL schema with queries, mutations, and subscriptions
- Real-time updates via WebSocket subscriptions
- Authentication and authorization
- Rate limiting and security features
- Query depth and complexity limiting
- Request logging and monitoring

## Development

### Prerequisites

- Node.js 18+
- npm or yarn
- Access to backend services (can run locally or use cloud functions)

### Setup

1. Install dependencies:
```bash
npm install
```

2. Copy environment variables:
```bash
cp .env.example .env
# For local development with local services:
cp .env.local.example .env.local
```

3. Configure service URLs in `.env` or `.env.local`:
   - For local development: Use localhost URLs (e.g., http://localhost:8080)
   - For cloud development: Use Cloud Function URLs

4. Start backend services (if running locally):
```bash
# In separate terminals:
# Document Service
cd ../novel-document-service && ./gradlew bootRun

# Auth Service (if needed)
cd ../auth-service && npm run dev
```

5. Run GraphQL gateway in development mode:
```bash
npm run dev
```

The GraphQL playground will be available at http://localhost:4000/graphql

### Scripts

- `npm run dev` - Start development server with hot reload
- `npm run build` - Build TypeScript to JavaScript
- `npm start` - Run production server
- `npm test` - Run tests
- `npm run lint` - Run ESLint
- `npm run type-check` - Check TypeScript types

## Deployment

### Cloud Run

1. Build Docker image:
```bash
docker build -t novel-graphql-gateway .
```

2. Push to Container Registry:
```bash
docker tag novel-graphql-gateway gcr.io/YOUR_PROJECT_ID/novel-graphql-gateway
docker push gcr.io/YOUR_PROJECT_ID/novel-graphql-gateway
```

3. Deploy to Cloud Run:
```bash
gcloud run deploy novel-graphql-gateway \
  --image gcr.io/YOUR_PROJECT_ID/novel-graphql-gateway \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

## API Documentation

### Authentication

Include JWT token in Authorization header:
```
Authorization: Bearer <token>
```

### Example Queries

Get current user:
```graphql
query Me {
  me {
    id
    email
    displayName
    role
  }
}
```

Get document with chapters:
```graphql
query GetDocument($id: ID!) {
  document(id: $id) {
    id
    title
    currentWordCount
    chapters {
      chapterNumber
      title
      wordCount
      scenes {
        sceneNumber
        content
        wordCount
      }
    }
  }
}
```

### Example Mutations

Create character:
```graphql
mutation CreateCharacter($input: CreateCharacterInput!) {
  createCharacter(input: $input) {
    characterId
    name
    role
    backstory
  }
}
```

Generate scene:
```graphql
mutation GenerateScene($input: SceneGenerationInput!) {
  generateScene(input: $input) {
    requestId
    status
    generatedText
    wordCount
  }
}
```

### Subscriptions

Subscribe to generation progress:
```graphql
subscription GenerationProgress($requestId: ID!) {
  generationProgress(requestId: $requestId) {
    requestId
    progress
    status
    currentChunk
  }
}
```

## Architecture

- **Apollo Server 4** - GraphQL server
- **Express** - HTTP server
- **GraphQL WS** - WebSocket support for subscriptions
- **REST Data Sources** - Communication with backend services
- **JWT** - Authentication tokens
- **Firestore** - Direct database access where needed

## Security

- JWT token validation
- Rate limiting per IP
- Query depth limiting (default: 10)
- Query complexity limiting (default: 1000)
- CORS configuration
- Request logging