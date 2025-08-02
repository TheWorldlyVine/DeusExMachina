# Technical Specification: Export Service

## Overview

The Export Service provides professional-grade document export capabilities for completed novels, supporting multiple industry-standard formats including EPUB 3.3, PDF, DOCX, and HTML. Built as a scalable microservice architecture with containerized format-specific exporters, it handles asynchronous processing through Cloud Tasks with temporary storage in Cloud Storage for efficient delivery.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Export Service                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────────┐   │
│  │  Export API    │  │  Task Queue    │  │  Status Tracker   │   │
│  │   (REST)       │  │   Manager      │  │    (Real-time)    │   │
│  └────────┬───────┘  └────────┬───────┘  └─────────┬─────────┘   │
│           │                    │                      │             │
│  ┌────────▼───────────────────▼──────────────────────▼─────────┐  │
│  │                   Export Orchestrator                         │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │  Validator  │  │  Processor   │  │   Formatter       │ │  │
│  │  │   Engine    │  │   Engine     │  │    Engine         │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────┬──────────────────────────────────┘  │
│                             │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐  │
│  │                   Format Exporters                           │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │  │
│  │  │    EPUB     │  │     PDF      │  │      DOCX         │ │  │
│  │  │  Exporter   │  │   Exporter   │  │    Exporter      │ │  │
│  │  └─────────────┘  └──────────────┘  └───────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Export Pipeline

```
Document → Validation → Processing → Formatting → Export → Storage → Delivery
   │           │            │            │          │         │          │
   └───────────┴────────────┴────────────┴──────────┴─────────┴──────────┘
                                Error Handling & Retry Logic
```

## Data Models

### Export Schema

```typescript
// Export Request
interface ExportRequest {
  id: string;
  documentId: string;
  projectId: string;
  userId: string;
  format: ExportFormat;
  options: ExportOptions;
  priority: 'high' | 'normal' | 'low';
  createdAt: Date;
  status: ExportStatus;
  metadata: ExportMetadata;
}

type ExportFormat = 'epub' | 'pdf' | 'docx' | 'html' | 'markdown' | 'txt';

interface ExportOptions {
  // Common options
  includeMetadata: boolean;
  includeCoverImage: boolean;
  includeTableOfContents: boolean;
  
  // Format-specific options
  epub?: EPUBOptions;
  pdf?: PDFOptions;
  docx?: DOCXOptions;
  html?: HTMLOptions;
}

interface EPUBOptions {
  version: '3.3' | '3.2' | '2.0';
  layout: 'reflowable' | 'fixed';
  coverImage?: {
    url: string;
    width: number;
    height: number;
  };
  metadata: {
    title: string;
    author: string;
    publisher?: string;
    isbn?: string;
    language: string;
    rights?: string;
    description?: string;
    subjects?: string[];
  };
  styling: {
    fontFamily?: string;
    fontSize?: string;
    lineHeight?: number;
    marginSize?: 'small' | 'medium' | 'large';
    theme?: 'default' | 'night' | 'sepia';
  };
  accessibility: {
    includeAltText: boolean;
    includeSemantic: boolean;
    includeARIA: boolean;
  };
}

interface PDFOptions {
  pageSize: 'A4' | 'Letter' | 'A5' | 'B5' | '6x9' | '5.5x8.5';
  orientation: 'portrait' | 'landscape';
  margins: {
    top: number;
    bottom: number;
    left: number;
    right: number;
  };
  quality: 'draft' | 'standard' | 'print' | 'professional';
  dpi: 72 | 150 | 300 | 600;
  colorSpace: 'RGB' | 'CMYK' | 'Grayscale';
  fonts: {
    body: string;
    heading: string;
    special?: string;
  };
  headers: {
    enabled: boolean;
    left?: string;
    center?: string;
    right?: string;
  };
  footers: {
    enabled: boolean;
    includePageNumbers: boolean;
    format?: string;
  };
  security?: {
    userPassword?: string;
    ownerPassword?: string;
    permissions: PDFPermissions;
  };
}

interface PDFPermissions {
  printing: 'none' | 'low' | 'high';
  modifying: boolean;
  copying: boolean;
  annotating: boolean;
  filling: boolean;
  extraction: boolean;
  assembly: boolean;
}

interface DOCXOptions {
  template?: 'manuscript' | 'book' | 'report' | 'custom';
  formatting: {
    fontFamily: string;
    fontSize: number;
    lineSpacing: number;
    paragraphSpacing: number;
    firstLineIndent: boolean;
  };
  headers: {
    author?: string;
    title?: string;
    includePageNumbers: boolean;
  };
  styles: {
    heading1: StyleDefinition;
    heading2: StyleDefinition;
    heading3: StyleDefinition;
    body: StyleDefinition;
    quote: StyleDefinition;
  };
  trackChanges: boolean;
  comments: 'include' | 'remove' | 'resolve';
}

interface HTMLOptions {
  template: 'single' | 'multi' | 'website';
  includeCSS: 'inline' | 'external' | 'none';
  includeJS: boolean;
  responsiveImages: boolean;
  minify: boolean;
  seoOptimized: boolean;
  openGraph: {
    title: string;
    description: string;
    image?: string;
    type: 'book' | 'article';
  };
}

// Export Status
interface ExportStatus {
  state: 'pending' | 'processing' | 'completed' | 'failed' | 'cancelled';
  progress: number; // 0-100
  currentStep?: string;
  estimatedCompletion?: Date;
  error?: ExportError;
  attempts: number;
}

interface ExportError {
  code: string;
  message: string;
  details?: any;
  recoverable: boolean;
}

// Export Result
interface ExportResult {
  id: string;
  requestId: string;
  format: ExportFormat;
  fileUrl: string;
  fileSize: number;
  checksum: string;
  expiresAt: Date;
  metadata: {
    pageCount?: number;
    wordCount: number;
    characterCount: number;
    generatedAt: Date;
    generator: string;
    version: string;
  };
  validation: ValidationResult;
}

interface ValidationResult {
  valid: boolean;
  warnings: ValidationWarning[];
  errors: ValidationError[];
  formatCompliance: {
    standard: string;
    version: string;
    compliance: 'full' | 'partial' | 'minimal';
  };
}

interface ValidationWarning {
  type: string;
  message: string;
  location?: string;
  suggestion?: string;
}

interface ValidationError {
  type: string;
  message: string;
  location?: string;
  fatal: boolean;
}

// Task Management
interface ExportTask {
  id: string;
  requestId: string;
  type: 'validate' | 'process' | 'format' | 'generate' | 'upload';
  status: 'pending' | 'running' | 'completed' | 'failed';
  input: any;
  output?: any;
  startedAt?: Date;
  completedAt?: Date;
  workerId?: string;
  retries: number;
  maxRetries: number;
}

// Template System
interface ExportTemplate {
  id: string;
  name: string;
  format: ExportFormat;
  category: 'fiction' | 'non-fiction' | 'academic' | 'business';
  description: string;
  thumbnail?: string;
  options: ExportOptions;
  styling: StyleConfiguration;
  metadata: {
    creator: string;
    created: Date;
    updated: Date;
    downloads: number;
    rating: number;
  };
}

interface StyleConfiguration {
  typography: {
    fontStack: string[];
    baseFontSize: number;
    lineHeight: number;
    paragraphSpacing: number;
  };
  colors: {
    text: string;
    background: string;
    accent: string;
    links: string;
  };
  spacing: {
    margins: SpacingValues;
    padding: SpacingValues;
    chapterBreaks: number;
  };
  components: {
    headings: HeadingStyles;
    quotes: QuoteStyles;
    lists: ListStyles;
  };
}
```

