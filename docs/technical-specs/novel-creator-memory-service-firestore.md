# Technical Specification: Memory Service (SCORE Pattern with Firestore)

## Overview

The Memory Service implements a SCORE-inspired (State, Context, Observation, Reflection, Execution) hierarchical memory architecture designed to maintain narrative coherence across 120k+ word novels. This service leverages Firestore's document model for flexible schema evolution, real-time updates, and seamless scaling while providing intelligent retrieval and summarization capabilities for the AI generation process.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Memory Service                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────────┐   │
│  │  State Layer   │  │ Context Layer  │  │Observation Layer  │   │
│  │  (Real-time)   │  │ (Session-based)│  │  (Event-driven)   │   │
│  └────────┬───────┘  └────────┬───────┘  └─────────┬─────────┘   │
│           │                    │                      │             │
│  ┌────────▼───────────────────▼──────────────────────▼─────────┐  │
│  │                    Memory Core Engine                        │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │Hierarchical │  │  Retrieval   │  │   Summarization   │ │  │
│  │  │  Storage    │  │   Engine     │  │     Engine        │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────┬──────────────────────────────────┘  │
│                             │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐  │
│  │                     Storage Backend                          │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │  Firestore  │  │  Memorystore │  │   BigQuery        │ │  │
│  │  │  (Primary)  │  │  (Cache)     │  │  (Analytics)      │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Firestore Collection Architecture

```
projects/{projectId}/
├── characters/{characterId}/
│   ├── metadata (document)
│   │   ├── name: string
│   │   ├── aliases: string[]
│   │   ├── createdAt: timestamp
│   │   └── lastActive: timestamp
│   ├── states/{sceneId}/
│   │   ├── physical: map
│   │   ├── emotional: map
│   │   ├── inventory: array
│   │   └── timestamp: timestamp
│   └── observations/{observationId}/
│       ├── sceneId: string
│       ├── type: string
│       ├── content: string
│       └── emotionalImpact: number
│
├── plot/{threadId}/
│   ├── metadata (document)
│   │   ├── name: string
│   │   ├── type: string
│   │   ├── status: string
│   │   └── tensionLevel: number
│   └── milestones/{milestoneId}/
│       ├── name: string
│       ├── achieved: boolean
│       └── impact: string
│
├── world/
│   ├── facts/{factId}/
│   │   ├── category: string
│   │   ├── fact: string
│   │   ├── importance: string
│   │   └── establishedIn: string
│   └── contradictions/{contradictionId}/
│       ├── factIds: array
│       ├── severity: string
│       └── resolved: boolean
│
├── summaries/{type}_{id}/
│   ├── level: string
│   ├── content: map
│   ├── metadata: map
│   └── embeddings: map
│
└── relationships/{relationshipId}/
    ├── source: reference
    ├── target: reference
    ├── type: string
    └── strength: number
```

## Data Models

### Firestore Document Schemas

```typescript
// Character Document
interface CharacterDocument {
  id: string;
  projectId: string;
  metadata: {
    name: string;
    aliases: string[];
    createdAt: Timestamp;
    lastActive: Timestamp;
    tags: string[]; // For efficient querying
  };
  currentState: CharacterState; // Denormalized for quick access
  stats: {
    sceneCount: number;
    wordCount: number;
    lastSceneId: string;
  };
}

interface CharacterState {
  physical: {
    location: string;
    condition: 'healthy' | 'injured' | 'sick' | 'other';
    appearance: Record<string, string>;
  };
  emotional: {
    mood: string;
    stress: number; // 0-10
    relationships: Record<string, number>; // characterId -> sentiment
  };
  inventory: string[];
  goals: Goal[];
  secrets: Secret[];
  timestamp: Timestamp;
  sceneId: string;
}

interface CharacterObservation {
  id: string;
  characterId: string;
  sceneId: string;
  timestamp: Timestamp;
  type: 'action' | 'dialogue' | 'thought' | 'description';
  content: string;
  consequences: string[];
  emotionalImpact: number; // -10 to 10
  tags: string[]; // For search optimization
}

// Plot Document
interface PlotDocument {
  id: string;
  projectId: string;
  metadata: {
    name: string;
    type: 'main' | 'subplot' | 'character-arc';
    status: 'setup' | 'rising' | 'climax' | 'falling' | 'resolved';
    createdAt: Timestamp;
    updatedAt: Timestamp;
  };
  description: string;
  involvedCharacters: string[]; // Character IDs
  keyEvents: string[]; // Event IDs
  tensionLevel: number; // 0-10
  dependencies: string[]; // Other thread IDs
  tags: string[];
}

interface PlotMilestone {
  id: string;
  threadId: string;
  name: string;
  description: string;
  achieved: boolean;
  achievedAt?: {
    sceneId: string;
    timestamp: Timestamp;
  };
  impact: 'minor' | 'moderate' | 'major';
  foreshadowing: string[];
  callbacks: string[];
}

// World Document
interface WorldFact {
  id: string;
  projectId: string;
  category: 'geography' | 'history' | 'technology' | 'magic' | 'society' | 'other';
  fact: string;
  establishedIn: string; // sceneId
  importance: 'trivial' | 'minor' | 'significant' | 'critical';
  relatedFacts: string[];
  tags: string[];
  embedding?: number[]; // For semantic search
  createdAt: Timestamp;
}

interface Contradiction {
  id: string;
  projectId: string;
  factIds: string[];
  description: string;
  severity: 'minor' | 'major' | 'critical';
  resolved: boolean;
  resolution?: {
    description: string;
    resolvedAt: Timestamp;
    resolvedBy: string; // userId
  };
  detectedAt: Timestamp;
}

// Summary Document
interface SummaryDocument {
  id: string; // Format: {level}_{parentId}_{hash}
  projectId: string;
  level: 'scene' | 'chapter' | 'act' | 'book';
  parentId?: string;
  content: {
    events: string;
    characterDevelopment: Record<string, string>;
    plotProgression: Record<string, string>;
    themeEvolution: Record<string, string>;
    worldBuilding: string[];
  };
  metadata: {
    wordCount: number;
    sceneRange: {
      start: string;
      end: string;
    };
    emotionalArc: number[];
    pacing: 'slow' | 'moderate' | 'fast';
  };
  embeddings: {
    plot: number[];
    character: number[];
    theme: number[];
  };
  generatedAt: Timestamp;
  version: number;
}

// Relationship Document
interface RelationshipDocument {
  id: string;
  projectId: string;
  source: {
    type: 'character' | 'location' | 'object' | 'concept';
    id: string;
    name: string;
  };
  target: {
    type: 'character' | 'location' | 'object' | 'concept';
    id: string;
    name: string;
  };
  relationshipType: string; // 'knows', 'loves', 'owns', etc.
  strength: number; // 0-10
  bidirectional: boolean;
  temporal: {
    establishedIn: string; // sceneId
    modifiedIn: string[]; // sceneIds
    endedIn?: string; // sceneId
  };
  tags: string[];
  lastUpdated: Timestamp;
}
```

