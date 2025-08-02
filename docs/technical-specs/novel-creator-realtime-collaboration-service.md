# Technical Specification: Real-time Collaboration Service

## Overview

The Real-time Collaboration Service enables multiple users to simultaneously work on novel manuscripts with live cursor tracking, conflict-free editing through Operational Transformation (OT), and instant synchronization. Built on WebSocket connections with Firestore for persistence and Pub/Sub for distributed messaging, it provides a seamless collaborative writing experience with support for 100+ concurrent users per document.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Real-time Collaboration Service                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────────┐   │
│  │ WebSocket      │  │   Session      │  │   Presence       │   │
│  │  Gateway       │  │   Manager      │  │   Tracker        │   │
│  └────────┬───────┘  └────────┬───────┘  └─────────┬─────────┘   │
│           │                    │                      │             │
│  ┌────────▼───────────────────▼──────────────────────▼─────────┐  │
│  │              Collaboration Coordination Engine                │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │     OT      │  │   Conflict   │  │    Awareness      │ │  │
│  │  │   Engine    │  │  Resolution  │  │    System         │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────┬──────────────────────────────────┘  │
│                             │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐  │
│  │                  Communication Layer                         │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │   Pub/Sub   │  │  Firestore   │  │   Memorystore     │ │  │
│  │  │ (Messaging) │  │ (Persistence)│  │    (Sessions)     │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Connection Flow

```
┌─────────┐      ┌──────────┐      ┌─────────────┐      ┌──────────┐
│ Client  │─────▶│ WebSocket│─────▶│   Session   │─────▶│ Document │
│ (React) │      │ Gateway  │      │   Manager   │      │ Channel  │
└─────────┘      └──────────┘      └─────────────┘      └──────────┘
     │                                                          │
     │                    Real-time Updates                     │
     └──────────────────────────────────────────────────────────┘
```

## Data Models

### Collaboration Schema

```typescript
// Session Management
interface CollaborationSession {
  id: string;
  documentId: string;
  userId: string;
  connectionId: string;
  role: 'owner' | 'editor' | 'commenter' | 'viewer';
  state: 'active' | 'idle' | 'disconnected';
  connectedAt: Date;
  lastActivity: Date;
  clientInfo: ClientInfo;
  permissions: SessionPermissions;
}

interface ClientInfo {
  userAgent: string;
  ipAddress: string;
  timezone: string;
  locale: string;
  screenResolution?: {
    width: number;
    height: number;
  };
}

interface SessionPermissions {
  canEdit: boolean;
  canComment: boolean;
  canSuggest: boolean;
  canViewOthers: boolean;
  canKickOthers: boolean;
}

// Presence and Awareness
interface UserPresence {
  userId: string;
  sessionId: string;
  documentId: string;
  displayName: string;
  avatar?: string;
  color: string; // Unique color for cursor/selection
  cursor?: CursorPosition;
  selection?: TextSelection;
  viewport?: ViewportInfo;
  status: UserStatus;
  lastSeen: Date;
}

interface CursorPosition {
  chunkId: string;
  sceneId: string;
  paragraphId: string;
  offset: number;
  timestamp: Date;
}

interface TextSelection {
  start: CursorPosition;
  end: CursorPosition;
  text: string;
}

interface ViewportInfo {
  chunkId: string;
  scrollTop: number;
  height: number;
}

interface UserStatus {
  state: 'active' | 'idle' | 'away';
  message?: string; // "John is typing...", "Jane is reviewing Chapter 3"
  isTyping: boolean;
  lastTyping?: Date;
}

// Operational Transformation
interface Operation {
  id: string;
  sessionId: string;
  documentId: string;
  revision: number; // Document revision this op is based on
  timestamp: Date;
  type: 'insert' | 'delete' | 'format' | 'annotation';
  path: OperationPath;
  data: OperationData;
  transformations?: TransformationLog[];
}

interface OperationPath {
  chunkId: string;
  sceneId: string;
  paragraphId: string;
  offset: number;
}

interface OperationData {
  // For insert
  text?: string;
  // For delete
  length?: number;
  // For format
  format?: TextFormat;
  // For annotation
  annotation?: Annotation;
}

interface TextFormat {
  type: 'bold' | 'italic' | 'underline' | 'strikethrough' | 'highlight';
  value: boolean | string; // boolean for toggles, string for highlight color
}

interface TransformationLog {
  againstOperation: string; // Operation ID
  originalPosition: number;
  transformedPosition: number;
  reason: string;
}

// Collaboration Events
interface CollaborationEvent {
  id: string;
  documentId: string;
  type: CollaborationEventType;
  sessionId: string;
  userId: string;
  timestamp: Date;
  data: any;
}

type CollaborationEventType = 
  | 'user.joined'
  | 'user.left'
  | 'user.idle'
  | 'user.active'
  | 'cursor.moved'
  | 'selection.changed'
  | 'operation.applied'
  | 'conflict.resolved'
  | 'document.locked'
  | 'document.unlocked';

// Conflict Resolution
interface EditConflict {
  id: string;
  documentId: string;
  operations: ConflictingOperation[];
  detectedAt: Date;
  resolution?: ConflictResolution;
  autoResolved: boolean;
}

interface ConflictingOperation {
  operation: Operation;
  sessionId: string;
  userId: string;
}

interface ConflictResolution {
  strategy: 'transform' | 'merge' | 'choose' | 'manual';
  winningOperation?: string;
  mergedResult?: Operation;
  resolvedBy?: string; // userId or 'system'
  resolvedAt: Date;
}

// Comments and Suggestions
interface Comment {
  id: string;
  documentId: string;
  threadId?: string; // For threaded discussions
  userId: string;
  content: string;
  range: TextSelection;
  createdAt: Date;
  updatedAt: Date;
  resolved: boolean;
  resolvedBy?: string;
  resolvedAt?: Date;
  replies: CommentReply[];
}

interface CommentReply {
  id: string;
  userId: string;
  content: string;
  createdAt: Date;
}

interface Suggestion {
  id: string;
  documentId: string;
  userId: string;
  type: 'edit' | 'addition' | 'deletion';
  original: string;
  suggested: string;
  range: TextSelection;
  reason?: string;
  status: 'pending' | 'accepted' | 'rejected';
  createdAt: Date;
  reviewedBy?: string;
  reviewedAt?: Date;
}
```

