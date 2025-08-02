# Technical Specification: Frontend Architecture

## Overview

The Novel Creator Frontend is a React 18+ application built with TypeScript, featuring a sophisticated document editor with virtual scrolling, real-time collaboration, and AI-powered writing assistance. The architecture emphasizes performance for handling 500k+ word documents, offline capabilities through service workers, and a responsive design that works across desktop and tablet devices.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Novel Creator Frontend                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────────┐   │
│  │     Shell      │  │   Router       │  │   Auth Layer      │   │
│  │   Component    │  │  (React Router)│  │   (Protected)     │   │
│  └────────┬───────┘  └────────┬───────┘  └─────────┬─────────┘   │
│           │                    │                      │             │
│  ┌────────▼───────────────────▼──────────────────────▼─────────┐  │
│  │                      Core Features                           │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │   Editor    │  │   Memory     │  │  Collaboration    │ │  │
│  │  │   Module    │  │   Module     │  │     Module        │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────┬──────────────────────────────────┘  │
│                             │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐  │
│  │                    State Management                          │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │Redux Toolkit│  │  RTK Query   │  │   Local State     │ │  │
│  │  │   (Global)  │  │   (API)      │  │   (Component)     │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Module Architecture

```
apps/frontend/novel-creator/
├── src/
│   ├── app/                    # Application shell
│   ├── features/               # Feature modules
│   │   ├── editor/            # Document editor
│   │   ├── memory/            # Character/plot tracking
│   │   ├── generation/        # AI generation
│   │   ├── collaboration/     # Real-time features
│   │   └── export/            # Export functionality
│   ├── shared/                # Shared components
│   │   ├── components/        # Reusable UI components
│   │   ├── hooks/             # Custom React hooks
│   │   ├── utils/             # Utility functions
│   │   └── types/             # TypeScript types
│   ├── store/                 # Redux store configuration
│   ├── services/              # API and external services
│   └── styles/                # Global styles and themes
├── public/                    # Static assets
├── tests/                     # Test files
└── vite.config.ts            # Vite configuration
```

## Core Components

### Document Editor

```typescript
// Editor Architecture
interface EditorState {
  document: {
    id: string;
    metadata: DocumentMetadata;
    structure: DocumentStructure;
    loadedChunks: Map<string, ContentChunk>;
    viewport: ViewportState;
  };
  selection: SelectionState;
  formatting: FormattingState;
  collaboration: CollaborationState;
  history: HistoryState;
}

interface ViewportState {
  scrollTop: number;
  height: number;
  visibleChunks: string[];
  bufferSize: number;
  zoom: number;
}

interface SelectionState {
  anchor: Position;
  focus: Position;
  type: 'caret' | 'range';
  affinity: 'forward' | 'backward';
}

interface Position {
  chunkId: string;
  sceneId: string;
  paragraphId: string;
  offset: number;
}

// Virtual Renderer Component
interface VirtualDocumentRenderer {
  render(): JSX.Element;
  scrollToPosition(position: Position): void;
  getVisibleRange(): Range;
  preloadChunks(chunkIds: string[]): Promise<void>;
}

// Editor Commands
interface EditorCommand {
  id: string;
  execute(state: EditorState): EditorState;
  undo(state: EditorState): EditorState;
  canExecute(state: EditorState): boolean;
}

interface CommandManager {
  execute(commandId: string, params?: any): void;
  undo(): void;
  redo(): void;
  canUndo(): boolean;
  canRedo(): boolean;
  registerCommand(command: EditorCommand): void;
}
```

### Component Library