## API Design

### REST API Endpoints

```yaml
# Export Creation
POST /api/v1/export
Request: CreateExportRequest
Response: ExportRequest
Description: Initiates a new export job

# Export Status
GET /api/v1/export/{exportId}
Response: ExportRequest with current status
Description: Gets the current status of an export job

# Export Result
GET /api/v1/export/{exportId}/download
Response: Redirect to signed download URL
Description: Gets the download URL for completed export

# Cancel Export
DELETE /api/v1/export/{exportId}
Response: 204 No Content
Description: Cancels a pending or running export

# Export History
GET /api/v1/export/history
Query:
  - userId: string
  - documentId: string
  - format: ExportFormat
  - status: ExportStatus
  - limit: number
  - offset: number
Response: ExportRequest[]

# Templates
GET /api/v1/export/templates
Query:
  - format: ExportFormat
  - category: string
Response: ExportTemplate[]

GET /api/v1/export/templates/{templateId}
Response: ExportTemplate

# Validation
POST /api/v1/export/validate
Request: ValidationRequest
Response: ValidationResult
Description: Pre-validates export options without creating export

# Format Capabilities
GET /api/v1/export/formats
Response: FormatCapabilities[]
Description: Lists supported formats and their options

# Batch Export
POST /api/v1/export/batch
Request: BatchExportRequest
Response: BatchExportResponse
Description: Creates multiple exports in different formats
```

### GraphQL Schema

```graphql
type Query {
  # Export queries
  export(id: ID!): Export
  exports(
    userId: ID
    documentId: ID
    format: ExportFormat
    status: ExportStatus
    limit: Int = 20
    offset: Int = 0
  ): ExportPage!
  
  # Template queries
  exportTemplates(
    format: ExportFormat
    category: String
  ): [ExportTemplate!]!
  
  exportTemplate(id: ID!): ExportTemplate
  
  # Format information
  exportFormats: [FormatInfo!]!
  formatCapabilities(format: ExportFormat!): FormatCapabilities!
  
  # Validation
  validateExportOptions(
    format: ExportFormat!
    options: ExportOptionsInput!
  ): ValidationResult!
}

type Mutation {
  # Export operations
  createExport(
    documentId: ID!
    format: ExportFormat!
    options: ExportOptionsInput
    templateId: ID
  ): Export!
  
  cancelExport(id: ID!): Boolean!
  
  retryExport(id: ID!): Export!
  
  # Batch operations
  createBatchExport(
    documentId: ID!
    formats: [ExportFormat!]!
    options: BatchExportOptionsInput
  ): BatchExport!
  
  # Template management
  createExportTemplate(
    input: CreateTemplateInput!
  ): ExportTemplate!
  
  updateExportTemplate(
    id: ID!
    input: UpdateTemplateInput!
  ): ExportTemplate!
  
  deleteExportTemplate(id: ID!): Boolean!
}

type Subscription {
  # Real-time export updates
  exportProgress(id: ID!): ExportProgress!
  exportComplete(userId: ID!): Export!
  batchExportProgress(id: ID!): BatchExportProgress!
}

type Export {
  id: ID!
  documentId: ID!
  format: ExportFormat!
  status: ExportStatus!
  progress: Float!
  currentStep: String
  options: ExportOptions!
  result: ExportResult
  createdAt: DateTime!
  completedAt: DateTime
  estimatedCompletion: DateTime
  error: ExportError
}

type ExportResult {
  downloadUrl: String!
  fileSize: Int!
  expiresAt: DateTime!
  metadata: ExportMetadata!
  validation: ValidationResult!
}

enum ExportFormat {
  EPUB
  PDF
  DOCX
  HTML
  MARKDOWN
  TXT
}

enum ExportStatus {
  PENDING
  PROCESSING
  COMPLETED
  FAILED
  CANCELLED
}
```