## API Design

### WebSocket Protocol

```typescript
// WebSocket Message Types
interface WebSocketMessage {
  id: string; // Unique message ID for acknowledgment
  type: MessageType;
  timestamp: Date;
  data: any;
}

type MessageType = 
  // Connection
  | 'connect'
  | 'disconnect'
  | 'heartbeat'
  | 'acknowledge'
  // Operations
  | 'operation'
  | 'operation.ack'
  | 'operation.reject'
  // Presence
  | 'presence.update'
  | 'presence.query'
  | 'cursor.update'
  | 'selection.update'
  // Collaboration
  | 'comment.create'
  | 'comment.reply'
  | 'comment.resolve'
  | 'suggestion.create'
  | 'suggestion.review'
  // Document state
  | 'document.lock'
  | 'document.unlock'
  | 'sync.request'
  | 'sync.response';

// Connection Messages
interface ConnectMessage {
  type: 'connect';
  data: {
    documentId: string;
    userId: string;
    token: string; // Auth token
    lastRevision?: number; // For reconnection
    clientInfo: ClientInfo;
  };
}

interface ConnectResponse {
  type: 'connect';
  data: {
    sessionId: string;
    currentRevision: number;
    activeUsers: UserPresence[];
    permissions: SessionPermissions;
  };
}

// Operation Messages
interface OperationMessage {
  type: 'operation';
  data: {
    operation: Operation;
    revision: number;
  };
}

interface OperationAck {
  type: 'operation.ack';
  data: {
    operationId: string;
    revision: number;
    transformed?: boolean;
  };
}

interface OperationReject {
  type: 'operation.reject';
  data: {
    operationId: string;
    reason: string;
    suggestion?: string;
  };
}

// Presence Messages
interface PresenceUpdate {
  type: 'presence.update';
  data: {
    presence: UserPresence;
  };
}

interface CursorUpdate {
  type: 'cursor.update';
  data: {
    cursor: CursorPosition;
  };
}

interface SelectionUpdate {
  type: 'selection.update';
  data: {
    selection: TextSelection | null;
  };
}

// Sync Messages
interface SyncRequest {
  type: 'sync.request';
  data: {
    fromRevision: number;
    toRevision?: number;
  };
}

interface SyncResponse {
  type: 'sync.response';
  data: {
    operations: Operation[];
    currentRevision: number;
    checksum: string; // For verification
  };
}
```

### REST API Endpoints

```yaml
# Session Management
POST /api/v1/collaboration/sessions
Request: CreateSessionRequest
Response: CollaborationSession

GET /api/v1/collaboration/sessions/{sessionId}
Response: CollaborationSession

DELETE /api/v1/collaboration/sessions/{sessionId}
Response: 204 No Content

# Presence
GET /api/v1/collaboration/documents/{documentId}/presence
Response: UserPresence[]

GET /api/v1/collaboration/documents/{documentId}/active-users
Response: ActiveUserSummary[]

# History
GET /api/v1/collaboration/documents/{documentId}/operations
Query:
  - fromRevision: number
  - toRevision: number
  - userId: string
  - limit: number
Response: Operation[]

# Comments
GET /api/v1/collaboration/documents/{documentId}/comments
Query:
  - resolved: boolean
  - userId: string
  - range: string (serialized range)
Response: Comment[]

POST /api/v1/collaboration/documents/{documentId}/comments
Request: CreateCommentRequest
Response: Comment

# Suggestions
GET /api/v1/collaboration/documents/{documentId}/suggestions
Query:
  - status: 'pending' | 'accepted' | 'rejected'
  - userId: string
Response: Suggestion[]

POST /api/v1/collaboration/documents/{documentId}/suggestions
Request: CreateSuggestionRequest
Response: Suggestion

PUT /api/v1/collaboration/suggestions/{suggestionId}/review
Request: ReviewSuggestionRequest
Response: Suggestion
```

### GraphQL Schema