```typescript
// Core UI Components
interface UIComponents {
  // Layout
  AppShell: React.FC<AppShellProps>;
  Sidebar: React.FC<SidebarProps>;
  Toolbar: React.FC<ToolbarProps>;
  StatusBar: React.FC<StatusBarProps>;
  
  // Editor
  TextEditor: React.FC<TextEditorProps>;
  FormatToolbar: React.FC<FormatToolbarProps>;
  CharacterMention: React.FC<CharacterMentionProps>;
  SceneBreak: React.FC<SceneBreakProps>;
  
  // Memory
  CharacterCard: React.FC<CharacterCardProps>;
  PlotThread: React.FC<PlotThreadProps>;
  WorldFactPanel: React.FC<WorldFactPanelProps>;
  Timeline: React.FC<TimelineProps>;
  
  // Generation
  GenerationPanel: React.FC<GenerationPanelProps>;
  SuggestionCard: React.FC<SuggestionCardProps>;
  PromptBuilder: React.FC<PromptBuilderProps>;
  
  // Collaboration
  PresenceIndicator: React.FC<PresenceIndicatorProps>;
  CursorOverlay: React.FC<CursorOverlayProps>;
  CommentThread: React.FC<CommentThreadProps>;
  
  // Common
  Button: React.FC<ButtonProps>;
  Input: React.FC<InputProps>;
  Select: React.FC<SelectProps>;
  Modal: React.FC<ModalProps>;
  Toast: React.FC<ToastProps>;
  Tooltip: React.FC<TooltipProps>;
  ContextMenu: React.FC<ContextMenuProps>;
}

// Theme System
interface Theme {
  colors: {
    primary: ColorScale;
    secondary: ColorScale;
    neutral: ColorScale;
    success: ColorScale;
    warning: ColorScale;
    error: ColorScale;
    
    // Editor specific
    editor: {
      background: string;
      text: string;
      selection: string;
      cursor: string;
      lineNumber: string;
      activeLineBackground: string;
    };
  };
  
  typography: {
    fontFamily: {
      sans: string;
      serif: string;
      mono: string;
    };
    fontSize: Scale;
    fontWeight: Scale;
    lineHeight: Scale;
  };
  
  spacing: Scale;
  radii: Scale;
  shadows: Scale;
  transitions: TransitionScale;
  
  // Responsive breakpoints
  breakpoints: {
    sm: string;
    md: string;
    lg: string;
    xl: string;
  };
}
```

## State Management

### Redux Store Structure

```typescript
// Store Configuration
interface RootState {
  // Core features
  editor: EditorState;
  memory: MemoryState;
  generation: GenerationState;
  collaboration: CollaborationState;
  export: ExportState;
  
  // App level
  auth: AuthState;
  ui: UIState;
  settings: SettingsState;
  
  // API state (RTK Query)
  api: ApiState;
}

// Editor Slice
const editorSlice = createSlice({
  name: 'editor',
  initialState: {
    document: null,
    selection: null,
    isLoading: false,
    isSaving: false,
    lastSaved: null,
    chunks: {},
    viewport: {
      scrollTop: 0,
      height: 0,
      visibleChunks: []
    }
  },
  reducers: {
    loadDocument: (state, action) => {
      state.document = action.payload;
      state.isLoading = false;
    },
    updateSelection: (state, action) => {
      state.selection = action.payload;
    },
    addChunk: (state, action) => {
      const { chunkId, chunk } = action.payload;
      state.chunks[chunkId] = chunk;
    },
    updateViewport: (state, action) => {
      state.viewport = { ...state.viewport, ...action.payload };
    },
    applyOperation: (state, action) => {
      // Apply OT operation to document
      const operation = action.payload;
      applyOperationToState(state, operation);
      state.lastSaved = null;
    }
  },
  extraReducers: (builder) => {
    // Handle async actions
    builder.addCase(saveDocument.fulfilled, (state) => {
      state.isSaving = false;
      state.lastSaved = new Date().toISOString();
    });
  }
});

// Memory Slice
const memorySlice = createSlice({
  name: 'memory',
  initialState: {
    characters: {},
    plotThreads: {},
    worldFacts: {},
    relationships: [],
    activeCharacter: null,
    searchResults: []
  },
  reducers: {
    updateCharacterState: (state, action) => {
      const { characterId, update } = action.payload;
      state.characters[characterId] = {
        ...state.characters[characterId],
        ...update
      };
    },
    addPlotMilestone: (state, action) => {
      const { threadId, milestone } = action.payload;
      state.plotThreads[threadId].milestones.push(milestone);
    },
    setActiveCharacter: (state, action) => {
      state.activeCharacter = action.payload;
    }
  }
});

// RTK Query API
const novelApi = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/v1',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token;
      if (token) {
        headers.set('authorization', `Bearer ${token}`);
      }
      return headers;
    }
  }),
  tagTypes: ['Document', 'Character', 'Export'],
  endpoints: (builder) => ({
    // Document endpoints
    getDocument: builder.query<Document, string>({
      query: (id) => `documents/${id}`,
      providesTags: ['Document']
    }),
    updateDocument: builder.mutation<Document, UpdateDocumentRequest>({
      query: ({ id, ...patch }) => ({
        url: `documents/${id}`,
        method: 'PATCH',
        body: patch
      }),
      invalidatesTags: ['Document']
    }),
    
    // Generation endpoints
    generateContent: builder.mutation<GenerationResult, GenerationRequest>({
      query: (request) => ({
        url: 'generation/generate',
        method: 'POST',
        body: request
      })
    }),
    
    // Memory endpoints
    getCharacters: builder.query<Character[], string>({
      query: (projectId) => `memory/characters?projectId=${projectId}`,
      providesTags: ['Character']
    }),
    
    // Export endpoints
    createExport: builder.mutation<ExportRequest, CreateExportRequest>({
      query: (request) => ({
        url: 'export',
        method: 'POST',
        body: request
      }),
      invalidatesTags: ['Export']
    })
  })
});
```

