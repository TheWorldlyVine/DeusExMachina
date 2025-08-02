# Technical Specification: API Gateway and GraphQL Schema (GCP)

## Overview

The API Gateway and GraphQL layer provides a unified, scalable entry point for all Novel Creator services. Built on Google Cloud Platform, it leverages Cloud Load Balancer for traffic distribution, Cloud Endpoints for API management, and Cloud Run for hosting the GraphQL server. The architecture implements a federated GraphQL schema that aggregates multiple backend services while providing real-time subscriptions, efficient caching, and comprehensive monitoring.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Client Applications                         │
│        (Web App, Mobile App, Third-party Integrations)              │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────────┐
│                    Cloud CDN (Global Edge Caching)                   │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────────┐
│                 Cloud Load Balancer (HTTPS/WSS)                      │
│                     - SSL Termination                                │
│                     - Path-based Routing                             │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────────┐
│                    Cloud Armor (WAF/DDoS)                            │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
            ┌─────────────┴──────────────┬────────────────────┐
            │                            │                      │
┌───────────▼──────────┐    ┌───────────▼──────────┐  ┌───────▼────────┐
│  Cloud Endpoints     │    │   GraphQL Gateway    │  │  WebSocket     │
│  (REST API)          │    │   (Cloud Run)        │  │  Gateway       │
│  - OpenAPI 3.0       │    │   - Apollo Server    │  │  (Cloud Run)   │
│  - Rate Limiting     │    │   - Federation       │  │  - Subscriptions│
│  - API Keys          │    │   - Schema Registry  │  │  - Pub/Sub      │
└──────────────────────┘    └──────────────────────┘  └────────────────┘
            │                            │                      │
┌───────────┴────────────────────────────┴──────────────────────┴────┐
│                         Backend Services                             │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐            │
│  │ Generation  │  │   Memory     │  │   Document    │            │
│  │  Service    │  │   Service    │  │   Service     │            │
│  │(Cloud Func) │  │(Cloud Func)  │  │ (Cloud Func)  │            │
│  └─────────────┘  └──────────────┘  └───────────────┘            │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐            │
│  │Collaboration│  │   Export     │  │     Auth      │            │
│  │  Service    │  │   Service    │  │   Service     │            │
│  │(Cloud Run)  │  │(Cloud Run)   │  │ (Cloud Func)  │            │
│  └─────────────┘  └──────────────┘  └───────────────┘            │
└─────────────────────────────────────────────────────────────────────┘
```

### GCP Service Integration

```
┌──────────────────────────────────────────────────────────┐
│                   GCP API Gateway Stack                   │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Traffic Management:                                     │
│  - Cloud Load Balancer (Global L7)                      │
│  - Cloud CDN (Edge caching)                             │
│  - Cloud Armor (Security policies)                      │
│                                                          │
│  API Management:                                         │
│  - Cloud Endpoints (REST API management)                 │
│  - API Gateway (OpenAPI routing)                        │
│  - Service Mesh (Istio on GKE - optional)              │
│                                                          │
│  Compute:                                                │
│  - Cloud Run (GraphQL server)                           │
│  - Cloud Functions (Microservices)                      │
│  - GKE (If needed for complex services)                 │
│                                                          │
│  Integration:                                            │
│  - Pub/Sub (Event streaming)                            │
│  - Cloud Tasks (Async processing)                       │
│  - Firestore (Real-time data)                          │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## GraphQL Schema Design

### Schema Architecture

```graphql
# Root Schema (schema.graphql)
type Query {
  # Document queries
  document(id: ID!): Document
  documents(
    projectId: ID!
    filter: DocumentFilter
    pagination: PaginationInput
  ): DocumentConnection!
  
  # Memory queries
  character(id: ID!): Character
  characters(projectId: ID!): [Character!]!
  plotThreads(projectId: ID!, status: ThreadStatus): [PlotThread!]!
  worldFacts(projectId: ID!, category: FactCategory): [WorldFact!]!
  generationContext(projectId: ID!, sceneId: ID!): GenerationContext!
  
  # User queries
  me: User!
  user(id: ID!): User
  project(id: ID!): Project
  projects: [Project!]!
  
  # Search
  search(query: SearchInput!): SearchResults!
}

type Mutation {
  # Document mutations
  createDocument(input: CreateDocumentInput!): Document!
  updateDocument(id: ID!, input: UpdateDocumentInput!): Document!
  deleteDocument(id: ID!): Boolean!
  
  # Content generation
  generateContent(input: GenerationInput!): GenerationJob!
  cancelGeneration(jobId: ID!): Boolean!
  
  # Memory mutations
  updateCharacterState(
    characterId: ID!
    sceneId: ID!
    state: CharacterStateInput!
  ): Character!
  recordObservation(
    characterId: ID!
    observation: ObservationInput!
  ): Character!
  addWorldFact(projectId: ID!, fact: WorldFactInput!): WorldFact!
  
  # Collaboration
  createComment(input: CreateCommentInput!): Comment!
  resolveComment(id: ID!): Comment!
  
  # Export
  createExport(input: CreateExportInput!): ExportJob!
  
  # Project management
  createProject(input: CreateProjectInput!): Project!
  updateProject(id: ID!, input: UpdateProjectInput!): Project!
  inviteCollaborator(projectId: ID!, email: String!, role: Role!): Invitation!
}

type Subscription {
  # Document updates
  documentUpdated(documentId: ID!): DocumentUpdate!
  
  # Real-time collaboration
  collaborationEvent(documentId: ID!): CollaborationEvent!
  cursorUpdate(documentId: ID!): CursorUpdate!
  presenceUpdate(documentId: ID!): PresenceUpdate!
  
  # Generation progress
  generationProgress(jobId: ID!): GenerationProgress!
  
  # Export progress
  exportProgress(jobId: ID!): ExportProgress!
  
  # Notifications
  notifications(userId: ID!): Notification!
}

# Federation directives
extend type User @key(fields: "id") {
  id: ID! @external
  projects: [Project!]!
}

extend type Project @key(fields: "id") {
  id: ID! @external
  documents: [Document!]!
  team: [TeamMember!]!
}
```