```graphql
type Query {
  # Session queries
  collaborationSession(id: ID!): CollaborationSession
  documentSessions(documentId: ID!): [CollaborationSession!]!
  userSessions(userId: ID!): [CollaborationSession!]!
  
  # Presence queries
  documentPresence(documentId: ID!): [UserPresence!]!
  userPresence(documentId: ID!, userId: ID!): UserPresence
  
  # Operation history
  operationHistory(
    documentId: ID!
    fromRevision: Int
    toRevision: Int
    limit: Int = 100
  ): [Operation!]!
  
  # Comments and suggestions
  documentComments(
    documentId: ID!
    resolved: Boolean
  ): [Comment!]!
  
  documentSuggestions(
    documentId: ID!
    status: SuggestionStatus
  ): [Suggestion!]!
}

type Mutation {
  # Session management
  createCollaborationSession(
    documentId: ID!
    clientInfo: ClientInfoInput!
  ): CollaborationSession!
  
  endCollaborationSession(sessionId: ID!): Boolean!
  
  # Comments
  createComment(
    documentId: ID!
    content: String!
    range: TextSelectionInput!
  ): Comment!
  
  replyToComment(
    commentId: ID!
    content: String!
  ): Comment!
  
  resolveComment(commentId: ID!): Comment!
  
  # Suggestions
  createSuggestion(
    documentId: ID!
    suggestion: CreateSuggestionInput!
  ): Suggestion!
  
  reviewSuggestion(
    suggestionId: ID!
    action: ReviewAction!
    reason: String
  ): Suggestion!
}

type Subscription {
  # Real-time collaboration
  documentCollaboration(documentId: ID!): CollaborationUpdate!
  userPresenceUpdates(documentId: ID!): PresenceUpdate!
  documentOperations(documentId: ID!): Operation!
  commentThreadUpdates(documentId: ID!): CommentUpdate!
}

union CollaborationUpdate = 
  | UserJoined
  | UserLeft
  | OperationApplied
  | ConflictDetected
  | DocumentLocked

enum SuggestionStatus {
  PENDING
  ACCEPTED
  REJECTED
}

enum ReviewAction {
  ACCEPT
  REJECT
}
```

## Implementation Details

### WebSocket Gateway

```typescript
import { WebSocketGateway, WebSocketServer, OnGatewayConnection, OnGatewayDisconnect } from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';

@WebSocketGateway({
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'],
    credentials: true
  },
  transports: ['websocket', 'polling'],
  pingTimeout: 60000,
  pingInterval: 25000
})
export class CollaborationGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  private server: Server;

  constructor(
    private sessionManager: SessionManager,
    private otEngine: OperationalTransformEngine,
    private presenceTracker: PresenceTracker,
    private pubsub: PubSubService
  ) {}

  async handleConnection(socket: Socket): Promise<void> {
    console.log(`Client attempting connection: ${socket.id}`);
    
    // Initial handshake timeout
    const handshakeTimeout = setTimeout(() => {
      socket.disconnect();
    }, 10000);
    
    socket.once('authenticate', async (data: ConnectMessage['data']) => {
      clearTimeout(handshakeTimeout);
      
      try {
        // Verify authentication
        const user = await this.verifyAuth(data.token);
        if (!user) {
          socket.emit('error', { message: 'Authentication failed' });
          socket.disconnect();
          return;
        }
        
        // Check document access
        const hasAccess = await this.checkDocumentAccess(
          user.id,
          data.documentId
        );
        if (!hasAccess) {
          socket.emit('error', { message: 'Access denied' });
          socket.disconnect();
          return;
        }
        
        // Create session
        const session = await this.sessionManager.createSession({
          documentId: data.documentId,
          userId: user.id,
          connectionId: socket.id,
          clientInfo: data.clientInfo
        });
        
        // Join document room
        socket.join(`doc:${data.documentId}`);
        socket.join(`user:${user.id}`);
        
        // Store session info on socket
        socket.data = {
          sessionId: session.id,
          userId: user.id,
          documentId: data.documentId,
          revision: data.lastRevision || 0
        };
        
        // Send connection response
        const response: ConnectResponse = {
          type: 'connect',
          data: {
            sessionId: session.id,
            currentRevision: await this.getDocumentRevision(data.documentId),
            activeUsers: await this.presenceTracker.getActiveUsers(data.documentId),
            permissions: session.permissions
          }
        };
        
        socket.emit('connected', response);
        
        // Notify others
        socket.to(`doc:${data.documentId}`).emit('user.joined', {
          userId: user.id,
          sessionId: session.id,
          displayName: user.displayName,
          timestamp: new Date()
        });
        
        // Subscribe to document updates
        await this.subscribeToDocument(socket, data.documentId);
        
      } catch (error) {
        console.error('Connection error:', error);
        socket.emit('error', { message: 'Connection failed' });
        socket.disconnect();
      }
    });
  }

  async handleDisconnect(socket: Socket): Promise<void> {
    if (!socket.data?.sessionId) return;
    
    const { sessionId, userId, documentId } = socket.data;
    
    // Update session status
    await this.sessionManager.endSession(sessionId);
    
    // Update presence
    await this.presenceTracker.removeUser(documentId, userId);
    
    // Notify others
    socket.to(`doc:${documentId}`).emit('user.left', {
      userId,
      sessionId,
      timestamp: new Date()
    });
    
    // Cleanup subscriptions
    await this.unsubscribeFromDocument(socket, documentId);
  }

  @SubscribeMessage('operation')
  async handleOperation(socket: Socket, data: OperationMessage['data']): Promise<void> {
    const { sessionId, documentId, revision } = socket.data;
    
    try {
      // Validate operation
      if (data.revision !== revision) {
        // Need to transform operation
        const transformed = await this.otEngine.transform(
          data.operation,
          documentId,
          data.revision,
          revision
        );
        
        data.operation = transformed;
      }
      
      // Apply operation
      const result = await this.otEngine.apply(
        data.operation,
        documentId,
        sessionId
      );
      
      // Update socket revision
      socket.data.revision = result.revision;
      
      // Send acknowledgment
      socket.emit('operation.ack', {
        operationId: data.operation.id,
        revision: result.revision,
        transformed: result.transformed
      });
      
      // Broadcast to others
      socket.to(`doc:${documentId}`).emit('operation', {
        operation: result.operation,
        revision: result.revision,
        userId: socket.data.userId
      });
      
      // Persist to storage
      await this.persistOperation(documentId, result.operation);
      
    } catch (error) {
      socket.emit('operation.reject', {
        operationId: data.operation.id,
        reason: error.message,
        suggestion: this.getSuggestion(error)
      });
    }
  }

  @SubscribeMessage('cursor.update')
  async handleCursorUpdate(socket: Socket, data: CursorUpdate['data']): Promise<void> {
    const { userId, documentId } = socket.data;
    
    // Update presence
    await this.presenceTracker.updateCursor(
      documentId,
      userId,
      data.cursor
    );
    
    // Broadcast to others in document
    socket.to(`doc:${documentId}`).emit('cursor.update', {
      userId,
      cursor: data.cursor
    });
  }

  @SubscribeMessage('selection.update')
  async handleSelectionUpdate(socket: Socket, data: SelectionUpdate['data']): Promise<void> {
    const { userId, documentId } = socket.data;
    
    // Update presence
    await this.presenceTracker.updateSelection(
      documentId,
      userId,
      data.selection
    );
    
    // Broadcast to others
    socket.to(`doc:${documentId}`).emit('selection.update', {
      userId,
      selection: data.selection
    });
  }

  @SubscribeMessage('sync.request')
  async handleSyncRequest(socket: Socket, data: SyncRequest['data']): Promise<void> {
    const { documentId } = socket.data;
    
    try {
      // Get operations in range
      const operations = await this.otEngine.getOperations(
        documentId,
        data.fromRevision,
        data.toRevision
      );
      
      // Calculate checksum for verification
      const checksum = await this.calculateChecksum(documentId);
      
      // Send sync response
      socket.emit('sync.response', {
        operations,
        currentRevision: await this.getDocumentRevision(documentId),
        checksum
      });
      
      // Update socket revision
      socket.data.revision = operations.length > 0 
        ? operations[operations.length - 1].revision
        : data.fromRevision;
        
    } catch (error) {
      socket.emit('error', {
        type: 'sync.failed',
        message: error.message
      });
    }
  }

  private async subscribeToDocument(socket: Socket, documentId: string): Promise<void> {
    // Subscribe to Pub/Sub for cross-server communication
    const subscription = await this.pubsub.subscribe(`document:${documentId}`, (message) => {
      // Don't send back to originating server
      if (message.serverId !== this.getServerId()) {
        socket.emit(message.type, message.data);
      }
    });
    
    socket.data.subscription = subscription;
  }
}
```

