# Technical Specification: Document Processing Service

## Overview

The Document Processing Service handles the storage, retrieval, and manipulation of large novel manuscripts (up to 500k+ words). It implements a chunked document architecture with virtual rendering for browser performance, real-time collaborative editing capabilities, and efficient versioning. The service uses Firestore for metadata and structure, while leveraging Cloud Storage for content chunks and full document backups.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Document Processing Service                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────────┐   │
│  │ Document API   │  │  Chunk Manager │  │ Version Control   │   │
│  │   (GraphQL)    │  │   (Streaming)  │  │    (Git-like)     │   │
│  └────────┬───────┘  └────────┬───────┘  └─────────┬─────────┘   │
│           │                    │                      │             │
│  ┌────────▼───────────────────▼──────────────────────▼─────────┐  │
│  │                  Document Processing Core                     │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │   Parser    │  │   Renderer   │  │    Differ         │ │  │
│  │  │  Engine     │  │   Engine     │  │    Engine         │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────┬──────────────────────────────────┘  │
│                             │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐  │
│  │                     Storage Layer                            │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │  Firestore  │  │Cloud Storage │  │  Memorystore      │ │  │
│  │  │ (Metadata)  │  │  (Chunks)    │  │   (Cache)         │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Document Structure

```
Document
├── Metadata (Firestore)
│   ├── id: string
│   ├── title: string
│   ├── projectId: string
│   ├── structure: DocumentStructure
│   └── stats: DocumentStats
│
├── Chunks (Cloud Storage)
│   ├── chunk_0001.json (scenes 1-10)
│   ├── chunk_0002.json (scenes 11-20)
│   └── ...
│
└── Versions (Cloud Storage)
    ├── v1/
    ├── v2/
    └── current/
```

## Data Models

### Document Schema

```typescript
// Document Metadata (Firestore)
interface DocumentMetadata {
  id: string;
  projectId: string;
  title: string;
  createdAt: Timestamp;
  updatedAt: Timestamp;
  createdBy: string;
  structure: DocumentStructure;
  stats: DocumentStats;
  settings: DocumentSettings;
  collaborators: CollaboratorInfo[];
  currentVersion: number;
  isLocked: boolean;
  lockInfo?: LockInfo;
}

interface DocumentStructure {
  acts: Act[];
  totalChapters: number;
  totalScenes: number;
  chunkMap: ChunkMapping[];
}

interface Act {
  id: string;
  title: string;
  order: number;
  chapters: Chapter[];
}

interface Chapter {
  id: string;
  title: string;
  order: number;
  scenes: SceneRef[];
  wordCount: number;
  chunkIds: string[];
}

interface SceneRef {
  id: string;
  title: string;
  order: number;
  wordCount: number;
  chunkId: string;
  offset: number; // Position within chunk
}

interface ChunkMapping {
  chunkId: string;
  startScene: string;
  endScene: string;
  size: number; // bytes
  wordCount: number;
  lastModified: Timestamp;
}

interface DocumentStats {
  totalWords: number;
  totalCharacters: number;
  totalParagraphs: number;
  avgWordsPerScene: number;
  lastAnalyzed: Timestamp;
}

interface DocumentSettings {
  chunkSize: number; // Target words per chunk (default: 10000)
  autoSave: boolean;
  autoSaveInterval: number; // seconds
  collaborationMode: 'live' | 'async';
  visibility: 'private' | 'shared' | 'public';
}

// Content Chunk (Cloud Storage)
interface ContentChunk {
  id: string;
  documentId: string;
  version: number;
  scenes: Scene[];
  metadata: ChunkMetadata;
}

interface Scene {
  id: string;
  content: SceneContent;
  metadata: SceneMetadata;
}

interface SceneContent {
  type: 'scene';
  title: string;
  paragraphs: Paragraph[];
}

interface Paragraph {
  id: string;
  type: 'narrative' | 'dialogue' | 'action' | 'description';
  text: string;
  speaker?: string; // For dialogue
  formatting: TextFormatting[];
  annotations: Annotation[];
}

interface TextFormatting {
  start: number;
  end: number;
  type: 'bold' | 'italic' | 'underline' | 'strikethrough';
}

interface Annotation {
  id: string;
  start: number;
  end: number;
  type: 'comment' | 'suggestion' | 'highlight';
  content: string;
  author: string;
  createdAt: Timestamp;
  resolved: boolean;
}

interface SceneMetadata {
  wordCount: number;
  characterCount: number;
  paragraphCount: number;
  characters: string[]; // Character IDs present
  locations: string[]; // Location IDs referenced
  lastEdited: Timestamp;
  lastEditedBy: string;
}

// Version Control
interface DocumentVersion {
  id: string;
  documentId: string;
  version: number;
  createdAt: Timestamp;
  createdBy: string;
  message: string; // Commit message
  changes: VersionChange[];
  snapshot: string; // GCS path to full snapshot
  parentVersion?: number;
}

interface VersionChange {
  type: 'add' | 'modify' | 'delete';
  path: string; // JSONPath to change
  oldValue?: any;
  newValue?: any;
  chunkId: string;
  sceneId?: string;
}

// Collaboration
interface CollaboratorInfo {
  userId: string;
  role: 'owner' | 'editor' | 'commenter' | 'viewer';
  addedAt: Timestamp;
  lastActive: Timestamp;
  cursor?: CursorPosition;
}

interface CursorPosition {
  chunkId: string;
  sceneId: string;
  paragraphId: string;
  offset: number;
  color: string;
}

interface EditOperation {
  id: string;
  documentId: string;
  userId: string;
  timestamp: Timestamp;
  operations: Operation[];
  checkpoint?: number; // For operational transformation
}

interface Operation {
  type: 'insert' | 'delete' | 'format' | 'annotate';
  path: string;
  position: number;
  content?: string;
  length?: number;
  attributes?: Record<string, any>;
}
```