### Local State Management

```typescript
// Custom hooks for local state
export function useEditorState() {
  const dispatch = useAppDispatch();
  const editorState = useAppSelector(state => state.editor);
  
  const [localState, setLocalState] = useState({
    isDirty: false,
    pendingOperations: [] as Operation[],
    cursorBlink: true
  });
  
  // Auto-save logic
  useEffect(() => {
    if (localState.isDirty && !editorState.isSaving) {
      const timer = setTimeout(() => {
        dispatch(saveDocument());
        setLocalState(prev => ({ ...prev, isDirty: false }));
      }, 5000); // 5 second debounce
      
      return () => clearTimeout(timer);
    }
  }, [localState.isDirty, editorState.isSaving]);
  
  return {
    ...editorState,
    localState,
    updateContent: (operation: Operation) => {
      dispatch(applyOperation(operation));
      setLocalState(prev => ({ ...prev, isDirty: true }));
    }
  };
}

// Optimistic updates
export function useOptimisticUpdate<T>(
  mutation: any,
  options?: OptimisticOptions
) {
  const [optimisticData, setOptimisticData] = useState<T | null>(null);
  
  const executeMutation = useCallback(async (variables: any) => {
    // Apply optimistic update
    if (options?.optimisticResponse) {
      setOptimisticData(options.optimisticResponse(variables));
    }
    
    try {
      const result = await mutation(variables).unwrap();
      setOptimisticData(null);
      return result;
    } catch (error) {
      // Rollback optimistic update
      setOptimisticData(null);
      throw error;
    }
  }, [mutation, options]);
  
  return [executeMutation, { optimisticData }];
}
```

## UI/UX Architecture

### Design System

```typescript
// Component Library Structure
export const DesignSystem = {
  // Atomic components
  atoms: {
    Button,
    Input,
    Label,
    Text,
    Icon,
    Spinner,
    Avatar,
    Badge
  },
  
  // Molecular components
  molecules: {
    FormField,
    Card,
    Dropdown,
    SearchBox,
    ToggleGroup,
    Slider,
    DatePicker
  },
  
  // Organism components
  organisms: {
    Header,
    Sidebar,
    DataTable,
    Form,
    Modal,
    Toolbar,
    TabPanel
  },
  
  // Templates
  templates: {
    PageLayout,
    AuthLayout,
    EditorLayout,
    DashboardLayout
  }
};

// Responsive Design Utilities
export const responsive = {
  // Breakpoint helpers
  breakpoints: {
    sm: '640px',
    md: '768px',
    lg: '1024px',
    xl: '1280px',
    '2xl': '1536px'
  },
  
  // Media query hooks
  useBreakpoint: () => {
    const [breakpoint, setBreakpoint] = useState(getBreakpoint());
    
    useEffect(() => {
      const handleResize = () => setBreakpoint(getBreakpoint());
      window.addEventListener('resize', handleResize);
      return () => window.removeEventListener('resize', handleResize);
    }, []);
    
    return breakpoint;
  },
  
  // Responsive component wrapper
  Responsive: ({ children, show, hide }) => {
    const breakpoint = useBreakpoint();
    // Logic to show/hide based on breakpoint
    return <>{children}</>;
  }
};

// Accessibility utilities
export const a11y = {
  // Screen reader only text
  ScreenReaderOnly: styled.span`
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  `,
  
  // Focus management
  useFocusTrap: (ref: RefObject<HTMLElement>) => {
    useEffect(() => {
      const element = ref.current;
      if (!element) return;
      
      const focusableElements = element.querySelectorAll(
        'a[href], button, textarea, input, select, [tabindex]:not([tabindex="-1"])'
      );
      
      const firstFocusable = focusableElements[0] as HTMLElement;
      const lastFocusable = focusableElements[focusableElements.length - 1] as HTMLElement;
      
      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === 'Tab') {
          if (e.shiftKey && document.activeElement === firstFocusable) {
            e.preventDefault();
            lastFocusable.focus();
          } else if (!e.shiftKey && document.activeElement === lastFocusable) {
            e.preventDefault();
            firstFocusable.focus();
          }
        }
      };
      
      element.addEventListener('keydown', handleKeyDown);
      firstFocusable?.focus();
      
      return () => element.removeEventListener('keydown', handleKeyDown);
    }, [ref]);
  },
  
  // Announcements
  useAnnounce: () => {
    const announce = useCallback((message: string, priority: 'polite' | 'assertive' = 'polite') => {
      const announcement = document.createElement('div');
      announcement.setAttribute('role', 'status');
      announcement.setAttribute('aria-live', priority);
      announcement.className = 'sr-only';
      announcement.textContent = message;
      
      document.body.appendChild(announcement);
      setTimeout(() => document.body.removeChild(announcement), 1000);
    }, []);
    
    return announce;
  }
};
```