### Operational Transform Engine

```typescript
@Injectable()
export class OperationalTransformEngine {
  constructor(
    private firestore: Firestore,
    private cache: CacheService
  ) {}

  async transform(
    operation: Operation,
    documentId: string,
    clientRevision: number,
    serverRevision: number
  ): Promise<Operation> {
    if (clientRevision === serverRevision) {
      return operation;
    }
    
    // Get operations between client and server revision
    const concurrentOps = await this.getOperations(
      documentId,
      clientRevision,
      serverRevision
    );
    
    // Transform against each concurrent operation
    let transformed = operation;
    for (const concurrent of concurrentOps) {
      transformed = this.transformPair(transformed, concurrent);
      
      // Log transformation
      if (!transformed.transformations) {
        transformed.transformations = [];
      }
      transformed.transformations.push({
        againstOperation: concurrent.id,
        originalPosition: operation.path.offset,
        transformedPosition: transformed.path.offset,
        reason: `Transformed against ${concurrent.type} operation`
      });
    }
    
    return transformed;
  }

  private transformPair(op1: Operation, op2: Operation): Operation {
    // Same chunk and scene check
    if (op1.path.chunkId !== op2.path.chunkId ||
        op1.path.sceneId !== op2.path.sceneId ||
        op1.path.paragraphId !== op2.path.paragraphId) {
      return op1; // No transformation needed
    }
    
    // Transform based on operation types
    if (op1.type === 'insert' && op2.type === 'insert') {
      return this.transformInsertInsert(op1, op2);
    } else if (op1.type === 'insert' && op2.type === 'delete') {
      return this.transformInsertDelete(op1, op2);
    } else if (op1.type === 'delete' && op2.type === 'insert') {
      return this.transformDeleteInsert(op1, op2);
    } else if (op1.type === 'delete' && op2.type === 'delete') {
      return this.transformDeleteDelete(op1, op2);
    } else if (op1.type === 'format' || op2.type === 'format') {
      return this.transformFormat(op1, op2);
    }
    
    return op1;
  }

  private transformInsertInsert(op1: Operation, op2: Operation): Operation {
    if (op1.path.offset < op2.path.offset || 
        (op1.path.offset === op2.path.offset && op1.timestamp < op2.timestamp)) {
      // op1 happens first, no change
      return op1;
    } else {
      // op2 happens first, shift op1
      return {
        ...op1,
        path: {
          ...op1.path,
          offset: op1.path.offset + op2.data.text!.length
        }
      };
    }
  }

  private transformInsertDelete(op1: Operation, op2: Operation): Operation {
    const deleteStart = op2.path.offset;
    const deleteEnd = op2.path.offset + op2.data.length!;
    
    if (op1.path.offset <= deleteStart) {
      // Insert before delete, no change
      return op1;
    } else if (op1.path.offset >= deleteEnd) {
      // Insert after delete, shift back
      return {
        ...op1,
        path: {
          ...op1.path,
          offset: op1.path.offset - op2.data.length!
        }
      };
    } else {
      // Insert within delete range, move to delete position
      return {
        ...op1,
        path: {
          ...op1.path,
          offset: deleteStart
        }
      };
    }
  }

  private transformDeleteInsert(op1: Operation, op2: Operation): Operation {
    const deleteStart = op1.path.offset;
    const deleteEnd = op1.path.offset + op1.data.length!;
    
    if (op2.path.offset <= deleteStart) {
      // Insert before delete, shift delete
      return {
        ...op1,
        path: {
          ...op1.path,
          offset: op1.path.offset + op2.data.text!.length
        }
      };
    } else if (op2.path.offset >= deleteEnd) {
      // Insert after delete, no change
      return op1;
    } else {
      // Insert within delete, split delete
      return {
        ...op1,
        data: {
          ...op1.data,
          length: op1.data.length! + op2.data.text!.length
        }
      };
    }
  }

  private transformDeleteDelete(op1: Operation, op2: Operation): Operation {
    const start1 = op1.path.offset;
    const end1 = op1.path.offset + op1.data.length!;
    const start2 = op2.path.offset;
    const end2 = op2.path.offset + op2.data.length!;
    
    if (end1 <= start2) {
      // op1 before op2, no change
      return op1;
    } else if (start1 >= end2) {
      // op1 after op2, shift back
      return {
        ...op1,
        path: {
          ...op1.path,
          offset: op1.path.offset - op2.data.length!
        }
      };
    } else if (start1 < start2 && end1 > end2) {
      // op1 contains op2, reduce length
      return {
        ...op1,
        data: {
          ...op1.data,
          length: op1.data.length! - op2.data.length!
        }
      };
    } else if (start2 < start1 && end2 > end1) {
      // op2 contains op1, op1 becomes no-op
      return {
        ...op1,
        data: {
          ...op1.data,
          length: 0
        }
      };
    } else {
      // Partial overlap
      const overlapStart = Math.max(start1, start2);
      const overlapEnd = Math.min(end1, end2);
      const overlapLength = overlapEnd - overlapStart;
      
      if (start1 < start2) {
        // op1 starts first
        return {
          ...op1,
          data: {
            ...op1.data,
            length: op1.data.length! - overlapLength
          }
        };
      } else {
        // op2 starts first
        return {
          ...op1,
          path: {
            ...op1.path,
            offset: start2
          },
          data: {
            ...op1.data,
            length: op1.data.length! - overlapLength
          }
        };
      }
    }
  }

  async apply(
    operation: Operation,
    documentId: string,
    sessionId: string
  ): Promise<OperationResult> {
    // Get document lock
    const lock = await this.acquireDocumentLock(documentId);
    
    try {
      // Get current revision
      const currentRevision = await this.getDocumentRevision(documentId);
      
      // Apply operation to document
      const chunk = await this.loadChunk(
        documentId,
        operation.path.chunkId
      );
      
      const updatedChunk = this.applyOperationToChunk(chunk, operation);
      
      // Save updated chunk
      await this.saveChunk(documentId, updatedChunk);
      
      // Update revision
      const newRevision = currentRevision + 1;
      await this.updateDocumentRevision(documentId, newRevision);
      
      // Store operation history
      await this.storeOperation(documentId, {
        ...operation,
        revision: newRevision,
        sessionId
      });
      
      return {
        operation,
        revision: newRevision,
        transformed: operation.transformations ? true : false
      };
      
    } finally {
      await this.releaseDocumentLock(lock);
    }
  }
}
```