## API Design

### GraphQL Schema

```graphql
type Query {
  # Document queries
  document(id: ID!): Document
  documentsByProject(projectId: ID!): [Document!]!
  
  # Chunk queries
  chunk(documentId: ID!, chunkId: ID!): ContentChunk
  chunksInRange(
    documentId: ID!
    startScene: ID!
    endScene: ID!
  ): [ContentChunk!]!
  
  # Scene queries
  scene(documentId: ID!, sceneId: ID!): Scene
  searchScenes(
    documentId: ID!
    query: String!
    limit: Int = 20
  ): [Scene!]!
  
  # Version queries
  documentVersions(
    documentId: ID!
    limit: Int = 50
  ): [DocumentVersion!]!
  
  documentAtVersion(
    documentId: ID!
    version: Int!
  ): Document
  
  # Collaboration queries
  activeCollaborators(documentId: ID!): [CollaboratorInfo!]!
  documentActivity(
    documentId: ID!
    since: DateTime!
  ): [EditOperation!]!
}

type Mutation {
  # Document operations
  createDocument(input: CreateDocumentInput!): Document!
  updateDocumentMetadata(
    id: ID!
    input: UpdateDocumentInput!
  ): Document!
  deleteDocument(id: ID!): Boolean!
  
  # Content operations
  updateScene(
    documentId: ID!
    sceneId: ID!
    content: SceneContentInput!
  ): Scene!
  
  insertScene(
    documentId: ID!
    chapterId: ID!
    position: Int!
    content: SceneContentInput!
  ): Scene!
  
  deleteScene(
    documentId: ID!
    sceneId: ID!
  ): Boolean!
  
  # Chunk operations
  updateChunk(
    documentId: ID!
    chunkId: ID!
    operations: [OperationInput!]!
  ): ContentChunk!
  
  # Version control
  createVersion(
    documentId: ID!
    message: String!
  ): DocumentVersion!
  
  revertToVersion(
    documentId: ID!
    version: Int!
  ): Document!
  
  # Collaboration
  addCollaborator(
    documentId: ID!
    userId: ID!
    role: CollaboratorRole!
  ): CollaboratorInfo!
  
  updateCollaboratorRole(
    documentId: ID!
    userId: ID!
    role: CollaboratorRole!
  ): CollaboratorInfo!
  
  removeCollaborator(
    documentId: ID!
    userId: ID!
  ): Boolean!
}

type Subscription {
  # Real-time document updates
  documentUpdates(documentId: ID!): DocumentUpdate!
  
  # Collaborative editing
  editOperations(documentId: ID!): EditOperation!
  cursorPositions(documentId: ID!): CursorUpdate!
  
  # Activity monitoring
  collaboratorActivity(documentId: ID!): CollaboratorActivity!
}

# Input types
input CreateDocumentInput {
  projectId: ID!
  title: String!
  structure: DocumentStructureInput
  settings: DocumentSettingsInput
}

input SceneContentInput {
  title: String!
  paragraphs: [ParagraphInput!]!
}

input ParagraphInput {
  type: ParagraphType!
  text: String!
  speaker: String
  formatting: [TextFormattingInput!]
}

input OperationInput {
  type: OperationType!
  path: String!
  position: Int!
  content: String
  length: Int
  attributes: JSON
}

# Enums
enum CollaboratorRole {
  OWNER
  EDITOR
  COMMENTER
  VIEWER
}

enum ParagraphType {
  NARRATIVE
  DIALOGUE
  ACTION
  DESCRIPTION
}

enum OperationType {
  INSERT
  DELETE
  FORMAT
  ANNOTATE
}
```

### REST API Endpoints