### Core Types

```graphql
# Document Types
type Document {
  id: ID!
  projectId: ID!
  title: String!
  metadata: DocumentMetadata!
  structure: DocumentStructure!
  stats: DocumentStats!
  collaborators: [Collaborator!]!
  currentVersion: Int!
  createdAt: DateTime!
  updatedAt: DateTime!
  createdBy: User!
  settings: DocumentSettings!
}

type DocumentMetadata {
  genre: String
  targetWordCount: Int
  completionPercentage: Float!
  tags: [String!]!
  coverImage: String
  synopsis: String
}

type DocumentStructure {
  acts: [Act!]!
  totalChapters: Int!
  totalScenes: Int!
  chunkMap: [ChunkMapping!]!
}

type DocumentStats {
  totalWords: Int!
  totalCharacters: Int!
  totalParagraphs: Int!
  avgWordsPerScene: Float!
  readingTime: Int! # minutes
}

# Memory Types
type Character {
  id: ID!
  projectId: ID!
  name: String!
  aliases: [String!]!
  currentState: CharacterState!
  biography: String
  appearance: CharacterAppearance!
  personality: CharacterPersonality!
  relationships: [Relationship!]!
  arc: CharacterArc!
  scenes: [SceneAppearance!]!
  wordCount: Int!
  lastAppearance: Scene
}

type CharacterState {
  physical: PhysicalState!
  emotional: EmotionalState!
  location: String!
  inventory: [String!]!
  goals: [Goal!]!
  secrets: [Secret!]!
}

type PlotThread {
  id: ID!
  projectId: ID!
  name: String!
  type: ThreadType!
  status: ThreadStatus!
  description: String!
  involvedCharacters: [Character!]!
  milestones: [PlotMilestone!]!
  tensionLevel: Int!
  dependencies: [PlotThread!]!
  resolution: String
}

type WorldFact {
  id: ID!
  projectId: ID!
  category: FactCategory!
  fact: String!
  importance: Importance!
  establishedIn: Scene!
  relatedFacts: [WorldFact!]!
  contradictions: [Contradiction!]!
}

# Generation Types
type GenerationContext {
  projectId: ID!
  sceneId: ID!
  availableTokens: Int!
  characters: [CharacterContext!]!
  plotThreads: [PlotThreadContext!]!
  worldFacts: [WorldFact!]!
  recentScenes: [SceneSummary!]!
  suggestions: [GenerationSuggestion!]!
}

type GenerationJob {
  id: ID!
  status: JobStatus!
  progress: Float!
  result: GenerationResult
  error: Error
  estimatedCompletion: DateTime
  createdAt: DateTime!
}

type GenerationResult {
  content: String!
  wordCount: Int!
  coherenceScore: Float!
  suggestions: [Suggestion!]!
  usage: TokenUsage!
}

# Collaboration Types
type CollaborationEvent {
  id: ID!
  type: CollaborationEventType!
  user: User!
  timestamp: DateTime!
  data: JSON!
}

type Comment {
  id: ID!
  documentId: ID!
  author: User!
  content: String!
  range: TextRange!
  thread: [CommentReply!]!
  resolved: Boolean!
  createdAt: DateTime!
  resolvedAt: DateTime
  resolvedBy: User
}

# Export Types
type ExportJob {
  id: ID!
  format: ExportFormat!
  status: JobStatus!
  progress: Float!
  downloadUrl: String
  error: Error
  metadata: ExportMetadata!
  createdAt: DateTime!
  completedAt: DateTime
}

# Common Types
enum JobStatus {
  PENDING
  PROCESSING
  COMPLETED
  FAILED
  CANCELLED
}

enum ExportFormat {
  EPUB
  PDF
  DOCX
  HTML
  MARKDOWN
}

interface Node {
  id: ID!
}

interface Connection {
  edges: [Edge!]!
  pageInfo: PageInfo!
  totalCount: Int!
}

interface Edge {
  node: Node!
  cursor: String!
}

type PageInfo {
  hasNextPage: Boolean!
  hasPreviousPage: Boolean!
  startCursor: String
  endCursor: String
}

# Input Types
input PaginationInput {
  first: Int
  after: String
  last: Int
  before: String
}

input DocumentFilter {
  title: String
  genre: String
  minWords: Int
  maxWords: Int
  updatedAfter: DateTime
  tags: [String!]
}

scalar DateTime
scalar JSON
scalar Upload
```

## API Gateway Implementation

### Cloud Run GraphQL Server