## API Design

### REST API Endpoints

```yaml
# Character Memory Management
GET /api/v1/memory/characters/{characterId}
Response: CharacterMemory

PUT /api/v1/memory/characters/{characterId}/state
Request: CharacterStateUpdate
Response: CharacterMemory

POST /api/v1/memory/characters/{characterId}/observations
Request: CharacterObservation
Response: ObservationResult

GET /api/v1/memory/characters/{characterId}/timeline
Query:
  - startScene: string
  - endScene: string
  - limit: number
Response: CharacterTimeline

# Plot Memory Management  
GET /api/v1/memory/plot/{projectId}
Response: PlotMemory

PUT /api/v1/memory/plot/{projectId}/threads/{threadId}
Request: PlotThreadUpdate
Response: PlotThread

POST /api/v1/memory/plot/{projectId}/milestones
Request: PlotMilestone
Response: MilestoneResult

# World Memory Management
GET /api/v1/memory/world/{projectId}
Response: WorldMemory

POST /api/v1/memory/world/{projectId}/facts
Request: WorldFact
Response: FactValidationResult

POST /api/v1/memory/world/{projectId}/validate
Request: ValidationRequest
Response: ValidationReport

# Search and Retrieval
POST /api/v1/memory/search
Request: MemorySearchQuery
Response: MemorySearchResults

GET /api/v1/memory/context/{projectId}/{sceneId}
Query: 
  - depth: number
  - include: string[]
Response: GenerationContext

# Real-time Subscriptions
WS /api/v1/memory/subscribe/{projectId}
Message Types:
  - character.state.changed
  - plot.milestone.achieved
  - world.contradiction.detected
```

### GraphQL Schema

```graphql
type Query {
  # Character queries
  character(id: ID!): CharacterMemory
  charactersByProject(projectId: ID!): [CharacterMemory!]!
  characterState(characterId: ID!, atScene: ID): CharacterState
  characterTimeline(
    characterId: ID!
    startScene: ID
    endScene: ID
    limit: Int = 20
  ): [CharacterState!]!
  
  # Plot queries
  plotMemory(projectId: ID!): PlotMemory
  plotThreads(projectId: ID!, status: ThreadStatus): [PlotThread!]!
  plotMilestones(threadId: ID!, achieved: Boolean): [PlotMilestone!]!
  
  # World queries
  worldMemory(projectId: ID!): WorldMemory
  worldFacts(
    projectId: ID!
    category: FactCategory
    importance: Importance
  ): [WorldFact!]!
  contradictions(
    projectId: ID!
    resolved: Boolean
  ): [Contradiction!]!
  
  # Context and search
  generationContext(
    projectId: ID!
    sceneId: ID!
    options: ContextOptions
  ): GenerationContext!
  
  memorySearch(query: MemorySearchInput!): MemorySearchResult!
}

type Mutation {
  # Character mutations
  updateCharacterState(
    characterId: ID!
    sceneId: ID!
    update: CharacterStateInput!
  ): CharacterMemory!
  
  recordObservation(
    characterId: ID!
    observation: ObservationInput!
  ): CharacterMemory!
  
  # Plot mutations
  updatePlotThread(
    threadId: ID!
    update: PlotThreadInput!
  ): PlotThread!
  
  achieveMilestone(
    milestoneId: ID!
    sceneId: ID!
  ): PlotMilestone!
  
  # World mutations
  addWorldFact(
    projectId: ID!
    fact: WorldFactInput!
  ): FactValidationResult!
  
  resolveContradiction(
    contradictionId: ID!
    resolution: String!
  ): Contradiction!
}

type Subscription {
  # Real-time memory updates
  characterUpdates(characterId: ID!): CharacterUpdate!
  plotProgressions(projectId: ID!): PlotProgression!
  contradictionDetected(projectId: ID!): Contradiction!
  memorySync(projectId: ID!): MemorySync!
}
```

## Implementation Details

### Firestore Integration