### Presence Tracker

```typescript
@Injectable()
export class PresenceTracker {
  private readonly IDLE_TIMEOUT = 60000; // 1 minute
  private readonly AWAY_TIMEOUT = 300000; // 5 minutes
  
  constructor(
    private redis: Redis,
    private firestore: Firestore
  ) {}

  async updatePresence(
    documentId: string,
    userId: string,
    presence: Partial<UserPresence>
  ): Promise<void> {
    const key = `presence:${documentId}:${userId}`;
    
    const current = await this.redis.hgetall(key);
    const updated: UserPresence = {
      ...current,
      ...presence,
      lastSeen: new Date()
    };
    
    // Update Redis
    await this.redis.hmset(key, updated);
    await this.redis.expire(key, 3600); // 1 hour TTL
    
    // Update presence set
    await this.redis.sadd(`presence:${documentId}`, userId);
    
    // Check for state changes
    if (current.status?.state !== updated.status?.state) {
      await this.broadcastStatusChange(documentId, userId, updated.status);
    }
  }

  async updateCursor(
    documentId: string,
    userId: string,
    cursor: CursorPosition
  ): Promise<void> {
    await this.updatePresence(documentId, userId, {
      cursor,
      status: {
        state: 'active',
        isTyping: false,
        lastTyping: new Date()
      }
    });
    
    // Debounced typing indicator
    this.updateTypingStatus(documentId, userId);
  }

  private updateTypingStatus = debounce(
    async (documentId: string, userId: string) => {
      const key = `presence:${documentId}:${userId}`;
      await this.redis.hset(key, 'status.isTyping', 'true');
      
      // Auto-clear typing after 3 seconds
      setTimeout(async () => {
        await this.redis.hset(key, 'status.isTyping', 'false');
      }, 3000);
    },
    500
  );

  async getActiveUsers(documentId: string): Promise<UserPresence[]> {
    const userIds = await this.redis.smembers(`presence:${documentId}`);
    const presences: UserPresence[] = [];
    
    for (const userId of userIds) {
      const key = `presence:${documentId}:${userId}`;
      const data = await this.redis.hgetall(key);
      
      if (data && this.isActive(data.lastSeen)) {
        presences.push(this.deserializePresence(data));
      } else {
        // Remove inactive user
        await this.redis.srem(`presence:${documentId}`, userId);
        await this.redis.del(key);
      }
    }
    
    return presences;
  }

  private isActive(lastSeen: string | Date): boolean {
    const lastSeenTime = new Date(lastSeen).getTime();
    const now = Date.now();
    return now - lastSeenTime < this.AWAY_TIMEOUT;
  }

  async trackActivity(
    documentId: string,
    userId: string,
    activity: string
  ): Promise<void> {
    // Update last activity
    await this.updatePresence(documentId, userId, {
      status: {
        state: 'active',
        message: activity
      }
    });
    
    // Log activity
    await this.firestore
      .collection('collaboration_activity')
      .add({
        documentId,
        userId,
        activity,
        timestamp: FieldValue.serverTimestamp()
      });
  }

  // Presence monitoring
  @Cron('*/30 * * * * *') // Every 30 seconds
  async checkIdleUsers(): Promise<void> {
    const documents = await this.redis.keys('presence:*');
    
    for (const key of documents) {
      const [, documentId, userId] = key.split(':');
      const lastSeen = await this.redis.hget(key, 'lastSeen');
      
      if (lastSeen) {
        const timeSinceLastSeen = Date.now() - new Date(lastSeen).getTime();
        
        if (timeSinceLastSeen > this.AWAY_TIMEOUT) {
          // User is away, remove presence
          await this.removeUser(documentId, userId);
        } else if (timeSinceLastSeen > this.IDLE_TIMEOUT) {
          // User is idle
          await this.updatePresence(documentId, userId, {
            status: { state: 'idle' }
          });
        }
      }
    }
  }
}
```