### Editor UI Components

```typescript
// Main Editor Component
export const NovelEditor: React.FC = () => {
  const { document, viewport, selection } = useEditorState();
  const { activeUsers } = useCollaboration();
  const virtualRenderer = useVirtualRenderer();
  
  return (
    <EditorLayout>
      <EditorToolbar />
      
      <EditorContainer>
        <LineNumbers viewport={viewport} />
        
        <EditorViewport
          onScroll={handleScroll}
          style={{ height: viewport.height }}
        >
          <VirtualScroller
            totalHeight={document?.totalHeight || 0}
            viewport={viewport}
            renderChunk={renderChunk}
          />
          
          <SelectionOverlay selection={selection} />
          
          <CollaborationLayer>
            {activeUsers.map(user => (
              <UserCursor
                key={user.id}
                user={user}
                position={user.cursor}
              />
            ))}
          </CollaborationLayer>
        </EditorViewport>
        
        <EditorGutter>
          <CharacterPanel />
          <OutlinePanel />
        </EditorGutter>
      </EditorContainer>
      
      <StatusBar />
    </EditorLayout>
  );
};

// Virtual Scroller Implementation
const VirtualScroller: React.FC<VirtualScrollerProps> = ({
  totalHeight,
  viewport,
  renderChunk
}) => {
  const visibleChunks = useVisibleChunks(viewport);
  const chunkCache = useChunkCache();
  
  return (
    <ScrollContainer style={{ height: totalHeight }}>
      <ScrollOffset style={{ transform: `translateY(${viewport.scrollTop}px)` }}>
        {visibleChunks.map(chunk => (
          <ChunkRenderer
            key={chunk.id}
            chunk={chunk}
            isVisible={true}
            onLoad={() => chunkCache.set(chunk.id, chunk)}
          />
        ))}
      </ScrollOffset>
      
      {/* Invisible chunks for measurement */}
      <MeasurementContainer>
        {pendingChunks.map(chunk => (
          <ChunkMeasurer
            key={chunk.id}
            chunk={chunk}
            onMeasured={dimensions => updateChunkDimensions(chunk.id, dimensions)}
          />
        ))}
      </MeasurementContainer>
    </ScrollContainer>
  );
};

// Rich Text Formatting Toolbar
const FormatToolbar: React.FC = () => {
  const { selection, canFormat } = useSelection();
  const { executeCommand } = useCommands();
  
  if (!selection || !canFormat) return null;
  
  const position = getToolbarPosition(selection);
  
  return (
    <FloatingToolbar style={{ top: position.top, left: position.left }}>
      <ToolbarButton
        icon="bold"
        active={selection.format.bold}
        onClick={() => executeCommand('toggleBold')}
        tooltip="Bold (Ctrl+B)"
      />
      <ToolbarButton
        icon="italic"
        active={selection.format.italic}
        onClick={() => executeCommand('toggleItalic')}
        tooltip="Italic (Ctrl+I)"
      />
      <ToolbarSeparator />
      <ToolbarButton
        icon="quote"
        onClick={() => executeCommand('toggleBlockquote')}
        tooltip="Quote"
      />
      <ToolbarButton
        icon="link"
        onClick={() => executeCommand('insertLink')}
        tooltip="Link (Ctrl+K)"
      />
      <ToolbarSeparator />
      <ToolbarDropdown
        icon="paragraph"
        options={paragraphStyles}
        value={selection.paragraphStyle}
        onChange={style => executeCommand('setParagraphStyle', style)}
      />
    </FloatingToolbar>
  );
};
```

## Performance Optimization

### Code Splitting

```typescript
// Route-based code splitting
const routes = [
  {
    path: '/',
    component: lazy(() => import('./pages/Dashboard'))
  },
  {
    path: '/novel/:id',
    component: lazy(() => import('./pages/NovelEditor'))
  },
  {
    path: '/novel/:id/memory',
    component: lazy(() => import('./pages/MemoryView'))
  },
  {
    path: '/novel/:id/export',
    component: lazy(() => import('./pages/ExportView'))
  },
  {
    path: '/settings',
    component: lazy(() => import('./pages/Settings'))
  }
];

// Feature-based code splitting
const AIGenerationPanel = lazy(() => 
  import('./features/generation/GenerationPanel')
);

const CollaborationOverlay = lazy(() => 
  import('./features/collaboration/CollaborationOverlay')
);

// Component with suspense boundary
export const App: React.FC = () => {
  return (
    <Router>
      <Suspense fallback={<LoadingScreen />}>
        <Routes>
          {routes.map(route => (
            <Route
              key={route.path}
              path={route.path}
              element={<route.component />}
            />
          ))}
        </Routes>
      </Suspense>
    </Router>
  );
};
```