## Implementation Details

### Export Orchestrator

```typescript
@Injectable()
export class ExportOrchestrator {
  constructor(
    private taskQueue: CloudTasksClient,
    private storage: Storage,
    private firestore: Firestore,
    private exporters: Map<ExportFormat, IExporter>
  ) {}

  async createExport(
    request: CreateExportRequest,
    userId: string
  ): Promise<ExportRequest> {
    // Validate request
    const validation = await this.validateRequest(request);
    if (!validation.valid) {
      throw new ValidationException(validation.errors);
    }
    
    // Create export record
    const exportRequest: ExportRequest = {
      id: uuidv4(),
      documentId: request.documentId,
      projectId: request.projectId,
      userId,
      format: request.format,
      options: this.mergeWithDefaults(request.format, request.options),
      priority: request.priority || 'normal',
      createdAt: new Date(),
      status: {
        state: 'pending',
        progress: 0,
        attempts: 0
      },
      metadata: {
        requestSource: request.source || 'web',
        clientInfo: request.clientInfo
      }
    };
    
    // Save to Firestore
    await this.firestore
      .collection('exports')
      .doc(exportRequest.id)
      .set(exportRequest);
    
    // Queue processing task
    await this.queueExportTask(exportRequest);
    
    return exportRequest;
  }

  private async queueExportTask(
    exportRequest: ExportRequest
  ): Promise<void> {
    const task = {
      httpRequest: {
        httpMethod: 'POST' as const,
        url: `${process.env.EXPORT_WORKER_URL}/process`,
        headers: {
          'Content-Type': 'application/json',
          'X-Export-Priority': exportRequest.priority
        },
        body: Buffer.from(JSON.stringify({
          exportId: exportRequest.id,
          format: exportRequest.format
        })).toString('base64')
      }
    };
    
    // Set task options based on priority
    const queuePath = this.taskQueue.queuePath(
      process.env.GCP_PROJECT_ID!,
      process.env.GCP_LOCATION!,
      this.getQueueName(exportRequest.priority)
    );
    
    await this.taskQueue.createTask({
      parent: queuePath,
      task: {
        ...task,
        scheduleTime: this.getScheduleTime(exportRequest.priority)
      }
    });
  }

  async processExport(exportId: string): Promise<void> {
    const exportRequest = await this.getExportRequest(exportId);
    
    try {
      // Update status
      await this.updateStatus(exportId, {
        state: 'processing',
        progress: 0,
        currentStep: 'Loading document'
      });
      
      // Load document
      const document = await this.loadDocument(exportRequest.documentId);
      await this.updateProgress(exportId, 10);
      
      // Validate document
      await this.updateStatus(exportId, {
        currentStep: 'Validating content'
      });
      const validationResult = await this.validateDocument(document, exportRequest.format);
      await this.updateProgress(exportId, 20);
      
      // Process content
      await this.updateStatus(exportId, {
        currentStep: 'Processing content'
      });
      const processedContent = await this.processContent(document, exportRequest.options);
      await this.updateProgress(exportId, 40);
      
      // Format document
      await this.updateStatus(exportId, {
        currentStep: 'Formatting document'
      });
      const formattedContent = await this.formatContent(
        processedContent,
        exportRequest.format,
        exportRequest.options
      );
      await this.updateProgress(exportId, 60);
      
      // Generate export
      await this.updateStatus(exportId, {
        currentStep: 'Generating export file'
      });
      const exporter = this.exporters.get(exportRequest.format);
      if (!exporter) {
        throw new Error(`No exporter found for format: ${exportRequest.format}`);
      }
      
      const exportFile = await exporter.export(formattedContent, exportRequest.options);
      await this.updateProgress(exportId, 80);
      
      // Upload to storage
      await this.updateStatus(exportId, {
        currentStep: 'Uploading file'
      });
      const fileUrl = await this.uploadExportFile(exportId, exportFile);
      await this.updateProgress(exportId, 95);
      
      // Create result
      const result: ExportResult = {
        id: uuidv4(),
        requestId: exportId,
        format: exportRequest.format,
        fileUrl,
        fileSize: exportFile.size,
        checksum: await this.calculateChecksum(exportFile),
        expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000), // 24 hours
        metadata: {
          pageCount: exportFile.pageCount,
          wordCount: document.stats.totalWords,
          characterCount: document.stats.totalCharacters,
          generatedAt: new Date(),
          generator: 'NovelCreator Export Service',
          version: process.env.EXPORT_SERVICE_VERSION!
        },
        validation: validationResult
      };
      
      // Update status to completed
      await this.updateStatus(exportId, {
        state: 'completed',
        progress: 100,
        currentStep: 'Export completed'
      });
      
      // Save result
      await this.saveExportResult(exportId, result);
      
      // Send notification
      await this.notifyCompletion(exportRequest.userId, exportId, result);
      
    } catch (error) {
      await this.handleExportError(exportId, error);
    }
  }

  private async validateDocument(
    document: Document,
    format: ExportFormat
  ): Promise<ValidationResult> {
    const validator = new DocumentValidator(format);
    const errors: ValidationError[] = [];
    const warnings: ValidationWarning[] = [];
    
    // Check document structure
    if (!document.structure || document.structure.totalScenes === 0) {
      errors.push({
        type: 'EMPTY_DOCUMENT',
        message: 'Document has no content',
        fatal: true
      });
    }
    
    // Check word count limits
    if (format === 'epub' && document.stats.totalWords > 1000000) {
      warnings.push({
        type: 'LARGE_DOCUMENT',
        message: 'Document exceeds recommended EPUB size',
        suggestion: 'Consider splitting into multiple volumes'
      });
    }
    
    // Format-specific validation
    switch (format) {
      case 'epub':
        this.validateEPUB(document, errors, warnings);
        break;
      case 'pdf':
        this.validatePDF(document, errors, warnings);
        break;
      case 'docx':
        this.validateDOCX(document, errors, warnings);
        break;
    }
    
    return {
      valid: errors.filter(e => e.fatal).length === 0,
      errors,
      warnings,
      formatCompliance: {
        standard: this.getFormatStandard(format),
        version: this.getFormatVersion(format),
        compliance: this.calculateCompliance(errors, warnings)
      }
    };
  }
}
```