### Conflict Resolution

```typescript
@Injectable()
export class ConflictResolutionService {
  constructor(
    private otEngine: OperationalTransformEngine,
    private firestore: Firestore,
    private aiService: AIGenerationService
  ) {}

  async detectConflict(
    operations: Operation[]
  ): Promise<EditConflict | null> {
    // Check for conflicting operations
    for (let i = 0; i < operations.length - 1; i++) {
      for (let j = i + 1; j < operations.length; j++) {
        if (this.areConflicting(operations[i], operations[j])) {
          return {
            id: uuidv4(),
            documentId: operations[i].documentId,
            operations: [
              { operation: operations[i], sessionId: operations[i].sessionId, userId: '' },
              { operation: operations[j], sessionId: operations[j].sessionId, userId: '' }
            ],
            detectedAt: new Date(),
            autoResolved: false
          };
        }
      }
    }
    
    return null;
  }

  private areConflicting(op1: Operation, op2: Operation): boolean {
    // Same location check
    if (op1.path.chunkId !== op2.path.chunkId ||
        op1.path.sceneId !== op2.path.sceneId ||
        op1.path.paragraphId !== op2.path.paragraphId) {
      return false;
    }
    
    // Time window check (operations within 100ms)
    const timeDiff = Math.abs(op1.timestamp.getTime() - op2.timestamp.getTime());
    if (timeDiff > 100) {
      return false;
    }
    
    // Check for semantic conflicts
    if (op1.type === 'delete' && op2.type === 'format') {
      // Formatting deleted text
      return true;
    }
    
    if (op1.type === 'insert' && op2.type === 'insert') {
      // Both inserting at exact same position
      return op1.path.offset === op2.path.offset;
    }
    
    return false;
  }

  async resolveConflict(
    conflict: EditConflict,
    strategy: 'transform' | 'merge' | 'choose' | 'manual' = 'transform'
  ): Promise<ConflictResolution> {
    switch (strategy) {
      case 'transform':
        return this.resolveByTransformation(conflict);
      
      case 'merge':
        return this.resolveByMerging(conflict);
      
      case 'choose':
        return this.resolveByChoosing(conflict);
      
      case 'manual':
        return this.resolveManually(conflict);
      
      default:
        throw new Error(`Unknown resolution strategy: ${strategy}`);
    }
  }

  private async resolveByTransformation(
    conflict: EditConflict
  ): Promise<ConflictResolution> {
    const [op1, op2] = conflict.operations.map(o => o.operation);
    
    // Transform operations against each other
    const transformed1 = await this.otEngine.transform(
      op1,
      op1.documentId,
      op1.revision,
      op2.revision
    );
    
    const transformed2 = await this.otEngine.transform(
      op2,
      op2.documentId,
      op2.revision,
      op1.revision
    );
    
    // Apply both transformed operations
    await this.otEngine.apply(transformed1, op1.documentId, op1.sessionId);
    await this.otEngine.apply(transformed2, op2.documentId, op2.sessionId);
    
    return {
      strategy: 'transform',
      resolvedBy: 'system',
      resolvedAt: new Date()
    };
  }

  private async resolveByMerging(
    conflict: EditConflict
  ): Promise<ConflictResolution> {
    const operations = conflict.operations.map(o => o.operation);
    
    // Use AI to merge conflicting edits
    const mergedContent = await this.aiService.mergeConflictingEdits(
      operations.map(op => ({
        type: op.type,
        content: op.data.text || '',
        position: op.path.offset
      }))
    );
    
    // Create merged operation
    const mergedOp: Operation = {
      id: uuidv4(),
      sessionId: 'system',
      documentId: operations[0].documentId,
      revision: Math.max(...operations.map(o => o.revision)) + 1,
      timestamp: new Date(),
      type: 'insert',
      path: operations[0].path,
      data: { text: mergedContent }
    };
    
    return {
      strategy: 'merge',
      mergedResult: mergedOp,
      resolvedBy: 'system',
      resolvedAt: new Date()
    };
  }
}
```