```yaml
# Document Management
GET /api/v1/documents/{documentId}
Response: Document with metadata

GET /api/v1/documents/{documentId}/structure
Response: Full document structure

POST /api/v1/documents
Request: CreateDocumentRequest
Response: Document

DELETE /api/v1/documents/{documentId}
Response: 204 No Content

# Chunk Operations
GET /api/v1/documents/{documentId}/chunks/{chunkId}
Response: ContentChunk

GET /api/v1/documents/{documentId}/chunks
Query:
  - startScene: string
  - endScene: string
  - limit: number
Response: ContentChunk[]

PUT /api/v1/documents/{documentId}/chunks/{chunkId}
Request: ChunkUpdate
Response: ContentChunk

# Export Operations
GET /api/v1/documents/{documentId}/export
Query:
  - format: 'json' | 'markdown' | 'docx'
  - includeAnnotations: boolean
Response: Redirect to signed Cloud Storage URL

# Version Control
GET /api/v1/documents/{documentId}/versions
Response: DocumentVersion[]

POST /api/v1/documents/{documentId}/versions
Request: CreateVersionRequest
Response: DocumentVersion

POST /api/v1/documents/{documentId}/versions/{version}/restore
Response: Document

# Real-time Collaboration
WS /api/v1/documents/{documentId}/collaborate
Messages:
  - operation: Edit operation
  - cursor: Cursor position update
  - presence: User presence update
```

## Implementation Details

### Document Storage Strategy

```typescript
@Injectable()
export class DocumentStorageService {
  private firestore: Firestore;
  private storage: Storage;
  private cache: Redis;
  
  constructor() {
    this.firestore = new Firestore();
    this.storage = new Storage();
    this.cache = new Redis({
      host: process.env.REDIS_HOST
    });
  }
  
  async createDocument(
    input: CreateDocumentInput,
    userId: string
  ): Promise<Document> {
    // Create document metadata in Firestore
    const docRef = this.firestore.collection('documents').doc();
    const documentId = docRef.id;
    
    const metadata: DocumentMetadata = {
      id: documentId,
      projectId: input.projectId,
      title: input.title,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
      createdBy: userId,
      structure: input.structure || this.createDefaultStructure(),
      stats: this.createEmptyStats(),
      settings: {
        chunkSize: 10000,
        autoSave: true,
        autoSaveInterval: 30,
        collaborationMode: 'live',
        visibility: 'private',
        ...input.settings
      },
      collaborators: [{
        userId,
        role: 'owner',
        addedAt: FieldValue.serverTimestamp(),
        lastActive: FieldValue.serverTimestamp()
      }],
      currentVersion: 1,
      isLocked: false
    };
    
    // Create initial empty chunk
    const initialChunk = await this.createInitialChunk(documentId);
    
    // Save metadata
    await docRef.set(metadata);
    
    // Create initial version
    await this.createVersion(documentId, userId, 'Initial document creation');
    
    return {
      metadata,
      chunks: [initialChunk]
    };
  }
  
  async loadDocumentChunks(
    documentId: string,
    chunkIds: string[]
  ): Promise<ContentChunk[]> {
    // Try cache first
    const cachedChunks = await this.loadChunksFromCache(documentId, chunkIds);
    const missingIds = chunkIds.filter(id => !cachedChunks.has(id));
    
    if (missingIds.length === 0) {
      return Array.from(cachedChunks.values());
    }
    
    // Load missing chunks from Cloud Storage
    const bucket = this.storage.bucket(process.env.DOCUMENT_BUCKET!);
    const loadPromises = missingIds.map(async (chunkId) => {
      const file = bucket.file(`documents/${documentId}/chunks/${chunkId}.json`);
      const [content] = await file.download();
      const chunk = JSON.parse(content.toString()) as ContentChunk;
      
      // Cache for future use
      await this.cacheChunk(documentId, chunkId, chunk);
      
      return chunk;
    });
    
    const loadedChunks = await Promise.all(loadPromises);
    
    // Combine cached and loaded chunks
    return [
      ...Array.from(cachedChunks.values()),
      ...loadedChunks
    ];
  }
  
  async updateChunk(
    documentId: string,
    chunkId: string,
    operations: Operation[]
  ): Promise<ContentChunk> {
    // Load current chunk
    const chunk = await this.loadChunk(documentId, chunkId);
    
    // Apply operations using Operational Transformation
    const updatedChunk = this.applyOperations(chunk, operations);
    
    // Validate chunk size
    if (this.calculateChunkSize(updatedChunk) > MAX_CHUNK_SIZE) {
      // Split chunk if too large
      const [chunk1, chunk2] = await this.splitChunk(updatedChunk);
      await this.saveChunk(documentId, chunk1);
      await this.saveChunk(documentId, chunk2);
      
      // Update document structure
      await this.updateDocumentStructure(documentId, chunkId, [chunk1.id, chunk2.id]);
      
      return chunk1;
    }
    
    // Save updated chunk
    await this.saveChunk(documentId, updatedChunk);
    
    // Invalidate cache
    await this.invalidateChunkCache(documentId, chunkId);
    
    return updatedChunk;
  }
  
  private async saveChunk(
    documentId: string,
    chunk: ContentChunk
  ): Promise<void> {
    const bucket = this.storage.bucket(process.env.DOCUMENT_BUCKET!);
    const file = bucket.file(`documents/${documentId}/chunks/${chunk.id}.json`);
    
    await file.save(JSON.stringify(chunk), {
      contentType: 'application/json',
      metadata: {
        documentId,
        chunkId: chunk.id,
        wordCount: chunk.metadata.wordCount.toString(),
        lastModified: new Date().toISOString()
      }
    });
  }
}
```