```typescript
// server.ts - Main GraphQL server on Cloud Run
import { ApolloServer } from '@apollo/server';
import { expressMiddleware } from '@apollo/server/express4';
import { ApolloServerPluginDrainHttpServer } from '@apollo/server/plugin/drainHttpServer';
import { makeExecutableSchema } from '@graphql-tools/schema';
import { WebSocketServer } from 'ws';
import { useServer } from 'graphql-ws/lib/use/ws';
import express from 'express';
import cors from 'cors';
import http from 'http';

// Schema and resolvers
import { typeDefs } from './schema';
import { resolvers } from './resolvers';
import { dataSources } from './datasources';
import { createContext } from './context';

// Middleware
import { authMiddleware } from './middleware/auth';
import { rateLimitMiddleware } from './middleware/rateLimit';
import { loggingPlugin } from './plugins/logging';
import { cachingPlugin } from './plugins/caching';
import { tracingPlugin } from './plugins/tracing';

async function startApolloServer() {
  const app = express();
  const httpServer = http.createServer(app);
  
  // Create executable schema
  const schema = makeExecutableSchema({
    typeDefs,
    resolvers
  });
  
  // WebSocket server for subscriptions
  const wsServer = new WebSocketServer({
    server: httpServer,
    path: '/graphql/subscriptions',
  });
  
  // WebSocket server cleanup
  const serverCleanup = useServer(
    {
      schema,
      context: async (ctx) => {
        // Authenticate WebSocket connection
        const token = ctx.connectionParams?.authentication;
        const user = await authenticateToken(token);
        
        return {
          user,
          pubsub: createPubSub(),
          dataSources: createDataSources()
        };
      },
      onConnect: async (ctx) => {
        console.log('Client connected:', ctx.connectionParams);
      },
      onDisconnect: async (ctx) => {
        console.log('Client disconnected');
      }
    },
    wsServer
  );
  
  // Create Apollo Server
  const server = new ApolloServer({
    schema,
    plugins: [
      ApolloServerPluginDrainHttpServer({ httpServer }),
      {
        async serverWillStart() {
          return {
            async drainServer() {
              await serverCleanup.dispose();
            },
          };
        },
      },
      loggingPlugin,
      cachingPlugin,
      tracingPlugin,
    ],
    formatError: (err) => {
      // Log error to Cloud Logging
      console.error('GraphQL error:', err);
      
      // Remove stack traces in production
      if (process.env.NODE_ENV === 'production') {
        delete err.extensions?.exception?.stacktrace;
      }
      
      return err;
    },
  });
  
  await server.start();
  
  // Apply middleware
  app.use(cors({
    origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
    credentials: true
  }));
  
  app.use(express.json({ limit: '10mb' }));
  app.use(authMiddleware);
  app.use(rateLimitMiddleware);
  
  // Health check endpoint
  app.get('/health', (req, res) => {
    res.json({ status: 'healthy', timestamp: new Date() });
  });
  
  // GraphQL endpoint
  app.use(
    '/graphql',
    expressMiddleware(server, {
      context: createContext,
    })
  );
  
  const PORT = process.env.PORT || 8080;
  
  await new Promise<void>((resolve) => {
    httpServer.listen({ port: PORT }, resolve);
  });
  
  console.log(`🚀 Server ready at http://localhost:${PORT}/graphql`);
  console.log(`🚀 Subscriptions ready at ws://localhost:${PORT}/graphql/subscriptions`);
}