### Comment System

```typescript
@Injectable()
export class CommentService {
  constructor(
    private firestore: Firestore,
    private notificationService: NotificationService
  ) {}

  async createComment(
    documentId: string,
    userId: string,
    content: string,
    range: TextSelection
  ): Promise<Comment> {
    const comment: Comment = {
      id: uuidv4(),
      documentId,
      userId,
      content,
      range,
      createdAt: new Date(),
      updatedAt: new Date(),
      resolved: false,
      replies: []
    };
    
    // Save to Firestore
    await this.firestore
      .collection('documents')
      .doc(documentId)
      .collection('comments')
      .doc(comment.id)
      .set(comment);
    
    // Notify mentioned users
    const mentions = this.extractMentions(content);
    for (const mentionedUser of mentions) {
      await this.notificationService.notify(mentionedUser, {
        type: 'comment.mention',
        documentId,
        commentId: comment.id,
        fromUser: userId,
        preview: content.substring(0, 100)
      });
    }
    
    // Broadcast to active users
    await this.broadcastCommentUpdate(documentId, {
      type: 'comment.created',
      comment
    });
    
    return comment;
  }

  async replyToComment(
    commentId: string,
    userId: string,
    content: string
  ): Promise<Comment> {
    const commentRef = await this.firestore
      .collectionGroup('comments')
      .where('id', '==', commentId)
      .limit(1)
      .get();
    
    if (commentRef.empty) {
      throw new Error('Comment not found');
    }
    
    const commentDoc = commentRef.docs[0];
    const comment = commentDoc.data() as Comment;
    
    const reply: CommentReply = {
      id: uuidv4(),
      userId,
      content,
      createdAt: new Date()
    };
    
    comment.replies.push(reply);
    comment.updatedAt = new Date();
    
    await commentDoc.ref.update({
      replies: comment.replies,
      updatedAt: comment.updatedAt
    });
    
    // Notify original commenter
    if (comment.userId !== userId) {
      await this.notificationService.notify(comment.userId, {
        type: 'comment.reply',
        documentId: comment.documentId,
        commentId: comment.id,
        fromUser: userId,
        preview: content.substring(0, 100)
      });
    }
    
    return comment;
  }

  private extractMentions(content: string): string[] {
    const mentionRegex = /@(\w+)/g;
    const mentions: string[] = [];
    let match;
    
    while ((match = mentionRegex.exec(content)) !== null) {
      mentions.push(match[1]);
    }
    
    return mentions;
  }
}
```

## Performance Optimization

### Connection Pooling

```typescript
@Injectable()
export class ConnectionPoolService {
  private pools: Map<string, ConnectionPool> = new Map();
  
  async getConnection(documentId: string): Promise<PooledConnection> {
    let pool = this.pools.get(documentId);
    
    if (!pool) {
      pool = this.createPool(documentId);
      this.pools.set(documentId, pool);
    }
    
    return pool.acquire();
  }
  
  private createPool(documentId: string): ConnectionPool {
    return new ConnectionPool({
      min: 2,
      max: 100,
      idleTimeout: 300000, // 5 minutes
      acquireTimeout: 10000,
      createConnection: async () => {
        return new DocumentConnection(documentId);
      },
      validateConnection: async (conn) => {
        return conn.isAlive();
      }
    });
  }
  
  // Cleanup idle pools
  @Cron('*/5 * * * *') // Every 5 minutes
  async cleanupPools(): Promise<void> {
    for (const [documentId, pool] of this.pools.entries()) {
      if (pool.idle === pool.size && pool.size > pool.min) {
        // Shrink pool
        await pool.drain();
        this.pools.delete(documentId);
      }
    }
  }
}
```

### Message Batching

```typescript
@Injectable()
export class MessageBatchingService {
  private batches: Map<string, MessageBatch> = new Map();
  private readonly BATCH_SIZE = 50;
  private readonly BATCH_TIMEOUT = 50; // ms
  
  async queueMessage(
    documentId: string,
    message: any
  ): Promise<void> {
    let batch = this.batches.get(documentId);
    
    if (!batch) {
      batch = {
        messages: [],
        timer: null
      };
      this.batches.set(documentId, batch);
    }
    
    batch.messages.push(message);
    
    if (batch.messages.length >= this.BATCH_SIZE) {
      // Send immediately if batch is full
      this.sendBatch(documentId);
    } else if (!batch.timer) {
      // Set timer for batch timeout
      batch.timer = setTimeout(() => {
        this.sendBatch(documentId);
      }, this.BATCH_TIMEOUT);
    }
  }
  
  private sendBatch(documentId: string): void {
    const batch = this.batches.get(documentId);
    if (!batch || batch.messages.length === 0) return;
    
    // Clear timer
    if (batch.timer) {
      clearTimeout(batch.timer);
    }
    
    // Send batched messages
    this.broadcastBatch(documentId, batch.messages);
    
    // Clear batch
    this.batches.delete(documentId);
  }
}
```

## Security Considerations

### Authentication and Authorization