```typescript
import { 
  Firestore, 
  FieldValue, 
  Transaction,
  CollectionReference,
  Query,
  DocumentSnapshot,
  QuerySnapshot
} from '@google-cloud/firestore';

@Injectable()
export class FirestoreMemoryService {
  private db: Firestore;
  
  constructor() {
    this.db = new Firestore({
      projectId: process.env.GCP_PROJECT_ID,
      // Firestore automatically uses Application Default Credentials
    });
  }
  
  // Character State Management with Real-time Updates
  async updateCharacterState(
    characterId: string,
    sceneId: string,
    update: CharacterStateUpdate
  ): Promise<CharacterMemory> {
    const characterRef = this.db
      .collection('projects').doc(update.projectId)
      .collection('characters').doc(characterId);
    
    const stateRef = characterRef
      .collection('states').doc(sceneId);
    
    try {
      // Use transaction for consistency
      const result = await this.db.runTransaction(async (transaction) => {
        const charDoc = await transaction.get(characterRef);
        
        if (!charDoc.exists) {
          throw new Error('Character not found');
        }
        
        // Create new state document
        const newState: CharacterState = {
          ...charDoc.data()!.currentState,
          ...update.changes,
          timestamp: FieldValue.serverTimestamp(),
          sceneId
        };
        
        // Validate state
        this.validateCharacterState(newState);
        
        // Update current state on character document
        transaction.update(characterRef, {
          currentState: newState,
          'metadata.lastActive': FieldValue.serverTimestamp(),
          'stats.lastSceneId': sceneId
        });
        
        // Create historical state record
        transaction.set(stateRef, newState);
        
        // Create observation if provided
        if (update.observation) {
          const obsRef = characterRef
            .collection('observations')
            .doc();
          
          transaction.set(obsRef, {
            ...update.observation,
            characterId,
            sceneId,
            timestamp: FieldValue.serverTimestamp()
          });
        }
        
        return { characterId, newState };
      });
      
      // Trigger real-time updates for subscribers
      await this.publishUpdate('character.state.changed', {
        characterId,
        sceneId,
        state: result.newState
      });
      
      return this.getCharacterMemory(characterId);
      
    } catch (error) {
      console.error('Failed to update character state:', error);
      throw error;
    }
  }
  
  // Efficient Context Building with Firestore Queries
  async buildGenerationContext(
    projectId: string,
    sceneId: string,
    options: ContextOptions
  ): Promise<GenerationContext> {
    const projectRef = this.db.collection('projects').doc(projectId);
    
    // Parallel fetch all required data
    const [
      characters,
      activeThreads,
      recentFacts,
      summaries,
      relationships
    ] = await Promise.all([
      this.getActiveCharacters(projectRef, sceneId),
      this.getActivePlotThreads(projectRef),
      this.getRecentWorldFacts(projectRef, options.factLimit || 50),
      this.getRelevantSummaries(projectRef, sceneId, options.depth || 2),
      this.getActiveRelationships(projectRef, sceneId)
    ]);
    
    // Build hierarchical context
    const context: GenerationContext = {
      projectId,
      sceneId,
      characters: await this.enrichCharacterContext(characters, sceneId),
      plot: {
        activeThreads,
        recentMilestones: await this.getRecentMilestones(activeThreads)
      },
      world: {
        establishedFacts: recentFacts,
        activeLocations: this.extractActiveLocations(characters)
      },
      summaries: this.organizeSummariesByLevel(summaries),
      relationships: this.buildRelationshipGraph(relationships),
      metadata: {
        timestamp: new Date(),
        tokenBudget: options.maxTokens || 100000
      }
    };
    
    // Cache context for performance
    await this.cacheContext(projectId, sceneId, context);
    
    return context;
  }
  
  // Real-time Contradiction Detection
  async addWorldFact(
    projectId: string,
    fact: WorldFactInput
  ): Promise<FactValidationResult> {
    const factsRef = this.db
      .collection('projects').doc(projectId)
      .collection('world').doc('facts')
      .collection('facts');
    
    // Check for contradictions using Firestore queries
    const potentialContradictions = await this.findPotentialContradictions(
      factsRef,
      fact
    );
    
    if (potentialContradictions.length > 0) {
      // Create contradiction records
      const contradictions = await Promise.all(
        potentialContradictions.map(existing => 
          this.createContradiction(projectId, fact, existing)
        )
      );
      
      return {
        fact: { ...fact, id: 'pending' },
        valid: false,
        contradictions,
        suggestions: this.generateResolutionSuggestions(contradictions)
      };
    }
    
    // Add the fact
    const factDoc = await factsRef.add({
      ...fact,
      createdAt: FieldValue.serverTimestamp(),
      tags: this.generateFactTags(fact)
    });
    
    return {
      fact: { ...fact, id: factDoc.id },
      valid: true,
      contradictions: [],
      suggestions: []
    };
  }
  
  // Composite Queries with Indexes
  async searchMemory(
    projectId: string,
    query: MemorySearchQuery
  ): Promise<MemorySearchResult> {
    const results: MemorySearchResult = {
      characters: [],
      plot: [],
      world: [],
      relationships: []
    };
    
    // Build Firestore queries based on search parameters
    const projectRef = this.db.collection('projects').doc(projectId);
    
    // Character search
    if (query.includeCharacters) {
      const charQuery = projectRef.collection('characters')
        .where('metadata.tags', 'array-contains-any', query.tags || [])
        .orderBy('metadata.lastActive', 'desc')
        .limit(query.limit || 20);
      
      const charResults = await charQuery.get();
      results.characters = charResults.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
    }
    
    // Plot search with compound queries
    if (query.includePlot) {
      const plotQuery = projectRef.collection('plot')
        .where('status', 'in', query.plotStatuses || ['setup', 'rising', 'climax'])
        .where('tensionLevel', '>=', query.minTension || 0)
        .orderBy('tensionLevel', 'desc')
        .limit(query.limit || 20);
      
      const plotResults = await plotQuery.get();
      results.plot = plotResults.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
    }
    
    // Use collection group queries for subcollections
    if (query.searchObservations) {
      const obsQuery = this.db.collectionGroup('observations')
        .where('projectId', '==', projectId)
        .where('tags', 'array-contains-any', query.tags || [])
        .orderBy('timestamp', 'desc')
        .limit(query.limit || 50);
      
      const obsResults = await obsQuery.get();
      results.observations = obsResults.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
    }
    
    return results;
  }
  
  // Batch Operations for Performance
  async batchUpdateCharacterStates(
    updates: CharacterStateUpdate[]
  ): Promise<void> {
    const batch = this.db.batch();
    
    for (const update of updates) {
      const stateRef = this.db
        .collection('projects').doc(update.projectId)
        .collection('characters').doc(update.characterId)
        .collection('states').doc(update.sceneId);
      
      batch.set(stateRef, {
        ...update.state,
        timestamp: FieldValue.serverTimestamp()
      });
    }
    
    await batch.commit();
  }
  
  // Real-time Listeners for Collaboration
  subscribeToMemoryUpdates(
    projectId: string,
    callback: (update: MemoryUpdate) => void
  ): () => void {
    const unsubscribers: (() => void)[] = [];
    
    // Listen to character updates
    const charListener = this.db
      .collection('projects').doc(projectId)
      .collection('characters')
      .onSnapshot((snapshot) => {
        snapshot.docChanges().forEach(change => {
          if (change.type === 'modified') {
            callback({
              type: 'character',
              action: 'update',
              data: {
                id: change.doc.id,
                ...change.doc.data()
              }
            });
          }
        });
      });
    
    unsubscribers.push(charListener);
    
    // Listen to plot progressions
    const plotListener = this.db
      .collection('projects').doc(projectId)
      .collection('plot')
      .onSnapshot((snapshot) => {
        snapshot.docChanges().forEach(change => {
          callback({
            type: 'plot',
            action: change.type,
            data: {
              id: change.doc.id,
              ...change.doc.data()
            }
          });
        });
      });
    
    unsubscribers.push(plotListener);
    
    // Return unsubscribe function
    return () => {
      unsubscribers.forEach(unsub => unsub());
    };
  }
}
```