### Format-Specific Exporters

#### EPUB Exporter

```typescript
@Injectable()
export class EPUBExporter implements IExporter {
  async export(
    content: FormattedContent,
    options: EPUBOptions
  ): Promise<ExportFile> {
    const epub = new EPUBGenerator({
      version: options.version || '3.3',
      layout: options.layout || 'reflowable'
    });
    
    // Add metadata
    epub.addMetadata({
      title: options.metadata.title,
      author: options.metadata.author,
      publisher: options.metadata.publisher,
      language: options.metadata.language || 'en',
      isbn: options.metadata.isbn,
      rights: options.metadata.rights,
      description: options.metadata.description,
      subjects: options.metadata.subjects || [],
      modified: new Date().toISOString()
    });
    
    // Add cover image
    if (options.includeCoverImage && options.coverImage) {
      await epub.addCover(options.coverImage);
    }
    
    // Add stylesheet
    const css = this.generateStylesheet(options.styling);
    epub.addCSS('styles.css', css);
    
    // Add navigation
    if (options.includeTableOfContents) {
      epub.addNavigation(this.generateNavigation(content));
    }
    
    // Add content chapters
    for (const chapter of content.chapters) {
      const chapterHtml = this.renderChapter(chapter, options);
      epub.addChapter({
        id: chapter.id,
        title: chapter.title,
        content: chapterHtml,
        level: chapter.level || 1
      });
    }
    
    // Add accessibility features
    if (options.accessibility.includeSemantic) {
      epub.addAccessibilityMetadata({
        accessMode: ['textual', 'visual'],
        accessibilityFeature: [
          'structuralNavigation',
          'tableOfContents',
          'readingOrder'
        ],
        accessibilityHazard: ['none'],
        accessibilitySummary: 'This publication conforms to WCAG 2.1 Level AA.'
      });
    }
    
    // Generate EPUB file
    const epubBuffer = await epub.generate();
    
    // Validate generated EPUB
    const validation = await this.validateEPUB(epubBuffer);
    if (!validation.valid) {
      throw new Error(`EPUB validation failed: ${validation.errors.join(', ')}`);
    }
    
    return {
      buffer: epubBuffer,
      size: epubBuffer.length,
      mimeType: 'application/epub+zip',
      extension: 'epub',
      pageCount: content.chapters.length
    };
  }

  private generateStylesheet(styling: EPUBOptions['styling']): string {
    return `
      @namespace epub "http://www.idpf.org/2007/ops";
      
      body {
        font-family: ${styling.fontFamily || 'Georgia, serif'};
        font-size: ${styling.fontSize || '1em'};
        line-height: ${styling.lineHeight || 1.6};
        margin: ${this.getMarginSize(styling.marginSize || 'medium')};
        text-align: justify;
        hyphens: auto;
      }
      
      h1, h2, h3, h4, h5, h6 {
        font-family: 'Helvetica Neue', Arial, sans-serif;
        font-weight: bold;
        page-break-after: avoid;
      }
      
      h1 {
        font-size: 2em;
        margin-top: 2em;
        margin-bottom: 1em;
        text-align: center;
      }
      
      h2 {
        font-size: 1.5em;
        margin-top: 1.5em;
        margin-bottom: 0.75em;
      }
      
      p {
        margin: 0;
        text-indent: 1.5em;
      }
      
      p:first-of-type,
      h1 + p,
      h2 + p,
      h3 + p {
        text-indent: 0;
      }
      
      .chapter-break {
        page-break-before: always;
      }
      
      .scene-break {
        text-align: center;
        margin: 2em 0;
      }
      
      .scene-break::before {
        content: "* * *";
      }
      
      blockquote {
        margin: 1em 2em;
        font-style: italic;
      }
      
      /* Accessibility */
      .visually-hidden {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0,0,0,0);
        white-space: nowrap;
        border: 0;
      }
    `;
  }

  private renderChapter(
    chapter: Chapter,
    options: EPUBOptions
  ): string {
    let html = `
      <section epub:type="chapter" class="chapter-break">
        <h1>${this.escapeHtml(chapter.title)}</h1>
    `;
    
    for (const scene of chapter.scenes) {
      if (scene.isSceneBreak) {
        html += '<div class="scene-break" aria-label="Scene break"></div>';
      }
      
      html += '<section epub:type="scene">';
      
      for (const paragraph of scene.paragraphs) {
        html += this.renderParagraph(paragraph, options);
      }
      
      html += '</section>';
    }
    
    html += '</section>';
    
    return html;
  }

  private async validateEPUB(epubBuffer: Buffer): Promise<ValidationResult> {
    // Use EPUBCheck for validation
    const validator = new EPUBCheck();
    const result = await validator.validate(epubBuffer);
    
    return {
      valid: result.errors.length === 0,
      errors: result.errors.map(e => ({
        type: e.type,
        message: e.message,
        location: e.location,
        fatal: e.severity === 'ERROR'
      })),
      warnings: result.warnings.map(w => ({
        type: w.type,
        message: w.message,
        location: w.location
      })),
      formatCompliance: {
        standard: 'EPUB',
        version: '3.3',
        compliance: result.errors.length === 0 ? 'full' : 'partial'
      }
    };
  }
}
```