### Memory Management

```typescript
// Chunk memory management
class ChunkMemoryManager {
  private cache: LRUCache<string, ContentChunk>;
  private loadingChunks: Map<string, Promise<ContentChunk>>;
  private memoryPressureThreshold = 100 * 1024 * 1024; // 100MB
  
  constructor() {
    this.cache = new LRUCache({
      maxSize: 50, // Maximum 50 chunks in memory
      sizeCalculation: (chunk) => JSON.stringify(chunk).length,
      dispose: (chunk) => this.disposeChunk(chunk)
    });
    
    this.loadingChunks = new Map();
    this.monitorMemoryPressure();
  }
  
  async getChunk(chunkId: string): Promise<ContentChunk> {
    // Check cache
    const cached = this.cache.get(chunkId);
    if (cached) return cached;
    
    // Check if already loading
    const loading = this.loadingChunks.get(chunkId);
    if (loading) return loading;
    
    // Load chunk
    const loadPromise = this.loadChunk(chunkId);
    this.loadingChunks.set(chunkId, loadPromise);
    
    try {
      const chunk = await loadPromise;
      this.cache.set(chunkId, chunk);
      return chunk;
    } finally {
      this.loadingChunks.delete(chunkId);
    }
  }
  
  private monitorMemoryPressure() {
    if ('memory' in performance) {
      setInterval(() => {
        const memory = (performance as any).memory;
        if (memory.usedJSHeapSize > this.memoryPressureThreshold) {
          this.cache.clear();
          console.warn('Memory pressure detected, clearing chunk cache');
        }
      }, 5000);
    }
  }
  
  private disposeChunk(chunk: ContentChunk) {
    // Clean up any resources
    chunk.paragraphs = [];
    chunk.scenes = [];
  }
}

// React memory optimization hooks
export function useMemoryOptimizedList<T>(
  items: T[],
  renderItem: (item: T) => React.ReactNode
) {
  const [visibleRange, setVisibleRange] = useState({ start: 0, end: 50 });
  const containerRef = useRef<HTMLDivElement>(null);
  
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    
    const observer = new IntersectionObserver(
      (entries) => {
        // Update visible range based on intersection
        const firstVisible = entries.find(e => e.isIntersecting);
        if (firstVisible) {
          const index = parseInt(firstVisible.target.getAttribute('data-index')!);
          setVisibleRange({
            start: Math.max(0, index - 10),
            end: Math.min(items.length, index + 60)
          });
        }
      },
      { root: container, rootMargin: '100px' }
    );
    
    // Observe sentinel elements
    const sentinels = container.querySelectorAll('.list-sentinel');
    sentinels.forEach(el => observer.observe(el));
    
    return () => observer.disconnect();
  }, [items.length]);
  
  const visibleItems = items.slice(visibleRange.start, visibleRange.end);
  
  return {
    containerRef,
    visibleItems,
    renderList: () => (
      <div ref={containerRef}>
        {visibleRange.start > 0 && (
          <div 
            className="list-sentinel" 
            data-index="0"
            style={{ height: visibleRange.start * ITEM_HEIGHT }}
          />
        )}
        
        {visibleItems.map((item, index) => (
          <div key={visibleRange.start + index}>
            {renderItem(item)}
          </div>
        ))}
        
        {visibleRange.end < items.length && (
          <div
            className="list-sentinel"
            data-index={items.length - 1}
            style={{ height: (items.length - visibleRange.end) * ITEM_HEIGHT }}
          />
        )}
      </div>
    )
  };
}
```

### Performance Monitoring