### Virtual Document Rendering

```typescript
@Injectable()
export class VirtualDocumentRenderer {
  constructor(
    private storageService: DocumentStorageService,
    private cache: CacheService
  ) {}
  
  async renderDocumentView(
    documentId: string,
    viewportConfig: ViewportConfig
  ): Promise<RenderedDocument> {
    // Get document metadata
    const metadata = await this.storageService.getDocumentMetadata(documentId);
    
    // Calculate visible chunks based on viewport
    const visibleChunks = this.calculateVisibleChunks(
      metadata.structure,
      viewportConfig
    );
    
    // Load only visible chunks
    const chunks = await this.storageService.loadDocumentChunks(
      documentId,
      visibleChunks.map(c => c.chunkId)
    );
    
    // Pre-fetch adjacent chunks for smooth scrolling
    this.prefetchAdjacentChunks(documentId, visibleChunks);
    
    // Render document with placeholders for non-loaded content
    return {
      metadata,
      renderedContent: this.renderChunks(chunks),
      totalHeight: this.calculateTotalHeight(metadata.structure),
      loadedRange: {
        start: visibleChunks[0].startScene,
        end: visibleChunks[visibleChunks.length - 1].endScene
      }
    };
  }
  
  private calculateVisibleChunks(
    structure: DocumentStructure,
    viewport: ViewportConfig
  ): ChunkMapping[] {
    const visibleChunks: ChunkMapping[] = [];
    let currentHeight = 0;
    
    for (const chunk of structure.chunkMap) {
      const chunkHeight = this.estimateChunkHeight(chunk);
      
      if (currentHeight + chunkHeight >= viewport.scrollTop &&
          currentHeight <= viewport.scrollTop + viewport.height) {
        visibleChunks.push(chunk);
      }
      
      currentHeight += chunkHeight;
      
      if (currentHeight > viewport.scrollTop + viewport.height) {
        break;
      }
    }
    
    return visibleChunks;
  }
  
  private async prefetchAdjacentChunks(
    documentId: string,
    visibleChunks: ChunkMapping[]
  ): Promise<void> {
    const allChunks = await this.storageService.getDocumentStructure(documentId);
    const firstVisible = visibleChunks[0];
    const lastVisible = visibleChunks[visibleChunks.length - 1];
    
    const firstIndex = allChunks.chunkMap.findIndex(c => c.chunkId === firstVisible.chunkId);
    const lastIndex = allChunks.chunkMap.findIndex(c => c.chunkId === lastVisible.chunkId);
    
    // Prefetch 2 chunks before and after
    const prefetchIds: string[] = [];
    
    for (let i = Math.max(0, firstIndex - 2); i < firstIndex; i++) {
      prefetchIds.push(allChunks.chunkMap[i].chunkId);
    }
    
    for (let i = lastIndex + 1; i <= Math.min(allChunks.chunkMap.length - 1, lastIndex + 2); i++) {
      prefetchIds.push(allChunks.chunkMap[i].chunkId);
    }
    
    // Prefetch in background
    if (prefetchIds.length > 0) {
      setImmediate(() => {
        this.storageService.loadDocumentChunks(documentId, prefetchIds)
          .catch(err => console.error('Prefetch error:', err));
      });
    }
  }
}

interface ViewportConfig {
  scrollTop: number;
  height: number;
  bufferSize: number; // Extra content to render outside viewport
}

interface RenderedDocument {
  metadata: DocumentMetadata;
  renderedContent: RenderedContent;
  totalHeight: number;
  loadedRange: {
    start: string;
    end: string;
  };
}
```

### Operational Transformation for Collaboration