#### PDF Exporter

```typescript
@Injectable()
export class PDFExporter implements IExporter {
  async export(
    content: FormattedContent,
    options: PDFOptions
  ): Promise<ExportFile> {
    const pdf = new PDFDocument({
      size: options.pageSize || 'Letter',
      margins: options.margins || {
        top: 72,
        bottom: 72,
        left: 72,
        right: 72
      },
      info: {
        Title: content.metadata.title,
        Author: content.metadata.author,
        Subject: content.metadata.description,
        Keywords: content.metadata.keywords?.join(', '),
        Creator: 'NovelCreator Export Service',
        Producer: 'PDFKit',
        CreationDate: new Date()
      }
    });
    
    const chunks: Buffer[] = [];
    pdf.on('data', chunk => chunks.push(chunk));
    
    // Configure quality settings
    this.configureQuality(pdf, options.quality || 'standard');
    
    // Add fonts
    await this.loadFonts(pdf, options.fonts);
    
    // Add cover page
    if (options.includeCoverImage && content.coverImage) {
      this.addCoverPage(pdf, content.coverImage, content.metadata);
    }
    
    // Add table of contents
    if (options.includeTableOfContents) {
      this.addTableOfContents(pdf, content.chapters);
    }
    
    // Add content
    let pageNumber = 1;
    for (const chapter of content.chapters) {
      // Chapter title page
      pdf.addPage();
      this.addChapterTitle(pdf, chapter.title);
      
      // Chapter content
      for (const scene of chapter.scenes) {
        for (const paragraph of scene.paragraphs) {
          this.addParagraph(pdf, paragraph, options);
        }
        
        if (scene.isSceneBreak) {
          this.addSceneBreak(pdf);
        }
      }
      
      // Headers and footers
      if (options.headers.enabled || options.footers.enabled) {
        this.addHeadersFooters(pdf, options, pageNumber, chapter.title);
      }
      
      pageNumber = pdf.bufferedPageRange().count;
    }
    
    // Apply security if requested
    if (options.security) {
      this.applySecurity(pdf, options.security);
    }
    
    // Finalize PDF
    pdf.end();
    
    const pdfBuffer = Buffer.concat(chunks);
    
    // Post-process for print quality
    const finalBuffer = await this.postProcess(pdfBuffer, options);
    
    return {
      buffer: finalBuffer,
      size: finalBuffer.length,
      mimeType: 'application/pdf',
      extension: 'pdf',
      pageCount: pdf.bufferedPageRange().count
    };
  }

  private configureQuality(pdf: PDFDocument, quality: string): void {
    switch (quality) {
      case 'draft':
        pdf.compress = true;
        pdf.quality = 0.7;
        break;
      case 'standard':
        pdf.compress = true;
        pdf.quality = 0.85;
        break;
      case 'print':
        pdf.compress = false;
        pdf.quality = 0.95;
        break;
      case 'professional':
        pdf.compress = false;
        pdf.quality = 1.0;
        // Enable advanced features
        pdf.pdfVersion = '1.7';
        pdf.subset = false; // Embed full fonts
        break;
    }
  }

  private async postProcess(
    pdfBuffer: Buffer,
    options: PDFOptions
  ): Promise<Buffer> {
    if (options.quality === 'professional' && options.colorSpace === 'CMYK') {
      // Convert to CMYK for professional printing
      return this.convertToCMYK(pdfBuffer, options.dpi || 300);
    }
    
    if (options.dpi && options.dpi > 150) {
      // Optimize for high DPI printing
      return this.optimizeForPrint(pdfBuffer, options.dpi);
    }
    
    return pdfBuffer;
  }
}
```

#### DOCX Exporter

```typescript
@Injectable()
export class DOCXExporter implements IExporter {
  async export(
    content: FormattedContent,
    options: DOCXOptions
  ): Promise<ExportFile> {
    const doc = new Document({
      creator: content.metadata.author,
      title: content.metadata.title,
      description: content.metadata.description,
      styles: this.createStyles(options),
      numbering: this.createNumbering(options),
      sections: []
    });
    
    // Add cover page if template requires
    if (options.template && this.requiresCoverPage(options.template)) {
      doc.addSection({
        properties: {},
        children: this.createCoverPage(content.metadata, options)
      });
    }
    
    // Add main content
    const sections = [];
    
    for (const chapter of content.chapters) {
      const section = {
        properties: {
          type: SectionType.NEXT_PAGE,
          page: {
            margin: this.getMargins(options.template),
            size: this.getPageSize(options.template)
          }
        },
        headers: {
          default: this.createHeader(chapter.title, options)
        },
        footers: {
          default: this.createFooter(options)
        },
        children: []
      };
      
      // Chapter title
      section.children.push(
        new Paragraph({
          text: chapter.title,
          heading: HeadingLevel.HEADING_1,
          alignment: AlignmentType.CENTER,
          spacing: {
            after: 400
          }
        })
      );
      
      // Chapter content
      for (const scene of chapter.scenes) {
        for (const paragraph of scene.paragraphs) {
          section.children.push(
            this.createParagraph(paragraph, options)
          );
        }
        
        if (scene.isSceneBreak) {
          section.children.push(this.createSceneBreak());
        }
      }
      
      sections.push(section);
    }
    
    doc.addSection(...sections);
    
    // Generate DOCX
    const buffer = await Packer.toBuffer(doc);
    
    // Apply post-processing
    const finalBuffer = await this.postProcessDOCX(buffer, options);
    
    return {
      buffer: finalBuffer,
      size: finalBuffer.length,
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      extension: 'docx',
      pageCount: await this.estimatePageCount(content, options)
    };
  }

  private createStyles(options: DOCXOptions): IStylesOptions {
    return {
      default: {
        heading1: {
          run: {
            font: options.styles.heading1.font || 'Calibri',
            size: options.styles.heading1.size || 32,
            bold: true,
            color: options.styles.heading1.color || '000000'
          },
          paragraph: {
            spacing: {
              before: 240,
              after: 120
            },
            alignment: AlignmentType.CENTER
          }
        },
        heading2: {
          run: {
            font: options.styles.heading2.font || 'Calibri',
            size: options.styles.heading2.size || 28,
            bold: true,
            color: options.styles.heading2.color || '000000'
          },
          paragraph: {
            spacing: {
              before: 200,
              after: 100
            }
          }
        },
        document: {
          run: {
            font: options.formatting.fontFamily || 'Times New Roman',
            size: options.formatting.fontSize * 2 || 24, // Half-points
          },
          paragraph: {
            spacing: {
              line: options.formatting.lineSpacing * 240 || 360,
              before: 0,
              after: options.formatting.paragraphSpacing * 20 || 0
            },
            indent: {
              firstLine: options.formatting.firstLineIndent ? 720 : 0 // 0.5 inch
            }
          }
        }
      },
      paragraphStyles: [
        {
          id: 'SceneBreak',
          name: 'Scene Break',
          paragraph: {
            alignment: AlignmentType.CENTER,
            spacing: {
              before: 480,
              after: 480
            }
          }
        }
      ]
    };
  }

  private async postProcessDOCX(
    buffer: Buffer,
    options: DOCXOptions
  ): Promise<Buffer> {
    // Handle comments
    if (options.comments === 'remove') {
      return this.removeComments(buffer);
    } else if (options.comments === 'resolve') {
      return this.resolveComments(buffer);
    }
    
    // Add track changes metadata
    if (options.trackChanges) {
      return this.enableTrackChanges(buffer);
    }
    
    return buffer;
  }
}
```