```typescript
// Performance monitoring service
class PerformanceMonitor {
  private metrics: Map<string, PerformanceMetric> = new Map();
  
  measureComponent(componentName: string) {
    return (WrappedComponent: React.ComponentType) => {
      return React.memo((props: any) => {
        const renderStart = performance.now();
        
        useEffect(() => {
          const renderEnd = performance.now();
          this.recordMetric(componentName, 'render', renderEnd - renderStart);
        });
        
        return <WrappedComponent {...props} />;
      });
    };
  }
  
  measureAsync<T>(name: string, fn: () => Promise<T>): Promise<T> {
    const start = performance.now();
    
    return fn().then(
      result => {
        this.recordMetric(name, 'async', performance.now() - start);
        return result;
      },
      error => {
        this.recordMetric(name, 'async-error', performance.now() - start);
        throw error;
      }
    );
  }
  
  private recordMetric(name: string, type: string, duration: number) {
    const key = `${name}:${type}`;
    let metric = this.metrics.get(key);
    
    if (!metric) {
      metric = {
        name,
        type,
        count: 0,
        total: 0,
        average: 0,
        min: Infinity,
        max: -Infinity
      };
      this.metrics.set(key, metric);
    }
    
    metric.count++;
    metric.total += duration;
    metric.average = metric.total / metric.count;
    metric.min = Math.min(metric.min, duration);
    metric.max = Math.max(metric.max, duration);
    
    // Send to analytics if threshold exceeded
    if (duration > PERFORMANCE_THRESHOLD[type]) {
      this.reportSlowOperation(name, type, duration);
    }
  }
  
  getReport(): PerformanceReport {
    return {
      metrics: Array.from(this.metrics.values()),
      timestamp: new Date(),
      userAgent: navigator.userAgent,
      memory: (performance as any).memory
    };
  }
}

// Usage in components
@measureComponent('NovelEditor')
export class NovelEditor extends React.Component {
  // Component implementation
}
```

## Service Worker & Offline Support

### Service Worker Implementation

```typescript
// service-worker.ts
/// <reference lib="webworker" />
declare const self: ServiceWorkerGlobalScope;

const CACHE_NAME = 'novel-creator-v1';
const API_CACHE = 'novel-creator-api-v1';
const CHUNK_CACHE = 'novel-creator-chunks-v1';

// Assets to cache immediately
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/assets/app.js',
  '/assets/app.css'
];

// Install event
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS);
    })
  );
});

// Fetch event with advanced caching strategies
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);
  
  // API requests
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(handleApiRequest(request));
    return;
  }
  
  // Document chunks
  if (url.pathname.includes('/chunks/')) {
    event.respondWith(handleChunkRequest(request));
    return;
  }
  
  // Static assets
  event.respondWith(handleStaticRequest(request));
});

async function handleApiRequest(request: Request): Promise<Response> {
  // Try network first for API requests
  try {
    const response = await fetch(request);
    
    // Cache successful GET requests
    if (request.method === 'GET' && response.ok) {
      const cache = await caches.open(API_CACHE);
      cache.put(request, response.clone());
    }
    
    return response;
  } catch (error) {
    // Fallback to cache for GET requests
    if (request.method === 'GET') {
      const cached = await caches.match(request);
      if (cached) {
        return cached;
      }
    }
    
    // Return offline response
    return new Response(
      JSON.stringify({ error: 'Offline' }),
      { status: 503, headers: { 'Content-Type': 'application/json' } }
    );
  }
}

async function handleChunkRequest(request: Request): Promise<Response> {
  const cache = await caches.open(CHUNK_CACHE);
  
  // Check cache first for chunks
  const cached = await cache.match(request);
  if (cached) {
    // Update cache in background
    event.waitUntil(
      fetch(request).then(response => {
        if (response.ok) {
          cache.put(request, response);
        }
      })
    );
    return cached;
  }
  
  // Fetch from network
  try {
    const response = await fetch(request);
    if (response.ok) {
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    return new Response('Chunk not available offline', { status: 503 });
  }
}

// Background sync for offline changes
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-documents') {
    event.waitUntil(syncOfflineChanges());
  }
});

async function syncOfflineChanges() {
  const db = await openDB('novel-creator-offline', 1);
  const tx = db.transaction('pending-changes', 'readonly');
  const changes = await tx.objectStore('pending-changes').getAll();
  
  for (const change of changes) {
    try {
      await fetch(change.url, {
        method: change.method,
        headers: change.headers,
        body: change.body
      });
      
      // Remove synced change
      await db.delete('pending-changes', change.id);
    } catch (error) {
      console.error('Sync failed for change:', change.id);
    }
  }
}
```

### Offline Storage