// Start server
startApolloServer().catch((err) => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
```

### Context and Data Sources

```typescript
// context.ts - GraphQL context creation
import { PubSub } from '@google-cloud/pubsub';
import { Firestore } from '@google-cloud/firestore';
import { Storage } from '@google-cloud/storage';

export interface Context {
  user: AuthenticatedUser | null;
  dataSources: DataSources;
  pubsub: PubSubEngine;
  requestId: string;
  ip: string;
}

export async function createContext({ req, res }): Promise<Context> {
  // Extract user from JWT token
  const token = req.headers.authorization?.replace('Bearer ', '');
  const user = token ? await verifyToken(token) : null;
  
  // Create request-scoped data sources
  const dataSources = createDataSources();
  
  // Create Pub/Sub instance for real-time updates
  const pubsub = createPubSubEngine();
  
  return {
    user,
    dataSources,
    pubsub,
    requestId: req.headers['x-request-id'] || generateRequestId(),
    ip: req.ip
  };
}

// datasources.ts - Data source implementations
export function createDataSources(): DataSources {
  return {
    documentAPI: new DocumentAPI(),
    memoryAPI: new MemoryAPI(),
    generationAPI: new GenerationAPI(),
    userAPI: new UserAPI(),
    exportAPI: new ExportAPI()
  };
}

class DocumentAPI extends RESTDataSource {
  constructor() {
    super();
    this.baseURL = process.env.DOCUMENT_SERVICE_URL || 'https://document-service-abcd1234-uc.a.run.app';
  }
  
  async getDocument(id: string): Promise<Document> {
    return this.get(`/documents/${id}`);
  }
  
  async updateDocument(id: string, update: any): Promise<Document> {
    return this.patch(`/documents/${id}`, update);
  }
  
  @cacheResponse({ ttl: 60 })
  async getDocuments(projectId: string, filter?: DocumentFilter): Promise<Document[]> {
    return this.get('/documents', {
      projectId,
      ...filter
    });
  }
}

class MemoryAPI extends FirestoreDataSource {
  private firestore: Firestore;
  
  constructor() {
    super();
    this.firestore = new Firestore({
      projectId: process.env.GCP_PROJECT_ID
    });
  }
  
  async getCharacter(id: string): Promise<Character> {
    const doc = await this.firestore
      .collection('characters')
      .doc(id)
      .get();
    
    if (!doc.exists) {
      throw new Error('Character not found');
    }
    
    return { id: doc.id, ...doc.data() } as Character;
  }
  
  async updateCharacterState(
    characterId: string,
    sceneId: string,
    state: CharacterState
  ): Promise<Character> {
    const batch = this.firestore.batch();
    
    // Update current state
    const charRef = this.firestore.collection('characters').doc(characterId);
    batch.update(charRef, {
      currentState: state,
      lastUpdated: FieldValue.serverTimestamp()
    });
    
    // Add to state history
    const stateRef = charRef.collection('states').doc(sceneId);
    batch.set(stateRef, {
      ...state,
      timestamp: FieldValue.serverTimestamp()
    });
    
    await batch.commit();
    
    return this.getCharacter(characterId);
  }
}
```

### Resolvers

```typescript
// resolvers/index.ts - Main resolver map
export const resolvers = {
  Query: {
    // Document queries
    document: async (_, { id }, { dataSources, user }) => {
      await checkPermission(user, id, 'read');
      return dataSources.documentAPI.getDocument(id);
    },
    
    documents: async (_, { projectId, filter, pagination }, { dataSources, user }) => {
      await checkProjectAccess(user, projectId);
      
      const documents = await dataSources.documentAPI.getDocuments(
        projectId,
        filter
      );
      
      return createConnection(documents, pagination);
    },
    
    // Memory queries
    character: async (_, { id }, { dataSources }) => {
      return dataSources.memoryAPI.getCharacter(id);
    },
    
    generationContext: async (_, { projectId, sceneId }, { dataSources }) => {
      const [characters, plotThreads, worldFacts, recentScenes] = await Promise.all([
        dataSources.memoryAPI.getActiveCharacters(projectId, sceneId),
        dataSources.memoryAPI.getActivePlotThreads(projectId),
        dataSources.memoryAPI.getRelevantWorldFacts(projectId, sceneId),
        dataSources.documentAPI.getRecentScenes(projectId, sceneId, 5)
      ]);
      
      return {
        projectId,
        sceneId,
        availableTokens: 100000,
        characters,
        plotThreads,
        worldFacts,
        recentScenes,
        suggestions: await generateSuggestions(characters, plotThreads)
      };
    },
    
    // User queries
    me: async (_, __, { user }) => {
      if (!user) throw new AuthenticationError('Not authenticated');
      return user;
    },
    
    // Search
    search: async (_, { query }, { dataSources }) => {
      const [documents, characters, scenes] = await Promise.all([
        dataSources.documentAPI.searchDocuments(query),
        dataSources.memoryAPI.searchCharacters(query),
        dataSources.documentAPI.searchScenes(query)
      ]);
      
      return {
        documents,
        characters,
        scenes,
        totalCount: documents.length + characters.length + scenes.length
      };
    }
  },
  
  Mutation: {
    // Document mutations
    createDocument: async (_, { input }, { dataSources, user }) => {
      const document = await dataSources.documentAPI.createDocument({
        ...input,
        createdBy: user.id
      });
      
      // Initialize memory structures
      await dataSources.memoryAPI.initializeProject(document.projectId);
      
      return document;
    },
    
    // Generation mutations
    generateContent: async (_, { input }, { dataSources, user, pubsub }) => {
      const job = await dataSources.generationAPI.createGeneration({
        ...input,
        userId: user.id
      });
      
      // Publish job creation event
      await pubsub.publish('generation.created', {
        jobId: job.id,
        userId: user.id
      });
      
      return job;
    },
    
    // Memory mutations
    updateCharacterState: async (
      _, 
      { characterId, sceneId, state }, 
      { dataSources, pubsub }
    ) => {
      const character = await dataSources.memoryAPI.updateCharacterState(
        characterId,
        sceneId,
        state
      );
      
      // Publish update for real-time sync
      await pubsub.publish(`character.${characterId}.updated`, {
        character,
        sceneId
      });
      
      return character;
    },
    
    // Export mutations
    createExport: async (_, { input }, { dataSources, user }) => {
      return dataSources.exportAPI.createExport({
        ...input,
        userId: user.id
      });
    }
  },
  
  Subscription: {
    // Document subscriptions
    documentUpdated: {
      subscribe: (_, { documentId }, { pubsub }) => {
        return pubsub.asyncIterator(`document.${documentId}.updated`);
      }
    },
    
    // Collaboration subscriptions
    collaborationEvent: {
      subscribe: (_, { documentId }, { pubsub }) => {
        return pubsub.asyncIterator([
          `collab.${documentId}.cursor`,
          `collab.${documentId}.selection`,
          `collab.${documentId}.presence`
        ]);
      }
    },
    
    // Generation progress
    generationProgress: {
      subscribe: (_, { jobId }, { pubsub }) => {
        return pubsub.asyncIterator(`generation.${jobId}.progress`);
      }
    }
  },
  
  // Field resolvers
  Document: {
    createdBy: async (document, _, { dataSources }) => {
      return dataSources.userAPI.getUser(document.createdBy);
    },
    
    collaborators: async (document, _, { dataSources }) => {
      return dataSources.documentAPI.getCollaborators(document.id);
    },
    
    stats: async (document, _, { dataSources }) => {
      // Calculate stats on demand if not cached
      if (!document.stats) {
        return dataSources.documentAPI.calculateStats(document.id);
      }
      return document.stats;
    }
  },
  
  Character: {
    relationships: async (character, _, { dataSources }) => {
      return dataSources.memoryAPI.getCharacterRelationships(character.id);
    },
    
    scenes: async (character, _, { dataSources }) => {
      return dataSources.documentAPI.getCharacterScenes(
        character.projectId,
        character.id
      );
    },
    
    wordCount: async (character, _, { dataSources }) => {
      const scenes = await dataSources.documentAPI.getCharacterScenes(
        character.projectId,
        character.id
      );
      
      return scenes.reduce((total, scene) => total + scene.wordCount, 0);
    }
  },
  
  // Custom scalars
  DateTime: DateTimeResolver,
  JSON: JSONResolver
};
```

### Cloud Endpoints Configuration

```yaml
# openapi.yaml - Cloud Endpoints REST API definition
swagger: "2.0"
info:
  title: "Novel Creator API"
  description: "REST API for Novel Creator platform"
  version: "1.0.0"
host: "api.novel-creator.com"
schemes:
  - "https"
x-google-endpoints:
  - name: "api.novel-creator.com"
    target: "IP_ADDRESS"
x-google-management:
  metrics:
    - name: "novel-creator/request_count"
      displayName: "Request Count"
      description: "Number of API requests"
      metricKind: DELTA
      valueType: INT64
  quota:
    limits:
      - name: "requests-per-minute"
        metric: "novel-creator/request_count"
        unit: "1/min/{user}"
        values:
          STANDARD: 100
          PREMIUM: 1000

paths:
  /api/v1/documents/{documentId}:
    get:
      summary: "Get document"
      operationId: "getDocument"
      security:
        - api_key: []
        - bearer_auth: []
      parameters:
        - name: documentId
          in: path
          required: true
          type: string
      responses:
        200:
          description: "Document details"
          schema:
            $ref: "#/definitions/Document"
        404:
          description: "Document not found"
          
  /api/v1/generation/generate:
    post:
      summary: "Generate content"
      operationId: "generateContent"
      security:
        - bearer_auth: []
      parameters:
        - name: body
          in: body
          required: true
          schema:
            $ref: "#/definitions/GenerationRequest"
      x-google-quota:
        metricCosts:
          - metricName: "novel-creator/request_count"
            cost: 10
      responses:
        200:
          description: "Generation started"
          schema:
            $ref: "#/definitions/GenerationJob"
            
  /api/v1/export:
    post:
      summary: "Create export"
      operationId: "createExport"
      security:
        - bearer_auth: []
      parameters:
        - name: body
          in: body
          required: true
          schema:
            $ref: "#/definitions/ExportRequest"
      responses:
        200:
          description: "Export created"
          schema:
            $ref: "#/definitions/ExportJob"

securityDefinitions:
  api_key:
    type: "apiKey"
    name: "x-api-key"
    in: "header"
  bearer_auth:
    type: "oauth2"
    authorizationUrl: "https://auth.novel-creator.com/oauth/authorize"
    flow: "implicit"
    scopes:
      read: "Read access"
      write: "Write access"

definitions:
  Document:
    type: object
    properties:
      id:
        type: string
      title:
        type: string
      projectId:
        type: string
      metadata:
        type: object
      stats:
        type: object
```

### Load Balancer Configuration

```yaml
# terraform/modules/api-gateway/main.tf
# Global HTTP(S) Load Balancer with Cloud CDN

# Backend services
resource "google_compute_backend_service" "graphql_backend" {
  name                  = "novel-creator-graphql-backend"
  protocol              = "HTTP2"
  timeout_sec           = 30
  enable_cdn            = true
  
  cdn_policy {
    cache_mode = "CACHE_MODE_AUTO"
    default_ttl = 3600
    max_ttl     = 86400
    
    cache_key_policy {
      include_host         = true
      include_protocol     = true
      include_query_string = true
      
      query_string_whitelist = ["query", "variables"]
    }
  }
  
  backend {
    group = google_compute_region_network_endpoint_group.graphql_neg.id
    balancing_mode = "RATE"
    max_rate_per_endpoint = 1000
  }
  
  health_checks = [google_compute_health_check.graphql_health.id]
  
  log_config {
    enable = true
    sample_rate = 1.0
  }
}

# Cloud Run NEG for GraphQL server
resource "google_compute_region_network_endpoint_group" "graphql_neg" {
  name                  = "graphql-server-neg"
  network_endpoint_type = "SERVERLESS"
  region                = var.region
  
  cloud_run {
    service = google_cloud_run_service.graphql_server.name
  }
}

# URL Map for routing
resource "google_compute_url_map" "api_url_map" {
  name            = "novel-creator-api-url-map"
  default_service = google_compute_backend_service.graphql_backend.id
  
  host_rule {
    hosts        = ["api.novel-creator.com"]
    path_matcher = "api-paths"
  }
  
  path_matcher {
    name            = "api-paths"
    default_service = google_compute_backend_service.graphql_backend.id
    
    path_rule {
      paths   = ["/graphql", "/graphql/*"]
      service = google_compute_backend_service.graphql_backend.id
    }
    
    path_rule {
      paths   = ["/api/v1/*"]
      service = google_compute_backend_service.rest_backend.id
    }
    
    path_rule {
      paths   = ["/health"]
      service = google_compute_backend_service.health_backend.id
    }
  }
}

# HTTPS proxy
resource "google_compute_target_https_proxy" "api_https_proxy" {
  name             = "novel-creator-api-https-proxy"
  url_map          = google_compute_url_map.api_url_map.id
  ssl_certificates = [google_compute_managed_ssl_certificate.api_cert.id]
  
  ssl_policy = google_compute_ssl_policy.modern_tls.id
}

# Global forwarding rule
resource "google_compute_global_forwarding_rule" "api_forwarding_rule" {
  name                  = "novel-creator-api-forwarding-rule"
  ip_protocol          = "TCP"
  load_balancing_scheme = "EXTERNAL_MANAGED"
  port_range           = "443"
  target               = google_compute_target_https_proxy.api_https_proxy.id
  ip_address           = google_compute_global_address.api_ip.id
}

# Cloud Armor security policy
resource "google_compute_security_policy" "api_security_policy" {
  name = "novel-creator-api-security-policy"
  
  # Default rule
  rule {
    action   = "allow"
    priority = "2147483647"
    
    match {
      versioned_expr = "SRC_IPS_V1"
      
      config {
        src_ip_ranges = ["*"]
      }
    }
  }
  
  # Rate limiting rule
  rule {
    action   = "rate_based_ban"
    priority = "1000"
    
    match {
      versioned_expr = "SRC_IPS_V1"
      
      config {
        src_ip_ranges = ["*"]
      }
    }
    
    rate_limit_options {
      conform_action = "allow"
      exceed_action  = "deny(429)"
      
      rate_limit_threshold {
        count        = 100
        interval_sec = 60
      }
      
      ban_duration_sec = 600 # 10 minutes
    }
  }
  
  # Block common attacks
  rule {
    action   = "deny(403)"
    priority = "900"
    
    match {
      expr {
        expression = "evaluatePreconfiguredExpr('sqli-stable') || evaluatePreconfiguredExpr('xss-stable')"
      }
    }
  }
}
```

### Authentication & Authorization

```typescript
// middleware/auth.ts - Authentication middleware
import { OAuth2Client } from 'google-auth-library';
import { auth } from 'firebase-admin';

const oauthClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

export async function authMiddleware(req, res, next) {
  const token = req.headers.authorization?.replace('Bearer ', '');
  
  if (!token) {
    return next();
  }
  
  try {
    // Try Firebase Auth first
    const decodedToken = await auth().verifyIdToken(token);
    req.user = {
      id: decodedToken.uid,
      email: decodedToken.email,
      role: decodedToken.role || 'user'
    };
  } catch (firebaseError) {
    try {
      // Fallback to Google OAuth
      const ticket = await oauthClient.verifyIdToken({
        idToken: token,
        audience: process.env.GOOGLE_CLIENT_ID
      });
      
      const payload = ticket.getPayload();
      req.user = {
        id: payload.sub,
        email: payload.email,
        role: 'user'
      };
    } catch (googleError) {
      // Invalid token
      console.error('Auth error:', googleError);
    }
  }
  
  next();
}

// middleware/rateLimit.ts - Rate limiting with Redis
import { RateLimiterRedis } from 'rate-limiter-flexible';
import Redis from 'ioredis';

const redis = new Redis({
  host: process.env.REDIS_HOST || 'localhost',
  port: parseInt(process.env.REDIS_PORT || '6379'),
  enableReadyCheck: true,
  maxRetriesPerRequest: 3
});

const rateLimiter = new RateLimiterRedis({
  storeClient: redis,
  keyPrefix: 'rl:api',
  points: 100, // requests
  duration: 60, // per minute
  blockDuration: 60 * 10, // block for 10 minutes
});

// Different limits for different operations
const operationLimits = {
  'generateContent': { points: 10, duration: 60 },
  'createExport': { points: 5, duration: 60 },
  'default': { points: 100, duration: 60 }
};

export async function rateLimitMiddleware(req, res, next) {
  if (!req.user) {
    return next();
  }
  
  const operation = req.body?.operationName || 'default';
  const limits = operationLimits[operation] || operationLimits.default;
  
  try {
    await rateLimiter.consume(req.user.id, limits.points);
    next();
  } catch (rejRes) {
    res.status(429).json({
      error: 'Too Many Requests',
      retryAfter: Math.round(rejRes.msBeforeNext / 1000) || 60
    });
  }
}
```

### Monitoring & Observability

```typescript
// plugins/monitoring.ts - Monitoring integration
import { Plugin } from '@apollo/server';
import { Logging } from '@google-cloud/logging';
import { MetricServiceClient } from '@google-cloud/monitoring';
import { TraceExporter } from '@google-cloud/opentelemetry-cloud-trace-exporter';

const logging = new Logging();
const log = logging.log('graphql-server');
const metrics = new MetricServiceClient();

export const monitoringPlugin: Plugin = {
  async requestDidStart() {
    const start = Date.now();
    
    return {
      async willSendResponse(requestContext) {
        const duration = Date.now() - start;
        
        // Log to Cloud Logging
        const entry = log.entry({
          severity: 'INFO',
          resource: {
            type: 'cloud_run_revision',
            labels: {
              service_name: process.env.K_SERVICE,
              revision_name: process.env.K_REVISION
            }
          },
          jsonPayload: {
            operation: requestContext.request.operationName,
            duration,
            userId: requestContext.contextValue.user?.id,
            errors: requestContext.errors?.length || 0
          }
        });
        
        await log.write(entry);
        
        // Send metrics to Cloud Monitoring
        const timeSeries = {
          metric: {
            type: 'custom.googleapis.com/graphql/request_duration',
            labels: {
              operation: requestContext.request.operationName || 'unknown'
            }
          },
          resource: {
            type: 'cloud_run_revision',
            labels: {
              service_name: process.env.K_SERVICE,
              revision_name: process.env.K_REVISION,
              location: process.env.K_CONFIGURATION_REGION
            }
          },
          points: [{
            interval: {
              endTime: {
                seconds: Math.floor(Date.now() / 1000)
              }
            },
            value: {
              doubleValue: duration
            }
          }]
        };
        
        await metrics.createTimeSeries({
          name: `projects/${process.env.GCP_PROJECT_ID}`,
          timeSeries: [timeSeries]
        });
      },
      
      async didEncounterErrors(requestContext) {
        // Log errors with higher severity
        const entry = log.entry({
          severity: 'ERROR',
          jsonPayload: {
            operation: requestContext.request.operationName,
            errors: requestContext.errors,
            userId: requestContext.contextValue.user?.id,
            query: requestContext.request.query,
            variables: requestContext.request.variables
          }
        });
        
        await log.write(entry);
      }
    };
  }
};
```

### Caching Strategy

```typescript
// plugins/caching.ts - Response caching with Redis
import { Plugin } from '@apollo/server';
import Redis from 'ioredis';
import { createHash } from 'crypto';

const redis = new Redis({
  host: process.env.REDIS_HOST,
  port: parseInt(process.env.REDIS_PORT || '6379')
});

export const cachingPlugin: Plugin = {
  async requestDidStart() {
    return {
      async willSendResponse(requestContext) {
        const { request, response } = requestContext;
        
        // Only cache successful GET-like queries
        if (
          response.http.body.errors ||
          request.http.method !== 'POST' ||
          !isQueryOperation(request)
        ) {
          return;
        }
        
        // Generate cache key
        const cacheKey = generateCacheKey(request);
        
        // Check cache hints
        const maxAge = getMaxAge(response);
        if (maxAge > 0) {
          // Cache the response
          await redis.setex(
            cacheKey,
            maxAge,
            JSON.stringify(response.http.body)
          );
        }
      },
      
      async responseForOperation(requestContext) {
        const { request } = requestContext;
        
        if (!isQueryOperation(request)) {
          return null;
        }
        
        const cacheKey = generateCacheKey(request);
        const cached = await redis.get(cacheKey);
        
        if (cached) {
          // Return cached response
          return {
            http: {
              body: JSON.parse(cached),
              status: 200,
              headers: new Map([
                ['content-type', 'application/json'],
                ['x-cache', 'HIT']
              ])
            }
          };
        }
        
        return null;
      }
    };
  }
};

function generateCacheKey(request: any): string {
  const hash = createHash('sha256');
  hash.update(request.query);
  hash.update(JSON.stringify(request.variables || {}));
  hash.update(request.operationName || '');
  return `gql:${hash.digest('hex')}`;
}

function isQueryOperation(request: any): boolean {
  const operation = getOperationAST(request.query, request.operationName);
  return operation?.operation === 'query';
}

function getMaxAge(response: any): number {
  // Extract cache hints from response
  const cacheControl = response.http.body.extensions?.cacheControl;
  return cacheControl?.maxAge || 0;
}
```

## Security Considerations

### API Security Best Practices

```typescript
// Security configurations
export const securityConfig = {
  // CORS configuration
  cors: {
    origin: (origin, callback) => {
      const allowedOrigins = process.env.ALLOWED_ORIGINS?.split(',') || [];
      
      if (!origin || allowedOrigins.includes(origin)) {
        callback(null, true);
      } else {
        callback(new Error('Not allowed by CORS'));
      }
    },
    credentials: true,
    maxAge: 86400 // 24 hours
  },
  
  // Query depth limiting
  queryDepthLimit: 10,
  
  // Query complexity limiting
  queryComplexity: {
    maximumComplexity: 1000,
    scalarCost: 1,
    objectCost: 2,
    listFactor: 10,
    introspectionCost: 1000
  },
  
  // Request size limits
  requestSizeLimit: '10mb',
  
  // Timeout configuration
  requestTimeout: 30000, // 30 seconds
  
  // Security headers
  headers: {
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'X-XSS-Protection': '1; mode=block',
    'Strict-Transport-Security': 'max-age=31536000; includeSubDomains',
    'Content-Security-Policy': "default-src 'self'"
  }
};

// Input validation
export function validateInput(input: any, schema: any): void {
  const { error } = schema.validate(input, {
    abortEarly: false,
    stripUnknown: true
  });
  
  if (error) {
    throw new UserInputError('Validation failed', {
      validationErrors: error.details
    });
  }
}

// SQL injection prevention for search queries
export function sanitizeSearchQuery(query: string): string {
  // Remove potentially dangerous characters
  return query
    .replace(/[';""\\]/g, '')
    .replace(/--/g, '')
    .replace(/\/\*/g, '')
    .replace(/\*\//g, '')
    .trim();
}
```

## Performance Optimization

### Query Optimization

```typescript
// DataLoader implementation for N+1 query prevention
import DataLoader from 'dataloader';

export function createLoaders() {
  return {
    userLoader: new DataLoader(async (userIds: string[]) => {
      const users = await batchGetUsers(userIds);
      return userIds.map(id => users.find(u => u.id === id));
    }),
    
    documentLoader: new DataLoader(async (docIds: string[]) => {
      const docs = await batchGetDocuments(docIds);
      return docIds.map(id => docs.find(d => d.id === id));
    }),
    
    characterLoader: new DataLoader(async (charIds: string[]) => {
      const chars = await batchGetCharacters(charIds);
      return charIds.map(id => chars.find(c => c.id === id));
    })
  };
}

// Efficient pagination
export function createConnection(
  nodes: any[],
  pagination: PaginationInput
): Connection {
  const { first, after, last, before } = pagination;
  
  // Apply cursor-based pagination
  let startIndex = 0;
  let endIndex = nodes.length;
  
  if (after) {
    const afterIndex = nodes.findIndex(n => n.id === after);
    if (afterIndex >= 0) {
      startIndex = afterIndex + 1;
    }
  }
  
  if (before) {
    const beforeIndex = nodes.findIndex(n => n.id === before);
    if (beforeIndex >= 0) {
      endIndex = beforeIndex;
    }
  }
  
  let slicedNodes = nodes.slice(startIndex, endIndex);
  
  if (first) {
    slicedNodes = slicedNodes.slice(0, first);
  } else if (last) {
    slicedNodes = slicedNodes.slice(-last);
  }
  
  const edges = slicedNodes.map(node => ({
    node,
    cursor: node.id
  }));
  
  return {
    edges,
    pageInfo: {
      hasNextPage: endIndex < nodes.length,
      hasPreviousPage: startIndex > 0,
      startCursor: edges[0]?.cursor,
      endCursor: edges[edges.length - 1]?.cursor
    },
    totalCount: nodes.length
  };
}
```

## Testing Strategy

### API Testing

```typescript
// Integration tests
describe('GraphQL API', () => {
  let server: ApolloServer;
  let testClient: any;
  
  beforeAll(async () => {
    server = await createTestServer();
    testClient = createTestClient(server);
  });
  
  describe('Document Queries', () => {
    it('should fetch document by ID', async () => {
      const { query } = testClient;
      
      const GET_DOCUMENT = gql`
        query GetDocument($id: ID!) {
          document(id: $id) {
            id
            title
            metadata {
              genre
              targetWordCount
            }
            stats {
              totalWords
            }
          }
        }
      `;
      
      const result = await query({
        query: GET_DOCUMENT,
        variables: { id: 'test-doc-123' }
      });
      
      expect(result.errors).toBeUndefined();
      expect(result.data.document).toMatchObject({
        id: 'test-doc-123',
        title: expect.any(String)
      });
    });
  });
  
  describe('Generation Mutations', () => {
    it('should create generation job', async () => {
      const { mutate } = testClient;
      
      const CREATE_GENERATION = gql`
        mutation GenerateContent($input: GenerationInput!) {
          generateContent(input: $input) {
            id
            status
            estimatedCompletion
          }
        }
      `;
      
      const result = await mutate({
        mutation: CREATE_GENERATION,
        variables: {
          input: {
            documentId: 'test-doc-123',
            type: 'SCENE',
            context: {
              sceneId: 'scene-456',
              previousSceneId: 'scene-455'
            }
          }
        }
      });
      
      expect(result.errors).toBeUndefined();
      expect(result.data.generateContent).toMatchObject({
        id: expect.any(String),
        status: 'PENDING'
      });
    });
  });
});
```

## Deployment Configuration

### Cloud Run Deployment

```yaml
# cloudbuild.yaml - CI/CD pipeline
steps:
  # Build Docker image
  - name: 'gcr.io/cloud-builders/docker'
    args:
      - 'build'
      - '-t'
      - 'gcr.io/$PROJECT_ID/graphql-gateway:$COMMIT_SHA'
      - '-t'
      - 'gcr.io/$PROJECT_ID/graphql-gateway:latest'
      - '.'
      - '-f'
      - 'apps/api-gateway/Dockerfile'
    dir: 'apps/api-gateway'
    
  # Push to Container Registry
  - name: 'gcr.io/cloud-builders/docker'
    args:
      - 'push'
      - '--all-tags'
      - 'gcr.io/$PROJECT_ID/graphql-gateway'
    
  # Deploy to Cloud Run
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: gcloud
    args:
      - 'run'
      - 'deploy'
      - 'graphql-gateway'
      - '--image'
      - 'gcr.io/$PROJECT_ID/graphql-gateway:$COMMIT_SHA'
      - '--region'
      - 'us-central1'
      - '--platform'
      - 'managed'
      - '--allow-unauthenticated'
      - '--min-instances'
      - '2'
      - '--max-instances'
      - '100'
      - '--memory'
      - '2Gi'
      - '--cpu'
      - '2'
      - '--timeout'
      - '60'
      - '--concurrency'
      - '1000'
      - '--set-env-vars'
      - 'NODE_ENV=production,GCP_PROJECT_ID=$PROJECT_ID'
      
  # Update Cloud Endpoints
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: gcloud
    args:
      - 'endpoints'
      - 'services'
      - 'deploy'
      - 'openapi.yaml'

# Dockerfile
FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
COPY pnpm-lock.yaml ./

RUN npm install -g pnpm
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM node:18-alpine

WORKDIR /app

COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./

EXPOSE 8080

CMD ["node", "dist/server.js"]
```

## Performance Requirements

| Metric | Target | Measurement |
|--------|--------|-------------|
| Response Time (P95) | < 200ms | Cloud Monitoring |
| Response Time (P99) | < 500ms | Cloud Monitoring |
| Throughput | 10,000 RPS | Load Balancer metrics |
| Error Rate | < 0.1% | Error reporting |
| Availability | 99.95% | Uptime monitoring |
| WebSocket Connections | 50,000 | Cloud Run metrics |
| Cache Hit Rate | > 80% | Redis metrics |
| Cold Start Time | < 2s | Cloud Run metrics |

## Cost Optimization

### GCP Cost Management

```typescript
// Cost optimization strategies
export const costOptimization = {
  // Use Cloud CDN for static responses
  cdnCaching: {
    enabled: true,
    maxAge: 3600,
    staleWhileRevalidate: 86400
  },
  
  // Optimize Cloud Run instances
  cloudRun: {
    minInstances: 2, // Prevent cold starts
    maxInstances: 100, // Cap costs
    cpuThrottling: true, // Allow CPU throttling
    concurrency: 1000 // Max requests per instance
  },
  
  // Use Firestore efficiently
  firestore: {
    bundleQueries: true, // Bundle multiple queries
    cacheLocally: true, // Use local cache
    batchWrites: true // Batch write operations
  },
  
  // Optimize Cloud Functions
  cloudFunctions: {
    memoryAllocation: '256MB', // Right-size memory
    maxInstances: 50, // Limit concurrent executions
    minInstances: 0 // Scale to zero when idle
  }
};
```

## Conclusion

This API Gateway and GraphQL architecture provides a robust, scalable foundation for the Novel Creator platform on Google Cloud Platform. By leveraging Cloud Load Balancer, Cloud Run, and Cloud Endpoints, we achieve high availability, automatic scaling, and comprehensive API management. The GraphQL implementation with Apollo Server enables efficient data fetching, real-time subscriptions, and a great developer experience while maintaining security and performance standards.