```typescript
@Injectable()
export class CollaborationEngine {
  private operations: Map<string, Operation[]> = new Map();
  private documentLocks: Map<string, LockInfo> = new Map();
  
  constructor(
    private firestore: Firestore,
    private pubsub: PubSub
  ) {}
  
  async applyOperation(
    documentId: string,
    userId: string,
    operation: Operation
  ): Promise<TransformedOperation> {
    // Get current document version
    const docVersion = await this.getDocumentVersion(documentId);
    
    // Transform operation against concurrent operations
    const transformed = await this.transformOperation(
      operation,
      this.operations.get(documentId) || [],
      docVersion
    );
    
    // Apply operation
    await this.executeOperation(documentId, transformed);
    
    // Broadcast to other collaborators
    await this.broadcastOperation(documentId, userId, transformed);
    
    // Store operation for conflict resolution
    this.storeOperation(documentId, transformed);
    
    return transformed;
  }
  
  private async transformOperation(
    operation: Operation,
    concurrentOps: Operation[],
    baseVersion: number
  ): Promise<TransformedOperation> {
    let transformed = operation;
    
    // Apply OT algorithm
    for (const concurrent of concurrentOps) {
      if (concurrent.timestamp > operation.timestamp) {
        transformed = this.transform(transformed, concurrent);
      }
    }
    
    return {
      ...transformed,
      baseVersion,
      transformedAt: new Date()
    };
  }
  
  private transform(op1: Operation, op2: Operation): Operation {
    // Implement OT transformation rules
    if (op1.type === 'insert' && op2.type === 'insert') {
      if (op1.position < op2.position) {
        return op1;
      } else if (op1.position > op2.position) {
        return {
          ...op1,
          position: op1.position + op2.content!.length
        };
      } else {
        // Same position - use timestamp to determine order
        return op1.timestamp < op2.timestamp ? op1 : {
          ...op1,
          position: op1.position + op2.content!.length
        };
      }
    }
    
    if (op1.type === 'delete' && op2.type === 'insert') {
      if (op1.position < op2.position) {
        return op1;
      } else {
        return {
          ...op1,
          position: op1.position + op2.content!.length
        };
      }
    }
    
    if (op1.type === 'insert' && op2.type === 'delete') {
      if (op1.position <= op2.position) {
        return op1;
      } else if (op1.position > op2.position + op2.length!) {
        return {
          ...op1,
          position: op1.position - op2.length!
        };
      } else {
        return {
          ...op1,
          position: op2.position
        };
      }
    }
    
    if (op1.type === 'delete' && op2.type === 'delete') {
      if (op1.position < op2.position) {
        return op1;
      } else if (op1.position > op2.position) {
        return {
          ...op1,
          position: op1.position - Math.min(op2.length!, op1.position - op2.position)
        };
      } else {
        // Overlapping deletes
        return {
          ...op1,
          length: Math.max(0, op1.length! - op2.length!)
        };
      }
    }
    
    return op1;
  }
  
  async broadcastOperation(
    documentId: string,
    userId: string,
    operation: TransformedOperation
  ): Promise<void> {
    const topic = this.pubsub.topic(`document-${documentId}`);
    
    await topic.publish(Buffer.from(JSON.stringify({
      type: 'operation',
      userId,
      operation,
      timestamp: new Date()
    })));
  }
  
  subscribeToDocument(
    documentId: string,
    userId: string,
    callback: (update: DocumentUpdate) => void
  ): () => void {
    const subscription = this.pubsub
      .topic(`document-${documentId}`)
      .subscription(`user-${userId}`)
      .on('message', (message) => {
        const update = JSON.parse(message.data.toString());
        if (update.userId !== userId) {
          callback(update);
        }
        message.ack();
      });
    
    return () => subscription.removeAllListeners();
  }
}
```

### Version Control System

```typescript
@Injectable()
export class DocumentVersionControl {
  constructor(
    private firestore: Firestore,
    private storage: Storage
  ) {}
  
  async createVersion(
    documentId: string,
    userId: string,
    message: string
  ): Promise<DocumentVersion> {
    // Get current document state
    const currentState = await this.captureDocumentState(documentId);
    
    // Get previous version
    const previousVersion = await this.getLatestVersion(documentId);
    
    // Calculate diff
    const changes = await this.calculateChanges(
      previousVersion?.snapshot,
      currentState
    );
    
    // Create version record
    const version: DocumentVersion = {
      id: uuidv4(),
      documentId,
      version: (previousVersion?.version || 0) + 1,
      createdAt: new Date(),
      createdBy: userId,
      message,
      changes,
      snapshot: await this.saveSnapshot(documentId, currentState),
      parentVersion: previousVersion?.version
    };
    
    // Save version metadata
    await this.firestore
      .collection('documents')
      .doc(documentId)
      .collection('versions')
      .doc(version.version.toString())
      .set(version);
    
    // Update document current version
    await this.firestore
      .collection('documents')
      .doc(documentId)
      .update({
        currentVersion: version.version,
        updatedAt: FieldValue.serverTimestamp()
      });
    
    return version;
  }
  
  async revertToVersion(
    documentId: string,
    targetVersion: number,
    userId: string
  ): Promise<Document> {
    // Get target version
    const version = await this.getVersion(documentId, targetVersion);
    if (!version) {
      throw new Error('Version not found');
    }
    
    // Load snapshot
    const snapshot = await this.loadSnapshot(version.snapshot);
    
    // Create new version for the revert
    await this.createVersion(
      documentId,
      userId,
      `Reverted to version ${targetVersion}`
    );
    
    // Apply snapshot to current document
    await this.applySnapshot(documentId, snapshot);
    
    return this.loadDocument(documentId);
  }
  
  private async captureDocumentState(documentId: string): Promise<DocumentSnapshot> {
    const metadata = await this.firestore
      .collection('documents')
      .doc(documentId)
      .get();
    
    const chunks = await this.loadAllChunks(documentId);
    
    return {
      metadata: metadata.data() as DocumentMetadata,
      chunks,
      capturedAt: new Date()
    };
  }
  
  private async calculateChanges(
    previousSnapshot: string | undefined,
    currentState: DocumentSnapshot
  ): Promise<VersionChange[]> {
    if (!previousSnapshot) {
      return [{
        type: 'add',
        path: '/',
        newValue: currentState,
        chunkId: 'all'
      }];
    }
    
    const previous = await this.loadSnapshot(previousSnapshot);
    const changes: VersionChange[] = [];
    
    // Use diff algorithm to find changes
    const diff = this.diffDocuments(previous, currentState);
    
    for (const change of diff) {
      changes.push({
        type: change.type,
        path: change.path,
        oldValue: change.oldValue,
        newValue: change.newValue,
        chunkId: this.identifyAffectedChunk(change.path),
        sceneId: this.identifyAffectedScene(change.path)
      });
    }
    
    return changes;
  }
  
  private async saveSnapshot(
    documentId: string,
    state: DocumentSnapshot
  ): Promise<string> {
    const bucket = this.storage.bucket(process.env.DOCUMENT_BUCKET!);
    const path = `documents/${documentId}/versions/${state.capturedAt.getTime()}.json`;
    const file = bucket.file(path);
    
    await file.save(JSON.stringify(state), {
      contentType: 'application/json',
      metadata: {
        documentId,
        capturedAt: state.capturedAt.toISOString()
      }
    });
    
    return `gs://${process.env.DOCUMENT_BUCKET}/${path}`;
  }
}
```

### Search and Indexing

```typescript
@Injectable()
export class DocumentSearchService {
  private searchClient: SearchClient;
  