```typescript
// Offline storage manager
class OfflineStorageManager {
  private db: IDBDatabase | null = null;
  private readonly DB_NAME = 'novel-creator-offline';
  private readonly DB_VERSION = 1;
  
  async initialize() {
    return new Promise<void>((resolve, reject) => {
      const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);
      
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };
      
      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        
        // Documents store
        if (!db.objectStoreNames.contains('documents')) {
          const documentStore = db.createObjectStore('documents', {
            keyPath: 'id'
          });
          documentStore.createIndex('projectId', 'projectId');
          documentStore.createIndex('updatedAt', 'updatedAt');
        }
        
        // Chunks store
        if (!db.objectStoreNames.contains('chunks')) {
          const chunkStore = db.createObjectStore('chunks', {
            keyPath: ['documentId', 'chunkId']
          });
          chunkStore.createIndex('documentId', 'documentId');
        }
        
        // Pending changes store
        if (!db.objectStoreNames.contains('pending-changes')) {
          const changesStore = db.createObjectStore('pending-changes', {
            keyPath: 'id',
            autoIncrement: true
          });
          changesStore.createIndex('timestamp', 'timestamp');
        }
      };
    });
  }
  
  async saveDocument(document: Document) {
    const tx = this.db!.transaction(['documents'], 'readwrite');
    const store = tx.objectStore('documents');
    
    await store.put({
      ...document,
      _offline: true,
      _syncedAt: new Date()
    });
  }
  
  async saveChunk(documentId: string, chunkId: string, chunk: ContentChunk) {
    const tx = this.db!.transaction(['chunks'], 'readwrite');
    const store = tx.objectStore('chunks');
    
    await store.put({
      documentId,
      chunkId,
      chunk,
      _cachedAt: new Date()
    });
  }
  
  async queueChange(change: PendingChange) {
    const tx = this.db!.transaction(['pending-changes'], 'readwrite');
    const store = tx.objectStore('pending-changes');
    
    await store.add({
      ...change,
      timestamp: new Date()
    });
    
    // Request background sync
    if ('sync' in self.registration) {
      await self.registration.sync.register('sync-documents');
    }
  }
  
  async getOfflineDocuments(): Promise<Document[]> {
    const tx = this.db!.transaction(['documents'], 'readonly');
    const store = tx.objectStore('documents');
    const documents = await store.getAll();
    
    return documents.filter(doc => doc._offline);
  }
}

// React hook for offline functionality
export function useOfflineSync() {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [pendingChanges, setPendingChanges] = useState(0);
  const storageManager = useRef(new OfflineStorageManager());
  
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      // Trigger sync
      if ('sync' in self.registration) {
        self.registration.sync.register('sync-documents');
      }
    };
    
    const handleOffline = () => {
      setIsOnline(false);
    };
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    // Initialize storage
    storageManager.current.initialize();
    
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);
  
  const saveOffline = useCallback(async (data: any) => {
    await storageManager.current.queueChange(data);
    setPendingChanges(prev => prev + 1);
  }, []);
  
  return {
    isOnline,
    pendingChanges,
    saveOffline
  };
}
```

## Testing Strategy

### Component Testing

```typescript
// Component test example
describe('NovelEditor', () => {
  let store: MockStore;
  let mockApi: MockAdapter;
  
  beforeEach(() => {
    store = configureMockStore({
      editor: {
        document: mockDocument,
        selection: null,
        viewport: { scrollTop: 0, height: 800 }
      }
    });
    
    mockApi = new MockAdapter(axios);
  });
  
  it('should render document content', async () => {
    const { container } = render(
      <Provider store={store}>
        <NovelEditor />
      </Provider>
    );
    
    await waitFor(() => {
      expect(container.querySelector('.editor-viewport')).toBeInTheDocument();
    });
    
    expect(screen.getByText(mockDocument.title)).toBeInTheDocument();
  });
  
  it('should handle text selection', async () => {
    const { user } = render(
      <Provider store={store}>
        <NovelEditor />
      </Provider>
    );
    
    const paragraph = screen.getByText('First paragraph');
    
    // Simulate text selection
    await user.selectText(paragraph);
    
    expect(store.getActions()).toContainEqual(
      expect.objectContaining({
        type: 'editor/updateSelection',
        payload: expect.objectContaining({
          type: 'range'
        })
      })
    );
  });
  
  it('should apply formatting commands', async () => {
    const { user } = render(
      <Provider store={store}>
        <NovelEditor />
      </Provider>
    );
    
    // Select text
    await user.selectText(screen.getByText('Format me'));
    
    // Click bold button
    await user.click(screen.getByRole('button', { name: 'Bold' }));
    
    expect(mockApi.history.post).toHaveLength(1);
    expect(mockApi.history.post[0].data).toContain('bold');
  });
});

// Integration test
describe('Editor Integration', () => {
  it('should sync changes in real-time', async () => {
    const { container, rerender } = render(<App />);
    
    // Simulate WebSocket connection
    const ws = new WS('ws://localhost:3001');
    await ws.connected;
    
    // Make edit in one client
    const editor = screen.getByRole('textbox');
    await userEvent.type(editor, 'New text');
    
    // Verify operation sent
    expect(ws.messages).toContainEqual(
      expect.objectContaining({
        type: 'operation',
        data: expect.objectContaining({
          type: 'insert',
          text: 'New text'
        })
      })
    );
    
    // Simulate receiving update from another client
    ws.send({
      type: 'operation',
      data: {
        type: 'insert',
        position: 100,
        text: 'Other user text',
        userId: 'other-user'
      }
    });
    
    await waitFor(() => {
      expect(screen.getByText('Other user text')).toBeInTheDocument();
    });
  });
});
```