### Export Queue Management

```typescript
@Injectable()
export class ExportQueueManager {
  private queues: Map<string, Queue> = new Map();
  
  constructor(
    private redis: Redis,
    private metrics: MetricsService
  ) {
    this.initializeQueues();
  }
  
  private initializeQueues(): void {
    // Priority queues
    this.queues.set('high', new Queue('export-high', {
      redis: this.redis,
      defaultJobOptions: {
        removeOnComplete: 100,
        removeOnFail: 500,
        attempts: 3,
        backoff: {
          type: 'exponential',
          delay: 2000
        }
      }
    }));
    
    this.queues.set('normal', new Queue('export-normal', {
      redis: this.redis,
      defaultJobOptions: {
        removeOnComplete: 50,
        removeOnFail: 200,
        attempts: 3,
        backoff: {
          type: 'exponential',
          delay: 5000
        }
      }
    }));
    
    this.queues.set('low', new Queue('export-low', {
      redis: this.redis,
      defaultJobOptions: {
        removeOnComplete: 20,
        removeOnFail: 100,
        attempts: 2,
        backoff: {
          type: 'fixed',
          delay: 10000
        }
      }
    }));
    
    // Set up queue monitoring
    this.setupQueueMonitoring();
  }
  
  async addExportJob(
    exportRequest: ExportRequest
  ): Promise<Job> {
    const queue = this.queues.get(exportRequest.priority);
    if (!queue) {
      throw new Error(`Invalid priority: ${exportRequest.priority}`);
    }
    
    const job = await queue.add('export', {
      exportId: exportRequest.id,
      format: exportRequest.format,
      attempt: 1
    }, {
      priority: this.getPriorityValue(exportRequest.priority),
      delay: this.calculateDelay(exportRequest)
    });
    
    // Track metrics
    await this.metrics.incrementCounter('export.jobs.queued', {
      format: exportRequest.format,
      priority: exportRequest.priority
    });
    
    return job;
  }
  
  private setupQueueMonitoring(): void {
    for (const [priority, queue] of this.queues) {
      // Job completed
      queue.on('completed', async (job, result) => {
        await this.metrics.incrementCounter('export.jobs.completed', {
          priority,
          format: job.data.format
        });
        
        await this.metrics.recordHistogram('export.job.duration', 
          job.finishedOn! - job.timestamp, {
            priority,
            format: job.data.format
          }
        );
      });
      
      // Job failed
      queue.on('failed', async (job, err) => {
        await this.metrics.incrementCounter('export.jobs.failed', {
          priority,
          format: job.data.format,
          error: err.name
        });
        
        // Check if should retry
        if (job.attemptsMade < job.opts.attempts!) {
          console.log(`Retrying job ${job.id}, attempt ${job.attemptsMade + 1}`);
        } else {
          // Final failure
          await this.handleFinalFailure(job, err);
        }
      });
      
      // Queue health metrics
      setInterval(async () => {
        const counts = await queue.getJobCounts();
        
        await this.metrics.recordGauge('export.queue.size', counts.waiting, {
          priority,
          state: 'waiting'
        });
        
        await this.metrics.recordGauge('export.queue.size', counts.active, {
          priority,
          state: 'active'
        });
        
        await this.metrics.recordGauge('export.queue.size', counts.delayed, {
          priority,
          state: 'delayed'
        });
      }, 30000); // Every 30 seconds
    }
  }
}
```

### Export Storage