### Hierarchical Summary Management

```typescript
@Injectable()
export class SummaryEngine {
  constructor(
    private firestore: FirestoreMemoryService,
    private aiService: AIGenerationService
  ) {}
  
  async generateHierarchicalSummary(
    projectId: string,
    level: 'scene' | 'chapter' | 'act',
    contentIds: string[]
  ): Promise<SummaryDocument> {
    // Check cache first
    const cached = await this.getCachedSummary(projectId, level, contentIds);
    if (cached && !this.isStale(cached)) {
      return cached;
    }
    
    // Load content based on level
    const content = await this.loadContentForSummary(
      projectId,
      level,
      contentIds
    );
    
    // Generate summary using AI
    const summaryContent = await this.aiService.generateSummary(
      content,
      level
    );
    
    // Extract key information
    const extraction = this.extractKeyInformation(summaryContent);
    
    // Generate embeddings for semantic search
    const embeddings = await this.generateEmbeddings(extraction);
    
    // Create summary document
    const summary: SummaryDocument = {
      id: this.generateSummaryId(level, contentIds),
      projectId,
      level,
      content: extraction,
      metadata: {
        wordCount: content.totalWords,
        sceneRange: {
          start: contentIds[0],
          end: contentIds[contentIds.length - 1]
        },
        emotionalArc: this.calculateEmotionalArc(content),
        pacing: this.analyzePacing(content)
      },
      embeddings,
      generatedAt: new Date(),
      version: 1
    };
    
    // Store in Firestore
    await this.firestore.saveSummary(projectId, summary);
    
    return summary;
  }
  
  async getRelevantSummaries(
    projectId: string,
    sceneId: string,
    depth: number
  ): Promise<SummaryDocument[]> {
    const summaries: SummaryDocument[] = [];
    
    // Get scene summary
    const sceneSummary = await this.firestore
      .getSummaryBySceneId(projectId, 'scene', sceneId);
    if (sceneSummary) summaries.push(sceneSummary);
    
    if (depth >= 2) {
      // Get chapter summary
      const chapterSummary = await this.firestore
        .getSummaryContainingScene(projectId, 'chapter', sceneId);
      if (chapterSummary) summaries.push(chapterSummary);
    }
    
    if (depth >= 3) {
      // Get act summary
      const actSummary = await this.firestore
        .getSummaryContainingScene(projectId, 'act', sceneId);
      if (actSummary) summaries.push(actSummary);
    }
    
    return summaries;
  }
}
```

### SCORE Pattern Implementation