  constructor() {
    this.searchClient = new SearchClient({
      projectId: process.env.GCP_PROJECT_ID,
      searchIndex: 'novel-documents'
    });
  }
  
  async indexDocument(documentId: string): Promise<void> {
    const chunks = await this.loadAllChunks(documentId);
    const indexEntries: SearchEntry[] = [];
    
    for (const chunk of chunks) {
      for (const scene of chunk.scenes) {
        indexEntries.push({
          id: `${documentId}_${scene.id}`,
          documentId,
          sceneId: scene.id,
          chunkId: chunk.id,
          title: scene.content.title,
          content: this.extractTextContent(scene),
          characters: scene.metadata.characters,
          locations: scene.metadata.locations,
          wordCount: scene.metadata.wordCount,
          type: 'scene'
        });
        
        // Index individual paragraphs for fine-grained search
        for (const para of scene.content.paragraphs) {
          if (para.text.length > 50) { // Only index substantial paragraphs
            indexEntries.push({
              id: `${documentId}_${scene.id}_${para.id}`,
              documentId,
              sceneId: scene.id,
              paragraphId: para.id,
              chunkId: chunk.id,
              content: para.text,
              type: para.type,
              speaker: para.speaker
            });
          }
        }
      }
    }
    
    // Batch index
    await this.searchClient.indexDocuments(indexEntries);
  }
  
  async searchScenes(
    documentId: string,
    query: string,
    options: SearchOptions
  ): Promise<SearchResult[]> {
    const searchQuery = {
      query,
      filters: {
        documentId,
        type: options.searchType || 'scene'
      },
      limit: options.limit || 20,
      offset: options.offset || 0,
      highlight: {
        fields: ['content', 'title'],
        preTag: '<mark>',
        postTag: '</mark>'
      }
    };
    
    const results = await this.searchClient.search(searchQuery);
    
    return results.hits.map(hit => ({
      sceneId: hit.sceneId,
      chunkId: hit.chunkId,
      title: hit.title,
      excerpt: hit.highlight?.content || hit.content.substring(0, 200),
      score: hit.score,
      metadata: {
        wordCount: hit.wordCount,
        characters: hit.characters,
        locations: hit.locations
      }
    }));
  }
}
```

## Performance Optimization

### Chunk Management Strategy

```typescript
@Injectable()
export class ChunkOptimizationService {
  private readonly OPTIMAL_CHUNK_SIZE = 10000; // words
  private readonly MAX_CHUNK_SIZE = 15000;
  private readonly MIN_CHUNK_SIZE = 5000;
  
  async optimizeChunking(documentId: string): Promise<void> {
    const metadata = await this.getDocumentMetadata(documentId);
    const chunks = await this.loadAllChunks(documentId);
    
    // Analyze current chunking efficiency
    const analysis = this.analyzeChunking(chunks);
    
    if (analysis.needsOptimization) {
      const optimizedChunks = await this.rebalanceChunks(chunks);
      await this.applyOptimizedChunking(documentId, optimizedChunks);
    }
  }
  
  private analyzeChunking(chunks: ContentChunk[]): ChunkAnalysis {
    const sizes = chunks.map(c => c.metadata.wordCount);
    const avgSize = sizes.reduce((a, b) => a + b, 0) / sizes.length;
    const variance = this.calculateVariance(sizes, avgSize);
    
    return {
      needsOptimization: variance > 0.3 || 
                        sizes.some(s => s > this.MAX_CHUNK_SIZE || s < this.MIN_CHUNK_SIZE),
      currentAvgSize: avgSize,
      variance,
      problematicChunks: chunks.filter(c => 
        c.metadata.wordCount > this.MAX_CHUNK_SIZE || 
        c.metadata.wordCount < this.MIN_CHUNK_SIZE
      ).map(c => c.id)
    };
  }
  