```typescript
@Injectable()
export class ExportStorageService {
  private bucket: Bucket;
  
  constructor(private storage: Storage) {
    this.bucket = storage.bucket(process.env.EXPORT_BUCKET!);
  }
  
  async uploadExportFile(
    exportId: string,
    file: ExportFile
  ): Promise<string> {
    const filename = `exports/${exportId}/${exportId}.${file.extension}`;
    const gcsFile = this.bucket.file(filename);
    
    // Set metadata
    const metadata = {
      contentType: file.mimeType,
      metadata: {
        exportId,
        format: file.extension,
        size: file.size.toString(),
        pageCount: file.pageCount?.toString(),
        generated: new Date().toISOString()
      },
      cacheControl: 'private, max-age=86400' // 24 hours
    };
    
    // Upload with resumable upload for large files
    if (file.size > 5 * 1024 * 1024) { // 5MB
      await this.uploadResumable(gcsFile, file.buffer, metadata);
    } else {
      await gcsFile.save(file.buffer, metadata);
    }
    
    // Generate signed URL for download
    const [signedUrl] = await gcsFile.getSignedUrl({
      version: 'v4',
      action: 'read',
      expires: Date.now() + 24 * 60 * 60 * 1000, // 24 hours
      responseDisposition: `attachment; filename="${this.getSafeFilename(file)}"`,
      responseType: file.mimeType
    });
    
    return signedUrl;
  }
  
  private async uploadResumable(
    file: File,
    buffer: Buffer,
    metadata: any
  ): Promise<void> {
    const stream = file.createWriteStream({
      resumable: true,
      metadata,
      validation: 'crc32c'
    });
    
    return new Promise((resolve, reject) => {
      stream.on('error', reject);
      stream.on('finish', resolve);
      
      // Upload in chunks
      const chunkSize = 256 * 1024; // 256KB chunks
      let offset = 0;
      
      while (offset < buffer.length) {
        const chunk = buffer.slice(offset, offset + chunkSize);
        stream.write(chunk);
        offset += chunkSize;
      }
      
      stream.end();
    });
  }
  
  private getSafeFilename(file: ExportFile): string {
    // Sanitize filename for different operating systems
    const base = `novel_export_${Date.now()}`;
    const safe = base.replace(/[^a-zA-Z0-9_-]/g, '_');
    return `${safe}.${file.extension}`;
  }
  
  // Cleanup old exports
  @Cron('0 0 * * *') // Daily at midnight
  async cleanupExpiredExports(): Promise<void> {
    const [files] = await this.bucket.getFiles({
      prefix: 'exports/',
      maxResults: 1000
    });
    
    const now = Date.now();
    const expiryTime = 24 * 60 * 60 * 1000; // 24 hours
    
    for (const file of files) {
      const metadata = file.metadata;
      if (metadata.timeCreated) {
        const created = new Date(metadata.timeCreated).getTime();
        if (now - created > expiryTime) {
          await file.delete();
          console.log(`Deleted expired export: ${file.name}`);
        }
      }
    }
  }
}
```

## Performance Optimization

### Caching Strategy

```typescript
@Injectable()
export class ExportCacheService {
  constructor(
    private redis: Redis,
    private storage: Storage
  ) {}
  
  async getCachedExport(
    documentId: string,
    format: ExportFormat,
    optionsHash: string
  ): Promise<string | null> {
    const key = `export:cache:${documentId}:${format}:${optionsHash}`;
    const cached = await this.redis.get(key);
    
    if (cached) {
      // Verify file still exists
      const bucket = this.storage.bucket(process.env.EXPORT_BUCKET!);
      const file = bucket.file(cached);
      const [exists] = await file.exists();
      
      if (exists) {
        // Extend cache TTL
        await this.redis.expire(key, 3600); // 1 hour
        
        // Generate new signed URL
        const [signedUrl] = await file.getSignedUrl({
          version: 'v4',
          action: 'read',
          expires: Date.now() + 3600000 // 1 hour
        });
        
        return signedUrl;
      }
    }
    
    return null;
  }
  
  async cacheExport(
    documentId: string,
    format: ExportFormat,
    optionsHash: string,
    filePath: string
  ): Promise<void> {
    const key = `export:cache:${documentId}:${format}:${optionsHash}`;
    await this.redis.setex(key, 3600, filePath); // 1 hour cache
  }
}
```

### Worker Pool Management

```typescript
@Injectable()
export class ExportWorkerPool {
  private workers: Worker[] = [];
  private availableWorkers: Worker[] = [];
  private readonly MAX_WORKERS = parseInt(process.env.MAX_EXPORT_WORKERS || '4');
  
  constructor() {
    this.initializeWorkers();
  }
  
  private initializeWorkers(): void {
    for (let i = 0; i < this.MAX_WORKERS; i++) {
      const worker = new Worker('./export-worker.js', {
        workerData: {
          workerId: i,
          workerType: 'export'
        }
      });
      
      worker.on('message', (msg) => {
        if (msg.type === 'completed') {
          this.releaseWorker(worker);
        }
      });
      
      worker.on('error', (err) => {
        console.error(`Worker ${i} error:`, err);
        this.replaceWorker(i);
      });
      
      this.workers.push(worker);
      this.availableWorkers.push(worker);
    }
  }
  
  async processExport(
    exportId: string,
    format: ExportFormat
  ): Promise<void> {
    const worker = await this.getAvailableWorker();
    
    worker.postMessage({
      type: 'export',
      exportId,
      format
    });
  }
  
  private async getAvailableWorker(): Promise<Worker> {
    // Wait for available worker
    while (this.availableWorkers.length === 0) {
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    
    return this.availableWorkers.pop()!;
  }
  
  private releaseWorker(worker: Worker): void {
    if (!this.availableWorkers.includes(worker)) {
      this.availableWorkers.push(worker);
    }
  }
}
```

## Security Considerations

### Export Security