```typescript
@Injectable()
export class SCOREMemoryEngine {
  constructor(
    private firestore: FirestoreMemoryService,
    private stateManager: StateManager,
    private contextBuilder: ContextBuilder,
    private observationProcessor: ObservationProcessor,
    private reflectionEngine: ReflectionEngine
  ) {}
  
  // STATE: Track current narrative state
  async updateState(
    projectId: string,
    sceneId: string,
    updates: StateUpdate[]
  ): Promise<void> {
    await Promise.all(updates.map(update => 
      this.stateManager.applyUpdate(projectId, sceneId, update)
    ));
  }
  
  // CONTEXT: Build relevant context for generation
  async prepareContext(
    projectId: string,
    sceneId: string,
    request: ContextRequest
  ): Promise<GenerationContext> {
    return this.contextBuilder.build(projectId, sceneId, request);
  }
  
  // OBSERVATION: Process new narrative events
  async processObservations(
    projectId: string,
    sceneId: string,
    content: string
  ): Promise<ObservationResult> {
    const observations = await this.observationProcessor.extract(content);
    
    // Update states based on observations
    for (const obs of observations.characterObservations) {
      await this.firestore.addObservation(projectId, obs);
    }
    
    // Check for contradictions
    const contradictions = await this.detectContradictions(
      projectId,
      observations
    );
    
    return {
      observations,
      contradictions,
      stateChanges: observations.stateChanges
    };
  }
  
  // REFLECTION: Analyze patterns and generate insights
  async performReflection(
    projectId: string
  ): Promise<ReflectionInsights> {
    const insights = await this.reflectionEngine.analyze(projectId);
    
    // Store insights for future generation
    await this.firestore.saveInsights(projectId, insights);
    
    return insights;
  }
  
  // EXECUTION: Apply decisions and updates
  async executeUpdates(
    projectId: string,
    execution: ExecutionPlan
  ): Promise<void> {
    // Apply all state updates
    await this.batchUpdateStates(projectId, execution.stateUpdates);
    
    // Update plot progressions
    await this.updatePlotThreads(projectId, execution.plotUpdates);
    
    // Record milestones
    await this.recordMilestones(projectId, execution.milestones);
  }
}
```

### Caching Strategy with Memorystore

```typescript
@Injectable()
export class MemoryCacheService {
  private redis: Redis;
  
  constructor() {
    this.redis = new Redis({
      host: process.env.REDIS_HOST || 'localhost',
      port: parseInt(process.env.REDIS_PORT || '6379'),
      family: 4,
      db: 0,
      retryStrategy: (times) => Math.min(times * 50, 2000)
    });
  }
  
  async cacheContext(
    projectId: string,
    sceneId: string,
    context: GenerationContext,
    ttl: number = 3600 // 1 hour
  ): Promise<void> {
    const key = `context:${projectId}:${sceneId}`;
    await this.redis.setex(
      key,
      ttl,
      JSON.stringify(context)
    );
  }
  
  async getCachedContext(
    projectId: string,
    sceneId: string
  ): Promise<GenerationContext | null> {
    const key = `context:${projectId}:${sceneId}`;
    const cached = await this.redis.get(key);
    return cached ? JSON.parse(cached) : null;
  }
  
  async cacheCharacterState(
    characterId: string,
    state: CharacterState,
    ttl: number = 86400 // 24 hours
  ): Promise<void> {
    const key = `char:state:${characterId}`;
    await this.redis.setex(
      key,
      ttl,
      JSON.stringify(state)
    );
  }
  
  // Use Redis for real-time coordination
  async publishMemoryUpdate(
    projectId: string,
    update: MemoryUpdate
  ): Promise<void> {
    const channel = `memory:${projectId}`;
    await this.redis.publish(channel, JSON.stringify(update));
  }
  
  subscribeToUpdates(
    projectId: string,
    callback: (update: MemoryUpdate) => void
  ): void {
    const subscriber = this.redis.duplicate();
    const channel = `memory:${projectId}`;
    
    subscriber.subscribe(channel);
    subscriber.on('message', (ch, message) => {
      if (ch === channel) {
        callback(JSON.parse(message));
      }
    });
  }
}
```

### Analytics with BigQuery