```typescript
@Injectable()
export class CollaborationAuthService {
  constructor(
    private jwtService: JwtService,
    private documentService: DocumentService
  ) {}
  
  async verifyWebSocketToken(token: string): Promise<AuthUser | null> {
    try {
      const payload = await this.jwtService.verify(token);
      
      // Additional validation
      if (payload.type !== 'websocket' || !payload.userId) {
        return null;
      }
      
      // Check if token is not revoked
      const isRevoked = await this.isTokenRevoked(payload.jti);
      if (isRevoked) {
        return null;
      }
      
      return {
        id: payload.userId,
        email: payload.email,
        displayName: payload.displayName
      };
      
    } catch (error) {
      return null;
    }
  }
  
  async checkCollaborationPermission(
    userId: string,
    documentId: string,
    requiredPermission: 'view' | 'edit' | 'comment'
  ): Promise<boolean> {
    const collaborator = await this.documentService
      .getCollaborator(documentId, userId);
    
    if (!collaborator) {
      return false;
    }
    
    const permissionHierarchy = {
      view: ['viewer', 'commenter', 'editor', 'owner'],
      comment: ['commenter', 'editor', 'owner'],
      edit: ['editor', 'owner']
    };
    
    return permissionHierarchy[requiredPermission]
      .includes(collaborator.role);
  }
}
```

### Rate Limiting

```typescript
@Injectable()
export class CollaborationRateLimiter {
  private limits = {
    operations: { window: 1000, max: 100 }, // 100 ops per second
    cursor: { window: 100, max: 50 }, // 50 cursor updates per 100ms
    comments: { window: 60000, max: 10 } // 10 comments per minute
  };
  
  async checkLimit(
    userId: string,
    action: string
  ): Promise<boolean> {
    const limit = this.limits[action];
    if (!limit) return true;
    
    const key = `ratelimit:${action}:${userId}`;
    const current = await this.redis.incr(key);
    
    if (current === 1) {
      await this.redis.pexpire(key, limit.window);
    }
    
    return current <= limit.max;
  }
}
```

## Monitoring and Metrics

```typescript
@Injectable()
export class CollaborationMetrics {
  private metrics: MetricsClient;
  
  async recordCollaborationMetric(
    metric: string,
    value: number,
    labels: Record<string, string>
  ): Promise<void> {
    await this.metrics.createTimeSeries({
      metric: {
        type: `custom.googleapis.com/collaboration/${metric}`,
        labels
      },
      points: [{
        interval: { endTime: { seconds: Date.now() / 1000 } },
        value: { doubleValue: value }
      }]
    });
  }
  
  // Track key metrics
  async trackMetrics(): Promise<void> {
    // Active connections
    const activeConnections = await this.getActiveConnectionCount();
    await this.recordCollaborationMetric('active_connections', activeConnections, {});
    
    // Operations per second
    const opsPerSecond = await this.getOperationsPerSecond();
    await this.recordCollaborationMetric('operations_per_second', opsPerSecond, {});
    
    // Conflict rate
    const conflictRate = await this.getConflictRate();
    await this.recordCollaborationMetric('conflict_rate', conflictRate, {});
  }
}
```

## Testing Strategy

### Unit Tests

```typescript
describe('OperationalTransformEngine', () => {
  let engine: OperationalTransformEngine;
  
  beforeEach(() => {
    engine = new OperationalTransformEngine();
  });
  
  describe('transform operations', () => {
    it('should handle insert-insert conflicts', () => {
      const op1: Operation = {
        type: 'insert',
        path: { offset: 5 },
        data: { text: 'hello' }
      };
      
      const op2: Operation = {
        type: 'insert',
        path: { offset: 5 },
        data: { text: 'world' }
      };
      
      const result = engine.transformPair(op1, op2);
      
      // op2 happens first due to timestamp
      expect(result.path.offset).toBe(10);
    });
    
    it('should handle delete-delete overlaps', () => {
      const op1: Operation = {
        type: 'delete',
        path: { offset: 5 },
        data: { length: 10 }
      };
      
      const op2: Operation = {
        type: 'delete',
        path: { offset: 8 },
        data: { length: 10 }
      };
      
      const result = engine.transformPair(op1, op2);
      
      expect(result.data.length).toBe(7); // Reduced due to overlap
    });
  });
});
```

## Performance Requirements

| Metric | Target | Measurement |
|--------|--------|-------------|
| Connection time | < 1s | P95 |
| Operation latency | < 50ms | P95 |
| Cursor update latency | < 30ms | P95 |
| Conflict resolution | < 100ms | P95 |
| Message delivery | < 100ms | P95 |
| Concurrent users | 100+ | Per document |
| Operations/second | 1000+ | Per document |
| WebSocket connections | 50,000 | Per server |

## Deployment Configuration

```yaml
# collaboration-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: collaboration-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: collaboration-service
  template:
    spec:
      containers:
      - name: collaboration-service
        image: gcr.io/deus-ex-machina/collaboration-service:latest
        ports:
        - containerPort: 3000  # HTTP
        - containerPort: 3001  # WebSocket
        env:
        - name: NODE_ENV
          value: "production"
        - name: REDIS_URL
          value: "redis://redis-cluster:6379"
        - name: FIRESTORE_PROJECT
          value: "deus-ex-machina"
        resources:
          requests:
            cpu: "1"
            memory: "2Gi"
          limits:
            cpu: "2"
            memory: "4Gi"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 10
```

## Conclusion

The Real-time Collaboration Service provides a robust foundation for multi-user novel editing with conflict-free synchronization. By leveraging WebSockets, Operational Transformation, and distributed messaging through Pub/Sub, the service enables seamless collaboration while maintaining document integrity. The architecture supports 100+ concurrent users per document with sub-100ms latency for all operations.