  private async rebalanceChunks(chunks: ContentChunk[]): Promise<ContentChunk[]> {
    const allScenes: Scene[] = [];
    
    // Extract all scenes
    for (const chunk of chunks) {
      allScenes.push(...chunk.scenes);
    }
    
    // Regroup scenes into optimal chunks
    const optimizedChunks: ContentChunk[] = [];
    let currentChunk: Scene[] = [];
    let currentWordCount = 0;
    
    for (const scene of allScenes) {
      if (currentWordCount + scene.metadata.wordCount > this.OPTIMAL_CHUNK_SIZE &&
          currentChunk.length > 0) {
        // Create new chunk
        optimizedChunks.push(this.createChunk(currentChunk));
        currentChunk = [scene];
        currentWordCount = scene.metadata.wordCount;
      } else {
        currentChunk.push(scene);
        currentWordCount += scene.metadata.wordCount;
      }
    }
    
    // Add final chunk
    if (currentChunk.length > 0) {
      optimizedChunks.push(this.createChunk(currentChunk));
    }
    
    return optimizedChunks;
  }
}
```

### Caching Strategy

```typescript
@Injectable()
export class DocumentCacheService {
  private redis: Redis;
  private localCache: LRUCache<string, any>;
  
  constructor() {
    this.redis = new Redis({
      host: process.env.REDIS_HOST,
      keyPrefix: 'doc:'
    });
    
    this.localCache = new LRUCache({
      max: 100, // Maximum number of items
      ttl: 1000 * 60 * 5, // 5 minutes
      updateAgeOnGet: true
    });
  }
  
  async cacheChunk(
    documentId: string,
    chunkId: string,
    chunk: ContentChunk,
    ttl: number = 3600
  ): Promise<void> {
    const key = `chunk:${documentId}:${chunkId}`;
    
    // Local cache for immediate access
    this.localCache.set(key, chunk);
    
    // Redis cache for distributed access
    await this.redis.setex(
      key,
      ttl,
      JSON.stringify(chunk)
    );
  }
  
  async getCachedChunk(
    documentId: string,
    chunkId: string
  ): Promise<ContentChunk | null> {
    const key = `chunk:${documentId}:${chunkId}`;
    
    // Check local cache first
    const local = this.localCache.get(key);
    if (local) {
      return local;
    }
    
    // Check Redis cache
    const cached = await this.redis.get(key);
    if (cached) {
      const chunk = JSON.parse(cached);
      this.localCache.set(key, chunk);
      return chunk;
    }
    
    return null;
  }
  
  async preloadDocumentCache(
    documentId: string,
    chunkIds: string[]
  ): Promise<void> {
    // Use Redis pipeline for efficiency
    const pipeline = this.redis.pipeline();
    
    for (const chunkId of chunkIds) {
      pipeline.get(`chunk:${documentId}:${chunkId}`);
    }
    
    const results = await pipeline.exec();
    
    // Populate local cache
    results?.forEach((result, index) => {
      if (result[1]) {
        const key = `chunk:${documentId}:${chunkIds[index]}`;
        this.localCache.set(key, JSON.parse(result[1] as string));
      }
    });
  }
}
```

## Security and Access Control

### Document-Level Security

```typescript
@Injectable()
export class DocumentSecurityService {
  constructor(
    private firestore: Firestore,
    private authService: AuthService
  ) {}
  
  async checkDocumentAccess(
    userId: string,
    documentId: string,
    requiredRole: CollaboratorRole
  ): Promise<boolean> {
    const doc = await this.firestore
      .collection('documents')
      .doc(documentId)
      .get();
    
    if (!doc.exists) {
      return false;
    }
    
    const metadata = doc.data() as DocumentMetadata;
    const collaborator = metadata.collaborators.find(c => c.userId === userId);
    
    if (!collaborator) {
      return false;
    }
    
    return this.hasRequiredRole(collaborator.role, requiredRole);
  }
  
  private hasRequiredRole(
    userRole: CollaboratorRole,
    requiredRole: CollaboratorRole
  ): boolean {
    const roleHierarchy = {
      'viewer': 0,
      'commenter': 1,
      'editor': 2,
      'owner': 3
    };
    
    return roleHierarchy[userRole] >= roleHierarchy[requiredRole];
  }
  