```typescript
@Injectable()
export class MemoryAnalyticsService {
  private bigquery: BigQuery;
  
  constructor() {
    this.bigquery = new BigQuery({
      projectId: process.env.GCP_PROJECT_ID
    });
  }
  
  // Stream events to BigQuery for analysis
  async recordAnalyticsEvent(event: AnalyticsEvent): Promise<void> {
    const dataset = this.bigquery.dataset('novel_memory_analytics');
    const table = dataset.table('events');
    
    await table.insert({
      timestamp: new Date(),
      projectId: event.projectId,
      eventType: event.type,
      userId: event.userId,
      metadata: JSON.stringify(event.metadata)
    });
  }
  
  // Analyze memory patterns across projects
  async analyzeMemoryPatterns(
    timeRange: TimeRange
  ): Promise<MemoryPatternAnalysis> {
    const query = `
      WITH character_activity AS (
        SELECT 
          projectId,
          DATE(timestamp) as date,
          COUNT(DISTINCT JSON_EXTRACT_SCALAR(metadata, '$.characterId')) as active_characters,
          COUNT(*) as state_changes
        FROM \`${process.env.GCP_PROJECT_ID}.novel_memory_analytics.events\`
        WHERE eventType = 'character_state_update'
          AND timestamp BETWEEN @startTime AND @endTime
        GROUP BY projectId, date
      ),
      plot_progression AS (
        SELECT
          projectId,
          COUNT(CASE WHEN JSON_EXTRACT_SCALAR(metadata, '$.status') = 'resolved' THEN 1 END) as resolved_threads,
          AVG(CAST(JSON_EXTRACT_SCALAR(metadata, '$.tensionLevel') AS FLOAT64)) as avg_tension
        FROM \`${process.env.GCP_PROJECT_ID}.novel_memory_analytics.events\`
        WHERE eventType = 'plot_update'
          AND timestamp BETWEEN @startTime AND @endTime
        GROUP BY projectId
      )
      SELECT 
        ca.projectId,
        AVG(ca.active_characters) as avg_active_characters,
        SUM(ca.state_changes) as total_state_changes,
        pp.resolved_threads,
        pp.avg_tension
      FROM character_activity ca
      JOIN plot_progression pp ON ca.projectId = pp.projectId
      GROUP BY ca.projectId, pp.resolved_threads, pp.avg_tension
    `;
    
    const options = {
      query,
      params: {
        startTime: timeRange.start,
        endTime: timeRange.end
      }
    };
    
    const [rows] = await this.bigquery.query(options);
    return this.processAnalyticsResults(rows);
  }
}
```

## Security and Access Control

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Project-level access
    match /projects/{projectId} {
      allow read: if request.auth != null && 
        request.auth.uid in resource.data.members;
      allow write: if request.auth != null && 
        request.auth.uid in resource.data.owners;
      
      // Character memory access
      match /characters/{characterId} {
        allow read: if request.auth != null && 
          hasProjectAccess(projectId, 'read');
        allow write: if request.auth != null && 
          hasProjectAccess(projectId, 'write');
        
        // State history - read only for non-owners
        match /states/{stateId} {
          allow read: if request.auth != null && 
            hasProjectAccess(projectId, 'read');
          allow write: if request.auth != null && 
            hasProjectAccess(projectId, 'owner');
        }
        
        // Observations
        match /observations/{observationId} {
          allow read: if request.auth != null && 
            hasProjectAccess(projectId, 'read');
          allow create: if request.auth != null && 
            hasProjectAccess(projectId, 'write');
          allow update, delete: if false; // Immutable
        }
      }
      
      // Plot memory access
      match /plot/{threadId} {
        allow read: if request.auth != null && 
          hasProjectAccess(projectId, 'read');
        allow write: if request.auth != null && 
          hasProjectAccess(projectId, 'write');
        
        match /milestones/{milestoneId} {
          allow read: if request.auth != null && 
            hasProjectAccess(projectId, 'read');
          allow write: if request.auth != null && 
            hasProjectAccess(projectId, 'write');
        }
      }
      
      // World memory - requires special validation
      match /world/facts/facts/{factId} {
        allow read: if request.auth != null && 
          hasProjectAccess(projectId, 'read');
        allow create: if request.auth != null && 
          hasProjectAccess(projectId, 'write') &&
          isValidWorldFact(request.resource.data);
        allow update: if false; // Facts are immutable
        allow delete: if request.auth != null && 
          hasProjectAccess(projectId, 'owner');
      }
      
      // Summaries are system-generated only
      match /summaries/{summaryId} {
        allow read: if request.auth != null && 
          hasProjectAccess(projectId, 'read');
        allow write: if false; // Only system can write
      }
    }
    
    // Helper functions
    function hasProjectAccess(projectId, level) {
      let project = get(/databases/$(database)/documents/projects/$(projectId));
      return request.auth.uid in project.data.members &&
        (level == 'read' || 
         (level == 'write' && request.auth.uid in project.data.editors) ||
         (level == 'owner' && request.auth.uid in project.data.owners));
    }
    
    function isValidWorldFact(fact) {
      return fact.keys().hasAll(['category', 'fact', 'establishedIn', 'importance']) &&
        fact.category in ['geography', 'history', 'technology', 'magic', 'society', 'other'] &&
        fact.importance in ['trivial', 'minor', 'significant', 'critical'];
    }
  }
}
```

### Service-Level Security

```typescript
@Injectable()
export class MemorySecurityService {
  constructor(
    private firestore: Firestore,
    private authService: AuthService
  ) {}
  
  async validateAccess(
    userId: string,
    projectId: string,
    operation: 'read' | 'write' | 'delete'
  ): Promise<boolean> {
    const project = await this.firestore
      .collection('projects')
      .doc(projectId)
      .get();
    
    if (!project.exists) {
      return false;
    }
    
    const data = project.data()!;
    
    switch (operation) {
      case 'read':
        return data.members.includes(userId);
      case 'write':
        return data.editors.includes(userId) || 
               data.owners.includes(userId);
      case 'delete':
        return data.owners.includes(userId);
      default:
        return false;
    }
  }
  
  // Audit logging
  async logMemoryAccess(
    userId: string,
    projectId: string,
    operation: string,
    resourceType: string,
    resourceId: string
  ): Promise<void> {
    await this.firestore.collection('audit_logs').add({
      timestamp: FieldValue.serverTimestamp(),
      userId,
      projectId,
      operation,
      resourceType,
      resourceId,
      metadata: {
        ip: this.getClientIp(),
        userAgent: this.getUserAgent()
      }
    });
  }
}
```

## Performance Optimization

### Firestore Indexes

```yaml
# firestore.indexes.json
{
  "indexes": [
    {
      "collectionGroup": "characters",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "metadata.lastActive", "order": "DESCENDING" },
        { "fieldPath": "projectId", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "observations",
      "queryScope": "COLLECTION_GROUP",
      "fields": [
        { "fieldPath": "projectId", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "plot",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "tensionLevel", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "facts",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "category", "order": "ASCENDING" },
        { "fieldPath": "importance", "order": "DESCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "summaries",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "level", "order": "ASCENDING" },
        { "fieldPath": "generatedAt", "order": "DESCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

### Query Optimization

```typescript
@Injectable()
export class OptimizedMemoryQueries {
  constructor(private db: Firestore) {}
  
  // Use denormalized data for common queries
  async getCharacterContextFast(
    characterId: string,
    sceneId: string
  ): Promise<CharacterContext> {
    // Single document read instead of multiple queries
    const charDoc = await this.db
      .collection('characters')
      .doc(characterId)
      .get();
    
    if (!charDoc.exists) {
      throw new Error('Character not found');
    }
    
    const data = charDoc.data()!;
    
    // Current state is denormalized on the document
    return {
      currentState: data.currentState,
      stats: data.stats,
      recentObservations: await this.getRecentObservations(
        characterId,
        5 // Last 5 observations
      )
    };
  }
  