### Performance Testing

```typescript
// Performance test utilities
export async function measureRenderPerformance(
  component: React.ComponentType,
  props: any,
  iterations: number = 100
) {
  const measurements: number[] = [];
  
  for (let i = 0; i < iterations; i++) {
    const start = performance.now();
    
    const { unmount } = render(
      <Profiler id="test" onRender={() => {}}>
        <Component {...props} />
      </Profiler>
    );
    
    await waitForComponentToPaint(1);
    
    const end = performance.now();
    measurements.push(end - start);
    
    unmount();
  }
  
  return {
    average: measurements.reduce((a, b) => a + b) / measurements.length,
    median: measurements.sort()[Math.floor(measurements.length / 2)],
    p95: measurements.sort()[Math.floor(measurements.length * 0.95)],
    p99: measurements.sort()[Math.floor(measurements.length * 0.99)]
  };
}

// Load test for virtual scrolling
describe('Virtual Scrolling Performance', () => {
  it('should handle 500k word document efficiently', async () => {
    const largeDocument = generateLargeDocument(500000);
    
    const results = await measureRenderPerformance(
      NovelEditor,
      { document: largeDocument },
      10
    );
    
    expect(results.p95).toBeLessThan(100); // 100ms render time
    
    // Test scrolling performance
    const { container } = render(<NovelEditor document={largeDocument} />);
    const viewport = container.querySelector('.editor-viewport');
    
    const scrollMeasurements: number[] = [];
    
    for (let i = 0; i < 100; i++) {
      const start = performance.now();
      
      fireEvent.scroll(viewport, {
        target: { scrollTop: i * 1000 }
      });
      
      await waitForComponentToPaint(1);
      
      scrollMeasurements.push(performance.now() - start);
    }
    
    const avgScrollTime = scrollMeasurements.reduce((a, b) => a + b) / scrollMeasurements.length;
    expect(avgScrollTime).toBeLessThan(16); // 60fps
  });
});
```

## Build Configuration

### Vite Configuration

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  base: '/novel-creator/',
  
  plugins: [
    react({
      babel: {
        plugins: [
          ['@babel/plugin-transform-react-jsx', { runtime: 'automatic' }]
        ]
      }
    }),
    
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'robots.txt', 'apple-touch-icon.png'],
      manifest: {
        name: 'Novel Creator',
        short_name: 'NovelAI',
        theme_color: '#1a1a1a',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait',
        scope: '/novel-creator/',
        start_url: '/novel-creator/',
        icons: [
          {
            src: 'icon-192.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: 'icon-512.png',
            sizes: '512x512',
            type: 'image/png'
          }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/api\.novel-creator\.com\/api\//,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60 * 24 // 24 hours
              }
            }
          }
        ]
      }
    }),
    
    visualizer({
      template: 'treemap',
      open: true,
      gzipSize: true,
      brotliSize: true
    })
  ],
  
  optimizeDeps: {
    include: ['react', 'react-dom', '@reduxjs/toolkit', 'react-redux']
  },
  
  build: {
    target: 'es2020',
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'redux-vendor': ['@reduxjs/toolkit', 'react-redux'],
          'editor': ['./src/features/editor/index.ts'],
          'collaboration': ['./src/features/collaboration/index.ts']
        }
      }
    },
    reportCompressedSize: true,
    chunkSizeWarningLimit: 1000
  },
  
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:3001',
        ws: true
      }
    }
  }
});
```

## Performance Requirements

| Metric | Target | Measurement |
|--------|--------|-------------|
| Initial Load Time | < 3s | Lighthouse FCP |
| Time to Interactive | < 5s | Lighthouse TTI |
| Document Open | < 2s | Custom metric |
| Typing Latency | < 50ms | Input to render |
| Scroll Performance | 60fps | Frame timing |
| Memory Usage | < 200MB | Chrome DevTools |
| Bundle Size | < 500KB | Gzipped |
| Offline Capability | 100% | Service Worker |

## Deployment

### Production Build

```bash
# Build script
pnpm build:novel-creator

# Output structure
dist/
├── assets/
│   ├── app.[hash].js
│   ├── app.[hash].css
│   └── vendor.[hash].js
├── index.html
├── manifest.json
├── service-worker.js
└── icons/
```

## Conclusion

The Novel Creator Frontend Architecture provides a robust foundation for building a professional-grade novel writing application. With virtual scrolling for performance, real-time collaboration, comprehensive offline support, and a modular architecture, it delivers an exceptional user experience while maintaining code quality and developer productivity. The use of modern React patterns, TypeScript, and performance optimizations ensures the application can scale to handle large documents and many concurrent users.