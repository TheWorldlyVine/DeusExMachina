# Backend Integration Guide

This guide explains how the novel-creator frontend integrates with the backend services.

## Architecture Overview

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────────┐
│  Novel Creator  │────▶│  GraphQL Gateway │────▶│  Document Service   │
│   (Frontend)    │     │  (Port 4000)     │     │  (Port 8080)       │
└─────────────────┘     └──────────────────┘     └─────────────────────┘
                                 │
                                 ├──────▶ Auth Service (8081)
                                 ├──────▶ Memory Service (8082)
                                 └──────▶ AI Service (8083)
```

## Data Flow

### 1. Document Management

#### Creating a Document
```typescript
// Frontend: Create document via GraphQL
mutation CreateDocument($input: CreateDocumentInput!) {
  createDocument(input: $input) {
    id
    title
    chapters { ... }
  }
}
```

#### Saving Content
```typescript
// Frontend: Auto-save content
// 1. Content is edited in TipTap (HTML format)
// 2. Converted to Markdown via contentConverter
// 3. Sent to backend via updateScene mutation
mutation UpdateScene($documentId: ID!, $chapterNumber: Int!, $sceneNumber: Int!, $input: UpdateSceneInput!) {
  updateScene(documentId: $documentId, chapterNumber: $chapterNumber, sceneNumber: $sceneNumber, input: $input) {
    content
    wordCount
  }
}
```

### 2. Chapter and Scene Structure

The backend stores documents hierarchically:
- **Document**: Top-level container
  - **Chapters**: Numbered sections (1, 2, 3...)
    - **Scenes**: Content units within chapters (1, 2, 3...)

### 3. Content Format Conversion

- **Frontend Editor**: Uses TipTap which produces HTML
- **Backend Storage**: Expects Markdown format
- **Conversion**: Handled by `contentConverter.ts` utility

```typescript
// HTML from TipTap → Markdown for backend
const markdown = htmlToMarkdown(editorContent)

// Markdown from backend → HTML for TipTap
const html = markdownToHtml(backendContent)
```

## Local Development Setup

### 1. Start Backend Services

```bash
# Terminal 1: Document Service (Java)
cd apps/backend/novel-document-service
./gradlew bootRun
# Runs on http://localhost:8080

# Terminal 2: GraphQL Gateway
cd apps/backend/novel-graphql-gateway
npm run dev
# Runs on http://localhost:4000
```

### 2. Configure Frontend

The frontend automatically connects to the GraphQL gateway at `http://localhost:4000/graphql`.

### 3. Test the Integration

1. Open the novel creator: http://localhost:5173/novel-creator
2. Create or open a document
3. Start typing - content auto-saves every 2 seconds
4. Check the network tab to see GraphQL mutations

## API Endpoints

### GraphQL Gateway (Port 4000)

- **Endpoint**: `http://localhost:4000/graphql`
- **Playground**: `http://localhost:4000/graphql` (in development mode)

### Document Service REST API (Port 8080)

- `GET /document/{id}` - Get document with chapters and scenes
- `POST /document` - Create new document
- `PUT /document/{id}` - Update document metadata
- `DELETE /document/{id}` - Delete document
- `POST /chapter/{documentId}/{chapterNumber}` - Create chapter
- `PUT /chapter/{documentId}/{chapterNumber}` - Update chapter
- `DELETE /chapter/{documentId}/{chapterNumber}` - Delete chapter
- `POST /scene/{documentId}/{chapterNumber}/{sceneNumber}` - Create scene
- `PUT /scene/{documentId}/{chapterNumber}/{sceneNumber}` - Update scene
- `DELETE /scene/{documentId}/{chapterNumber}/{sceneNumber}` - Delete scene

## Authentication

1. User logs in via Firebase Auth
2. Frontend includes Firebase JWT token in requests
3. GraphQL Gateway validates token
4. Backend services trust the gateway's user context

## Common Issues and Solutions

### Issue: Cannot connect to backend
**Solution**: Ensure all services are running and check `.env.local` configuration

### Issue: Content not saving
**Solution**: Check browser console for errors, ensure GraphQL mutations are succeeding

### Issue: Formatting lost when reloading
**Solution**: Check HTML/Markdown conversion is working correctly

### Issue: CORS errors
**Solution**: Ensure frontend URL is in ALLOWED_ORIGINS in GraphQL gateway config

## Production Deployment

In production, the services are deployed as:
- Frontend: Static hosting (Cloudflare Pages, Vercel, etc.)
- GraphQL Gateway: Google Cloud Run
- Document Service: Google Cloud Functions
- Database: Firestore

Environment variables need to be updated to use production URLs instead of localhost.