  // Batch reads for efficiency
  async getMultipleCharacterStates(
    characterIds: string[],
    sceneId: string
  ): Promise<Map<string, CharacterState>> {
    const states = new Map<string, CharacterState>();
    
    // Firestore allows up to 10 documents per batch get
    const chunks = this.chunkArray(characterIds, 10);
    
    await Promise.all(chunks.map(async (chunk) => {
      const refs = chunk.map(id => 
        this.db.collection('characters').doc(id)
      );
      
      const docs = await this.db.getAll(...refs);
      
      docs.forEach((doc, index) => {
        if (doc.exists) {
          states.set(chunk[index], doc.data()!.currentState);
        }
      });
    }));
    
    return states;
  }
  
  // Use pagination for large result sets
  async* getCharacterTimelinePaginated(
    characterId: string,
    pageSize: number = 20
  ): AsyncGenerator<CharacterState[]> {
    let lastDoc: any = null;
    let hasMore = true;
    
    while (hasMore) {
      let query = this.db
        .collection('characters')
        .doc(characterId)
        .collection('states')
        .orderBy('timestamp', 'desc')
        .limit(pageSize);
      
      if (lastDoc) {
        query = query.startAfter(lastDoc);
      }
      
      const snapshot = await query.get();
      
      if (snapshot.empty) {
        hasMore = false;
      } else {
        lastDoc = snapshot.docs[snapshot.docs.length - 1];
        yield snapshot.docs.map(doc => doc.data() as CharacterState);
      }
    }
  }
}
```

## Monitoring and Observability

### Metrics Collection

```typescript
@Injectable()
export class MemoryMetricsService {
  private metrics: MetricsClient;
  
  constructor() {
    this.metrics = new MetricsClient({
      projectId: process.env.GCP_PROJECT_ID
    });
  }
  
  async recordMemoryOperation(
    operation: string,
    duration: number,
    success: boolean,
    metadata?: Record<string, any>
  ): Promise<void> {
    const timeSeries = {
      metric: {
        type: 'custom.googleapis.com/memory/operation',
        labels: {
          operation,
          success: success.toString()
        }
      },
      resource: {
        type: 'global',
        labels: {}
      },
      points: [{
        interval: {
          endTime: {
            seconds: Date.now() / 1000
          }
        },
        value: {
          doubleValue: duration
        }
      }]
    };
    
    await this.metrics.createTimeSeries({
      name: `projects/${process.env.GCP_PROJECT_ID}`,
      timeSeries: [timeSeries]
    });
  }
  
  // Custom dashboard metrics
  async getMemoryHealth(projectId: string): Promise<MemoryHealth> {
    const [
      characterCount,
      activeThreads,
      contradictionCount,
      summaryFreshness
    ] = await Promise.all([
      this.countActiveCharacters(projectId),
      this.countActiveThreads(projectId),
      this.countUnresolvedContradictions(projectId),
      this.checkSummaryFreshness(projectId)
    ]);
    
    return {
      projectId,
      metrics: {
        characterCount,
        activeThreads,
        contradictionCount,
        summaryFreshness
      },
      health: this.calculateHealthScore({
        characterCount,
        activeThreads,
        contradictionCount,
        summaryFreshness
      }),
      timestamp: new Date()
    };
  }
}
```

### Logging

```typescript
@Injectable()
export class MemoryLoggingService {
  private logger: Logging;
  
  constructor() {
    this.logger = new Logging({
      projectId: process.env.GCP_PROJECT_ID
    });
  }
  