  async enforceDocumentQuota(
    userId: string,
    projectId: string
  ): Promise<void> {
    const userDocs = await this.firestore
      .collection('documents')
      .where('createdBy', '==', userId)
      .where('projectId', '==', projectId)
      .count()
      .get();
    
    const subscription = await this.authService.getUserSubscription(userId);
    const maxDocuments = this.getMaxDocuments(subscription.plan);
    
    if (userDocs.data().count >= maxDocuments) {
      throw new Error(`Document limit reached. Maximum ${maxDocuments} documents allowed for ${subscription.plan} plan.`);
    }
  }
}
```

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Document access rules
    match /documents/{documentId} {
      allow read: if request.auth != null && 
        hasDocumentAccess(documentId, 'viewer');
      
      allow update: if request.auth != null && 
        hasDocumentAccess(documentId, 'editor') &&
        !resource.data.isLocked;
      
      allow delete: if request.auth != null && 
        hasDocumentAccess(documentId, 'owner');
      
      // Version control - read only
      match /versions/{version} {
        allow read: if request.auth != null && 
          hasDocumentAccess(documentId, 'viewer');
        allow write: if false; // System only
      }
      
      // Collaboration subcollection
      match /collaboration/{recordId} {
        allow read: if request.auth != null && 
          hasDocumentAccess(documentId, 'viewer');
        allow create: if request.auth != null && 
          hasDocumentAccess(documentId, 'editor') &&
          request.resource.data.userId == request.auth.uid;
      }
    }
    
    // Helper function
    function hasDocumentAccess(documentId, requiredRole) {
      let doc = get(/databases/$(database)/documents/documents/$(documentId));
      let collaborator = doc.data.collaborators[request.auth.uid];
      
      return collaborator != null && 
        roleLevel(collaborator.role) >= roleLevel(requiredRole);
    }
    
    function roleLevel(role) {
      return role == 'owner' ? 3 : 
             role == 'editor' ? 2 :
             role == 'commenter' ? 1 : 0;
    }
  }
}
```

## Monitoring and Observability

### Performance Metrics

```typescript
@Injectable()
export class DocumentMetricsService {
  private metrics: MetricsClient;
  
  constructor() {
    this.metrics = new MetricsClient({
      projectId: process.env.GCP_PROJECT_ID
    });
  }
  
  async recordDocumentOperation(
    operation: string,
    documentId: string,
    duration: number,
    metadata?: Record<string, any>
  ): Promise<void> {
    await this.metrics.createTimeSeries({
      metric: {
        type: 'custom.googleapis.com/document/operation',
        labels: {
          operation,
          documentId
        }
      },
      points: [{
        interval: { endTime: { seconds: Date.now() / 1000 } },
        value: { doubleValue: duration }
      }]
    });
  }
  
  async trackDocumentSize(documentId: string): Promise<void> {
    const stats = await this.calculateDocumentStats(documentId);
    
    await this.metrics.createTimeSeries({
      metric: {
        type: 'custom.googleapis.com/document/size',
        labels: { documentId }
      },
      points: [{
        interval: { endTime: { seconds: Date.now() / 1000 } },
        value: { 
          int64Value: stats.totalWords 
        }
      }]
    });
  }
}
```

## Testing Strategy

### Unit Tests

```typescript
describe('DocumentStorageService', () => {
  let service: DocumentStorageService;
  let mockFirestore: any;
  let mockStorage: any;
  
  beforeEach(() => {
    mockFirestore = createMockFirestore();
    mockStorage = createMockStorage();
    service = new DocumentStorageService(mockFirestore, mockStorage);
  });
  
  describe('createDocument', () => {
    it('should create document with default structure', async () => {
      const input = {
        projectId: 'proj-123',
        title: 'Test Novel'
      };
      
      const result = await service.createDocument(input, 'user-123');
      
      expect(result.metadata.title).toBe('Test Novel');
      expect(result.metadata.structure.acts).toHaveLength(3);
      expect(result.chunks).toHaveLength(1);
    });
  });
  
  describe('chunk operations', () => {
    it('should split large chunks automatically', async () => {
      const largeContent = generateLargeContent(20000); // 20k words
      
      const result = await service.updateChunk(
        'doc-123',
        'chunk-123',
        [{ type: 'insert', position: 0, content: largeContent }]
      );
      
      expect(mockStorage.save).toHaveBeenCalledTimes(2); // Split into 2 chunks
    });
  });
});
```

## Performance Requirements

| Metric | Target | Measurement |
|--------|--------|-------------|
| Document load time | < 500ms | P95 for first chunk |
| Chunk load time | < 100ms | P95 per chunk |
| Save latency | < 200ms | P95 for single scene |
| Search response | < 300ms | P95 for document search |
| Collaboration lag | < 150ms | P95 for operation sync |
| Version creation | < 2s | P95 for full document |
| Export generation | < 30s | P95 for 200k words |
| Memory usage | < 100MB | Per document in browser |

## Deployment Configuration

### Cloud Function Configuration

```yaml
# document-service/function.yaml
name: document-service
runtime: nodejs18
memory: 4096MB
timeout: 300s
max_instances: 50
min_instances: 2

environment_variables:
  NODE_ENV: production
  DOCUMENT_BUCKET: novel-documents-prod
  CHUNK_SIZE: "10000"
  MAX_DOCUMENT_SIZE: "10485760" # 10MB
  REDIS_HOST: "10.0.0.6"

service_account: document-service@deus-ex-machina.iam.gserviceaccount.com
```

## Conclusion

The Document Processing Service provides a robust, scalable solution for handling large novel manuscripts with real-time collaboration. By leveraging Firestore for metadata and structure, Cloud Storage for content chunks, and implementing virtual rendering with intelligent caching, the service delivers excellent performance even for 500k+ word documents. The chunked architecture ensures efficient memory usage in browsers while the operational transformation system enables seamless collaborative editing.