```typescript
@Injectable()
export class ExportSecurityService {
  constructor(
    private documentService: DocumentService,
    private cryptoService: CryptoService
  ) {}
  
  async validateExportAccess(
    userId: string,
    documentId: string
  ): Promise<boolean> {
    // Check document ownership
    const hasAccess = await this.documentService
      .checkAccess(userId, documentId, 'read');
    
    if (!hasAccess) {
      return false;
    }
    
    // Check export permissions
    const user = await this.getUserDetails(userId);
    if (user.subscription.plan === 'free') {
      // Check export limits
      const exportCount = await this.getMonthlyExportCount(userId);
      if (exportCount >= FREE_TIER_EXPORT_LIMIT) {
        throw new Error('Monthly export limit exceeded');
      }
    }
    
    return true;
  }
  
  async applyDocumentSecurity(
    file: Buffer,
    format: ExportFormat,
    security: any
  ): Promise<Buffer> {
    switch (format) {
      case 'pdf':
        return this.applyPDFSecurity(file, security);
      case 'epub':
        return this.applyEPUBDRM(file, security);
      default:
        return file;
    }
  }
  
  private async applyPDFSecurity(
    pdfBuffer: Buffer,
    security: PDFSecurityOptions
  ): Promise<Buffer> {
    const securedPdf = await PDFSecurity.secure(pdfBuffer, {
      userPassword: security.userPassword,
      ownerPassword: security.ownerPassword || this.generateOwnerPassword(),
      permissions: {
        printing: security.permissions.printing,
        modifying: security.permissions.modifying,
        copying: security.permissions.copying,
        annotating: security.permissions.annotating
      }
    });
    
    return securedPdf;
  }
}
```

## Monitoring and Metrics

```typescript
@Injectable()
export class ExportMetricsService {
  async recordExportMetrics(
    exportRequest: ExportRequest,
    result: ExportResult
  ): Promise<void> {
    // Record completion time
    const duration = result.metadata.generatedAt.getTime() - 
                    exportRequest.createdAt.getTime();
    
    await this.metrics.recordHistogram('export.duration', duration, {
      format: exportRequest.format,
      priority: exportRequest.priority,
      status: 'success'
    });
    
    // Record file size
    await this.metrics.recordHistogram('export.file_size', result.fileSize, {
      format: exportRequest.format
    });
    
    // Record page count
    if (result.metadata.pageCount) {
      await this.metrics.recordHistogram('export.page_count', 
        result.metadata.pageCount, {
          format: exportRequest.format
        }
      );
    }
    
    // Track format usage
    await this.metrics.incrementCounter('export.format.usage', {
      format: exportRequest.format
    });
  }
  
  async getExportStats(): Promise<ExportStatistics> {
    const stats = await this.redis.mget([
      'export:stats:total',
      'export:stats:success',
      'export:stats:failed',
      'export:stats:formats'
    ]);
    
    return {
      total: parseInt(stats[0] || '0'),
      success: parseInt(stats[1] || '0'),
      failed: parseInt(stats[2] || '0'),
      byFormat: JSON.parse(stats[3] || '{}'),
      successRate: stats[0] ? 
        (parseInt(stats[1] || '0') / parseInt(stats[0])) * 100 : 0
    };
  }
}
```

## Testing Strategy

### Unit Tests

```typescript
describe('ExportOrchestrator', () => {
  let orchestrator: ExportOrchestrator;
  let mockExporters: Map<ExportFormat, IExporter>;
  
  beforeEach(() => {
    mockExporters = new Map();
    mockExporters.set('epub', createMockExporter());
    mockExporters.set('pdf', createMockExporter());
    
    orchestrator = new ExportOrchestrator(
      mockTaskQueue,
      mockStorage,
      mockFirestore,
      mockExporters
    );
  });
  
  describe('createExport', () => {
    it('should create export request and queue task', async () => {
      const request = {
        documentId: 'doc-123',
        format: 'epub' as ExportFormat,
        options: {}
      };
      
      const result = await orchestrator.createExport(request, 'user-123');
      
      expect(result).toHaveProperty('id');
      expect(result.status.state).toBe('pending');
      expect(mockTaskQueue.createTask).toHaveBeenCalled();
    });
    
    it('should validate export options', async () => {
      const request = {
        documentId: 'doc-123',
        format: 'pdf' as ExportFormat,
        options: {
          pdf: {
            pageSize: 'invalid' // Invalid page size
          }
        }
      };
      
      await expect(orchestrator.createExport(request, 'user-123'))
        .rejects.toThrow('Invalid page size');
    });
  });
});
```

## Performance Requirements

| Metric | Target | Measurement |
|--------|--------|-------------|
| Export initiation | < 500ms | P95 |
| EPUB generation | < 30s | P95 for 200k words |
| PDF generation | < 45s | P95 for 200k words |
| DOCX generation | < 20s | P95 for 200k words |
| File upload | < 10s | P95 for 50MB |
| Queue processing | < 5s | P95 wait time |
| Concurrent exports | 100+ | Per instance |
| Storage efficiency | < $0.01 | Per export |

## Deployment Configuration

```yaml
# export-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: export-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: export-service
  template:
    spec:
      containers:
      - name: export-service
        image: gcr.io/deus-ex-machina/export-service:latest
        resources:
          requests:
            cpu: "2"
            memory: "4Gi"
          limits:
            cpu: "4"
            memory: "8Gi"
        env:
        - name: NODE_ENV
          value: "production"
        - name: MAX_EXPORT_WORKERS
          value: "4"
        - name: EXPORT_BUCKET
          value: "novel-exports-prod"
        volumeMounts:
        - name: temp-storage
          mountPath: /tmp/exports
      volumes:
      - name: temp-storage
        emptyDir:
          sizeLimit: 10Gi
```

## Conclusion

The Export Service provides a comprehensive solution for generating professional-quality exports of novel manuscripts. With support for industry-standard formats, robust error handling, and scalable architecture, it enables authors to seamlessly transition from digital creation to publishable outputs. The microservice design allows for independent scaling of format-specific exporters while maintaining consistent quality and performance.