  async logMemoryEvent(
    severity: 'INFO' | 'WARNING' | 'ERROR',
    message: string,
    context: Record<string, any>
  ): Promise<void> {
    const log = this.logger.log('memory-service');
    const entry = log.entry({
      severity,
      resource: {
        type: 'cloud_function',
        labels: {
          function_name: 'memory-service',
          region: process.env.FUNCTION_REGION || 'us-central1'
        }
      },
      jsonPayload: {
        message,
        ...context,
        timestamp: new Date().toISOString()
      }
    });
    
    await log.write(entry);
  }
}
```

## Testing Strategy

### Unit Tests

```typescript
describe('FirestoreMemoryService', () => {
  let service: FirestoreMemoryService;
  let mockFirestore: any;
  
  beforeEach(() => {
    mockFirestore = {
      collection: jest.fn().mockReturnThis(),
      doc: jest.fn().mockReturnThis(),
      get: jest.fn(),
      set: jest.fn(),
      update: jest.fn(),
      runTransaction: jest.fn()
    };
    
    service = new FirestoreMemoryService();
    (service as any).db = mockFirestore;
  });
  
  describe('updateCharacterState', () => {
    it('should update character state successfully', async () => {
      const characterId = 'char-123';
      const sceneId = 'scene-456';
      const update = {
        projectId: 'proj-789',
        changes: {
          physical: { location: 'Forest' }
        }
      };
      
      mockFirestore.runTransaction.mockImplementation(
        async (callback: any) => {
          const mockTransaction = {
            get: jest.fn().mockResolvedValue({
              exists: true,
              data: () => ({
                currentState: {
                  physical: { location: 'Castle' },
                  emotional: { mood: 'neutral' }
                }
              })
            }),
            update: jest.fn(),
            set: jest.fn()
          };
          
          return callback(mockTransaction);
        }
      );
      
      const result = await service.updateCharacterState(
        characterId,
        sceneId,
        update
      );
      
      expect(result).toBeDefined();
      expect(mockFirestore.runTransaction).toHaveBeenCalled();
    });
  });
});
```

### Integration Tests

```typescript
describe('Memory Service Integration', () => {
  let app: INestApplication;
  let memoryService: MemoryService;
  
  beforeAll(async () => {
    const moduleFixture = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();
    
    app = moduleFixture.createNestApplication();
    await app.init();
    
    memoryService = app.get<MemoryService>(MemoryService);
  });
  
  afterAll(async () => {
    await app.close();
  });
  
  it('should build complete generation context', async () => {
    // Create test data
    const projectId = await createTestProject();
    const sceneId = await createTestScene(projectId);
    await populateTestMemory(projectId);
    
    // Test context building
    const context = await memoryService.buildGenerationContext(
      projectId,
      sceneId,
      { depth: 2, maxTokens: 50000 }
    );
    
    expect(context).toMatchObject({
      projectId,
      sceneId,
      characters: expect.any(Array),
      plot: expect.objectContaining({
        activeThreads: expect.any(Array)
      }),
      world: expect.objectContaining({
        establishedFacts: expect.any(Array)
      })
    });
  });
});
```

## Performance Requirements

| Metric | Target | Measurement |
|--------|--------|-------------|
| Document read latency | < 20ms | P95 |
| State update latency | < 50ms | P95 |
| Context retrieval | < 200ms | P95 |
| Search query time | < 100ms | P95 |
| Real-time update delivery | < 100ms | P95 |
| Batch write throughput | 1000 docs/sec | Peak |
| Cache hit rate | > 85% | Average |
| Storage cost | < $0.18/GB/month | Monthly |

## Deployment Configuration

### Cloud Function Deployment

```yaml
# memory-service/function.yaml
name: memory-service
runtime: nodejs18
memory: 2048MB
timeout: 60s
max_instances: 100
min_instances: 2
vpc_connector: projects/deus-ex-machina/locations/us-central1/connectors/vpc-connector

environment_variables:
  NODE_ENV: production
  GCP_PROJECT_ID: deus-ex-machina
  REDIS_HOST: 10.0.0.5
  ENABLE_PROFILER: "true"

service_account: memory-service@deus-ex-machina.iam.gserviceaccount.com
```

### Terraform Configuration

```hcl
# Memory Service Infrastructure
resource "google_firestore_database" "memory" {
  project     = var.project_id
  name        = "(default)"
  location_id = var.region
  type        = "FIRESTORE_NATIVE"
}

resource "google_redis_instance" "memory_cache" {
  name           = "memory-cache"
  memory_size_gb = 4
  region         = var.region
  tier           = "STANDARD_HA"
  
  redis_configs = {
    maxmemory-policy = "allkeys-lru"
  }
}

resource "google_bigquery_dataset" "memory_analytics" {
  dataset_id = "novel_memory_analytics"
  location   = var.location
  
  default_table_expiration_ms = 7776000000 # 90 days
}

resource "google_bigquery_table" "memory_events" {
  dataset_id = google_bigquery_dataset.memory_analytics.dataset_id
  table_id   = "events"
  
  time_partitioning {
    type  = "DAY"
    field = "timestamp"
  }
  
  schema = jsonencode([
    {
      name = "timestamp"
      type = "TIMESTAMP"
      mode = "REQUIRED"
    },
    {
      name = "projectId"
      type = "STRING"
      mode = "REQUIRED"
    },
    {
      name = "eventType"
      type = "STRING"
      mode = "REQUIRED"
    },
    {
      name = "metadata"
      type = "JSON"
      mode = "NULLABLE"
    }
  ])
}
```

## Migration Strategy

### From PostgreSQL to Firestore

```typescript
@Injectable()
export class MemoryMigrationService {
  async migrateToFirestore(
    projectId: string,
    batchSize: number = 500
  ): Promise<MigrationResult> {
    const result: MigrationResult = {
      charactersM migrated: 0,
      plotThreadsMigrated: 0,
      factsMigrated: 0,
      errors: []
    };
    
    try {
      // Migrate characters
      await this.migrateCharacters(projectId, batchSize, result);
      
      // Migrate plot threads
      await this.migratePlotThreads(projectId, batchSize, result);
      
      // Migrate world facts
      await this.migrateWorldFacts(projectId, batchSize, result);
      
      // Migrate relationships
      await this.migrateRelationships(projectId, batchSize, result);
      
    } catch (error) {
      result.errors.push({
        phase: 'migration',
        error: error.message
      });
    }
    
    return result;
  }
}
```

## Advantages of Firestore Approach

1. **Simplified Architecture**: No need for separate PostgreSQL, Redis, and Neo4j instances
2. **Real-time Updates**: Native support for real-time listeners
3. **Automatic Scaling**: Handles millions of concurrent users without configuration
4. **Cost Effective**: Pay only for what you use, no idle infrastructure
5. **Flexible Schema**: Easy to evolve data models without migrations
6. **Native GCP Integration**: Works seamlessly with other Google Cloud services
7. **Built-in Security**: Granular security rules at the database level
8. **Offline Support**: SDK handles offline/online synchronization automatically

## Conclusion

The Firestore-based Memory Service provides a scalable, flexible, and cost-effective solution for managing narrative memory in the Novel Creator platform. By leveraging Firestore's document model, real-time capabilities, and seamless integration with Google Cloud services, we can deliver a robust memory system that maintains narrative coherence while supporting collaborative features and advanced analytics. The simplified architecture reduces operational overhead while providing better performance and reliability than a traditional multi-database approach.