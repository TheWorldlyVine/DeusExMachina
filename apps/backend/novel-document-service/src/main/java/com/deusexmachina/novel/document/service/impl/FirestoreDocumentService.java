package com.deusexmachina.novel.document.service.impl;

import com.google.cloud.firestore.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.deusexmachina.novel.document.config.DocumentConfig;
import com.deusexmachina.novel.document.model.Document;
import com.deusexmachina.novel.document.model.Chapter;
import com.deusexmachina.novel.document.model.Scene;
import com.deusexmachina.novel.document.model.SceneType;
import com.deusexmachina.novel.document.repository.DocumentRepository;
import com.deusexmachina.novel.document.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class FirestoreDocumentService implements DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreDocumentService.class);
    private static final String DOCUMENTS_COLLECTION = "documents";
    private static final String CHAPTERS_COLLECTION = "chapters";
    private static final String SCENES_COLLECTION = "scenes";
    private static final int MAX_SCENE_SIZE = 50000; // 50KB per scene for chunking
    
    private final Firestore firestore;
    private final Storage storage;
    private final DocumentConfig documentConfig;
    
    @Inject
    public FirestoreDocumentService(Firestore firestore, Storage storage, DocumentConfig documentConfig) {
        this.firestore = firestore;
        this.storage = storage;
        this.documentConfig = documentConfig;
        logger.info("FirestoreDocumentService initialized");
    }
    
    @Override
    public Document createDocument(Document document) {
        try {
            // Initialize document fields
            if (document.getChapterIds() == null) {
                document.setChapterIds(new ArrayList<>());
            }
            if (document.getTags() == null) {
                document.setTags(new ArrayList<>());
            }
            if (document.getMetadata() == null) {
                document.setMetadata(new HashMap<>());
            }
            
            // Save to Firestore
            DocumentReference docRef = firestore.collection(DOCUMENTS_COLLECTION).document(document.getId());
            docRef.set(document).get();
            
            logger.info("Created document: {}", document.getId());
            return document;
        } catch (Exception e) {
            logger.error("Error creating document", e);
            throw new RuntimeException("Failed to create document", e);
        }
    }
    
    @Override
    public Document getDocument(String documentId) {
        try {
            DocumentSnapshot snapshot = firestore.collection(DOCUMENTS_COLLECTION)
                    .document(documentId)
                    .get()
                    .get();
                    
            if (!snapshot.exists()) {
                return null;
            }
            
            Document document = snapshot.toObject(Document.class);
            if (document != null && !document.isActive()) {
                return null; // Soft deleted
            }
            
            return document;
        } catch (Exception e) {
            logger.error("Error getting document: {}", documentId, e);
            throw new RuntimeException("Failed to get document", e);
        }
    }
    
    @Override
    public Document updateDocument(Document document) {
        try {
            DocumentReference docRef = firestore.collection(DOCUMENTS_COLLECTION).document(document.getId());
            
            // Get existing document
            DocumentSnapshot snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                return null;
            }
            
            // Update only provided fields
            Map<String, Object> updates = new HashMap<>();
            if (document.getTitle() != null) updates.put("title", document.getTitle());
            if (document.getSubtitle() != null) updates.put("subtitle", document.getSubtitle());
            if (document.getDescription() != null) updates.put("description", document.getDescription());
            if (document.getGenre() != null) updates.put("genre", document.getGenre());
            if (document.getTags() != null) updates.put("tags", document.getTags());
            if (document.getStatus() != null) updates.put("status", document.getStatus());
            if (document.getSettings() != null) updates.put("settings", document.getSettings());
            if (document.getMetadata() != null) updates.put("metadata", document.getMetadata());
            updates.put("updatedAt", document.getUpdatedAt());
            
            docRef.update(updates).get();
            
            // Return updated document
            return getDocument(document.getId());
        } catch (Exception e) {
            logger.error("Error updating document: {}", document.getId(), e);
            throw new RuntimeException("Failed to update document", e);
        }
    }
    
    @Override
    public boolean deleteDocument(String documentId) {
        try {
            DocumentReference docRef = firestore.collection(DOCUMENTS_COLLECTION).document(documentId);
            
            // Soft delete by setting active to false
            Map<String, Object> updates = new HashMap<>();
            updates.put("active", false);
            updates.put("updatedAt", Instant.now());
            
            docRef.update(updates).get();
            
            // Also soft delete all chapters and scenes
            softDeleteChapters(documentId);
            
            logger.info("Soft deleted document: {}", documentId);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting document: {}", documentId, e);
            return false;
        }
    }
    
    @Override
    public List<Document> listDocumentsByUser(String userId) {
        try {
            Query query = firestore.collection(DOCUMENTS_COLLECTION)
                    .whereEqualTo("authorId", userId)
                    .whereEqualTo("active", true)
                    .orderBy("updatedAt", Query.Direction.DESCENDING);
            
            return query.get().get().getDocuments().stream()
                    .map(doc -> doc.toObject(Document.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error listing documents for user: {}", userId, e);
            throw new RuntimeException("Failed to list documents", e);
        }
    }
    
    @Override
    public List<Document> getDocumentsByContext(String contextId) {
        try {
            Query query = firestore.collection(DOCUMENTS_COLLECTION)
                    .whereEqualTo("contextId", contextId)
                    .whereEqualTo("active", true);
            
            return query.get().get().getDocuments().stream()
                    .map(doc -> doc.toObject(Document.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting documents by context: {}", contextId, e);
            throw new RuntimeException("Failed to get documents by context", e);
        }
    }
    
    @Override
    public Chapter createChapter(Chapter chapter) {
        try {
            // Initialize chapter fields
            if (chapter.getSceneIds() == null) {
                chapter.setSceneIds(new ArrayList<>());
            }
            
            // Save to Firestore
            DocumentReference chapterRef = firestore.collection(CHAPTERS_COLLECTION).document(chapter.getId());
            chapterRef.set(chapter).get();
            
            // Update document's chapter list
            updateDocumentChapterList(chapter.getDocumentId(), chapter.getId(), true);
            
            logger.info("Created chapter: {} for document: {}", chapter.getId(), chapter.getDocumentId());
            return chapter;
        } catch (Exception e) {
            logger.error("Error creating chapter", e);
            throw new RuntimeException("Failed to create chapter", e);
        }
    }
    
    @Override
    public Chapter getChapter(String chapterId) {
        try {
            DocumentSnapshot snapshot = firestore.collection(CHAPTERS_COLLECTION)
                    .document(chapterId)
                    .get()
                    .get();
                    
            if (!snapshot.exists()) {
                return null;
            }
            
            Chapter chapter = snapshot.toObject(Chapter.class);
            if (chapter != null && !chapter.isActive()) {
                return null; // Soft deleted
            }
            
            return chapter;
        } catch (Exception e) {
            logger.error("Error getting chapter: {}", chapterId, e);
            throw new RuntimeException("Failed to get chapter", e);
        }
    }
    
    @Override
    public Chapter getChapterByNumber(String documentId, int chapterNumber) {
        try {
            Query query = firestore.collection(CHAPTERS_COLLECTION)
                    .whereEqualTo("documentId", documentId)
                    .whereEqualTo("chapterNumber", chapterNumber)
                    .whereEqualTo("active", true)
                    .limit(1);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            if (docs.isEmpty()) {
                return null;
            }
            
            return docs.get(0).toObject(Chapter.class);
        } catch (Exception e) {
            logger.error("Error getting chapter by number: {} for document: {}", chapterNumber, documentId, e);
            throw new RuntimeException("Failed to get chapter by number", e);
        }
    }
    
    @Override
    public List<Chapter> getChaptersByDocument(String documentId) {
        try {
            Query query = firestore.collection(CHAPTERS_COLLECTION)
                    .whereEqualTo("documentId", documentId)
                    .whereEqualTo("active", true)
                    .orderBy("chapterNumber");
            
            return query.get().get().getDocuments().stream()
                    .map(doc -> doc.toObject(Chapter.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting chapters for document: {}", documentId, e);
            throw new RuntimeException("Failed to get chapters", e);
        }
    }
    
    @Override
    public Chapter updateChapter(Chapter chapter) {
        try {
            // Find chapter by document ID and chapter number
            Chapter existing = getChapterByNumber(chapter.getDocumentId(), chapter.getChapterNumber());
            if (existing == null) {
                return null;
            }
            
            DocumentReference chapterRef = firestore.collection(CHAPTERS_COLLECTION).document(existing.getId());
            
            // Update only provided fields
            Map<String, Object> updates = new HashMap<>();
            if (chapter.getTitle() != null) updates.put("title", chapter.getTitle());
            if (chapter.getSummary() != null) updates.put("summary", chapter.getSummary());
            if (chapter.getNotes() != null) updates.put("notes", chapter.getNotes());
            updates.put("updatedAt", chapter.getUpdatedAt());
            
            chapterRef.update(updates).get();
            
            // Update document's updated timestamp
            updateDocumentTimestamp(chapter.getDocumentId());
            
            return getChapter(existing.getId());
        } catch (Exception e) {
            logger.error("Error updating chapter", e);
            throw new RuntimeException("Failed to update chapter", e);
        }
    }
    
    @Override
    public boolean deleteChapter(String documentId, int chapterNumber) {
        try {
            Chapter chapter = getChapterByNumber(documentId, chapterNumber);
            if (chapter == null) {
                return false;
            }
            
            DocumentReference chapterRef = firestore.collection(CHAPTERS_COLLECTION).document(chapter.getId());
            
            // Soft delete
            Map<String, Object> updates = new HashMap<>();
            updates.put("active", false);
            updates.put("updatedAt", Instant.now());
            
            chapterRef.update(updates).get();
            
            // Also soft delete all scenes in the chapter
            softDeleteScenes(chapter.getId());
            
            // Update document's chapter list
            updateDocumentChapterList(documentId, chapter.getId(), false);
            
            logger.info("Soft deleted chapter: {}", chapter.getId());
            return true;
        } catch (Exception e) {
            logger.error("Error deleting chapter", e);
            return false;
        }
    }
    
    @Override
    public Scene createScene(Scene scene) {
        try {
            // Check if content needs chunking
            if (scene.getContent() != null && scene.getContent().length() > MAX_SCENE_SIZE) {
                scene = chunkSceneContent(scene);
            }
            
            // Initialize scene fields
            if (scene.getCharacterIds() == null) {
                scene.setCharacterIds(new ArrayList<>());
            }
            if (scene.getMetadata() == null) {
                scene.setMetadata(new HashMap<>());
            }
            if (scene.getType() == null) {
                scene.setType(SceneType.NARRATIVE);
            }
            
            // Calculate word count
            if (scene.getContent() != null) {
                scene.setWordCount(countWords(scene.getContent()));
            }
            
            // Save to Firestore
            DocumentReference sceneRef = firestore.collection(SCENES_COLLECTION).document(scene.getId());
            sceneRef.set(scene).get();
            
            // Update chapter's scene list
            updateChapterSceneList(scene.getChapterId(), scene.getId(), true);
            
            logger.info("Created scene: {} for chapter: {}", scene.getId(), scene.getChapterId());
            return scene;
        } catch (Exception e) {
            logger.error("Error creating scene", e);
            throw new RuntimeException("Failed to create scene", e);
        }
    }
    
    @Override
    public Scene getScene(String sceneId) {
        try {
            DocumentSnapshot snapshot = firestore.collection(SCENES_COLLECTION)
                    .document(sceneId)
                    .get()
                    .get();
                    
            if (!snapshot.exists()) {
                return null;
            }
            
            Scene scene = snapshot.toObject(Scene.class);
            if (scene != null && !scene.isActive()) {
                return null; // Soft deleted
            }
            
            // Load chunked content if necessary
            if (scene != null && scene.getStorageRef() != null) {
                scene = loadChunkedContent(scene);
            }
            
            return scene;
        } catch (Exception e) {
            logger.error("Error getting scene: {}", sceneId, e);
            throw new RuntimeException("Failed to get scene", e);
        }
    }
    
    @Override
    public Scene getSceneByNumber(String chapterId, int sceneNumber) {
        try {
            Query query = firestore.collection(SCENES_COLLECTION)
                    .whereEqualTo("chapterId", chapterId)
                    .whereEqualTo("sceneNumber", sceneNumber)
                    .whereEqualTo("active", true)
                    .limit(1);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            if (docs.isEmpty()) {
                return null;
            }
            
            Scene scene = docs.get(0).toObject(Scene.class);
            
            // Load chunked content if necessary
            if (scene != null && scene.getStorageRef() != null) {
                scene = loadChunkedContent(scene);
            }
            
            return scene;
        } catch (Exception e) {
            logger.error("Error getting scene by number: {} for chapter: {}", sceneNumber, chapterId, e);
            throw new RuntimeException("Failed to get scene by number", e);
        }
    }
    
    @Override
    public List<Scene> getScenesByChapter(String chapterId) {
        try {
            Query query = firestore.collection(SCENES_COLLECTION)
                    .whereEqualTo("chapterId", chapterId)
                    .whereEqualTo("active", true)
                    .orderBy("sceneNumber");
            
            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        Scene scene = doc.toObject(Scene.class);
                        // Load chunked content if necessary
                        if (scene != null && scene.getStorageRef() != null) {
                            scene = loadChunkedContent(scene);
                        }
                        return scene;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting scenes for chapter: {}", chapterId, e);
            throw new RuntimeException("Failed to get scenes", e);
        }
    }
    
    @Override
    public Scene updateScene(Scene scene) {
        try {
            // Find scene by chapter ID and scene number
            Chapter chapter = getChapterByNumber(scene.getDocumentId(), Integer.parseInt(scene.getChapterId()));
            if (chapter == null) {
                return null;
            }
            
            Scene existing = getSceneByNumber(chapter.getId(), scene.getSceneNumber());
            if (existing == null) {
                return null;
            }
            
            DocumentReference sceneRef = firestore.collection(SCENES_COLLECTION).document(existing.getId());
            
            // Update only provided fields
            Map<String, Object> updates = new HashMap<>();
            if (scene.getTitle() != null) updates.put("title", scene.getTitle());
            if (scene.getContent() != null) {
                // Check if content needs chunking
                if (scene.getContent().length() > MAX_SCENE_SIZE) {
                    scene.setId(existing.getId());
                    scene = chunkSceneContent(scene);
                    updates.put("storageRef", scene.getStorageRef());
                    updates.put("content", ""); // Clear inline content
                } else {
                    updates.put("content", scene.getContent());
                    // Clear storage ref if content fits inline
                    if (existing.getStorageRef() != null) {
                        deleteChunkedContent(existing.getStorageRef());
                        updates.put("storageRef", null);
                    }
                }
                updates.put("wordCount", countWords(scene.getContent()));
            }
            if (scene.getSummary() != null) updates.put("summary", scene.getSummary());
            if (scene.getType() != null) updates.put("type", scene.getType());
            if (scene.getNotes() != null) updates.put("notes", scene.getNotes());
            updates.put("updatedAt", scene.getUpdatedAt());
            
            sceneRef.update(updates).get();
            
            // Update chapter and document timestamps
            updateChapterTimestamp(chapter.getId());
            updateDocumentTimestamp(scene.getDocumentId());
            
            return getScene(existing.getId());
        } catch (Exception e) {
            logger.error("Error updating scene", e);
            throw new RuntimeException("Failed to update scene", e);
        }
    }
    
    @Override
    public boolean deleteScene(String documentId, int chapterNumber, int sceneNumber) {
        try {
            Chapter chapter = getChapterByNumber(documentId, chapterNumber);
            if (chapter == null) {
                return false;
            }
            
            Scene scene = getSceneByNumber(chapter.getId(), sceneNumber);
            if (scene == null) {
                return false;
            }
            
            DocumentReference sceneRef = firestore.collection(SCENES_COLLECTION).document(scene.getId());
            
            // Soft delete
            Map<String, Object> updates = new HashMap<>();
            updates.put("active", false);
            updates.put("updatedAt", Instant.now());
            
            sceneRef.update(updates).get();
            
            // Delete chunked content if exists
            if (scene.getStorageRef() != null) {
                deleteChunkedContent(scene.getStorageRef());
            }
            
            // Update chapter's scene list
            updateChapterSceneList(chapter.getId(), scene.getId(), false);
            
            logger.info("Soft deleted scene: {}", scene.getId());
            return true;
        } catch (Exception e) {
            logger.error("Error deleting scene", e);
            return false;
        }
    }
    
    @Override
    public String getFullDocumentContent(String documentId) {
        try {
            return getFullDocumentContentAsync(documentId).get();
        } catch (Exception e) {
            logger.error("Error getting full document content: {}", documentId, e);
            throw new RuntimeException("Failed to get document content", e);
        }
    }
    
    @Override
    public CompletableFuture<String> getFullDocumentContentAsync(String documentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document document = getDocument(documentId);
                if (document == null) {
                    return null;
                }
                
                StringBuilder content = new StringBuilder();
                
                // Add title
                content.append(document.getTitle()).append("\n\n");
                if (document.getSubtitle() != null) {
                    content.append(document.getSubtitle()).append("\n\n");
                }
                
                // Get all chapters
                List<Chapter> chapters = getChaptersByDocument(documentId);
                
                for (Chapter chapter : chapters) {
                    // Add chapter title
                    content.append("Chapter ").append(chapter.getChapterNumber())
                           .append(": ").append(chapter.getTitle()).append("\n\n");
                    
                    // Get all scenes in chapter
                    List<Scene> scenes = getScenesByChapter(chapter.getId());
                    
                    for (Scene scene : scenes) {
                        if (scene.getTitle() != null && !scene.getTitle().isEmpty()) {
                            content.append("## ").append(scene.getTitle()).append("\n\n");
                        }
                        content.append(scene.getContent()).append("\n\n");
                    }
                    
                    content.append("\n"); // Extra line between chapters
                }
                
                return content.toString();
            } catch (Exception e) {
                logger.error("Error assembling document content: {}", documentId, e);
                throw new RuntimeException("Failed to assemble document content", e);
            }
        });
    }
    
    @Override
    public String exportDocument(String documentId, String format) {
        try {
            return exportDocumentAsync(documentId, format).get();
        } catch (Exception e) {
            logger.error("Error exporting document: {} to format: {}", documentId, format, e);
            throw new RuntimeException("Failed to export document", e);
        }
    }
    
    @Override
    public CompletableFuture<String> exportDocumentAsync(String documentId, String format) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String content = getFullDocumentContent(documentId);
                if (content == null) {
                    return null;
                }
                
                switch (format.toLowerCase()) {
                    case "txt":
                    case "text":
                        return content;
                        
                    case "markdown":
                    case "md":
                        // Already in markdown format
                        return content;
                        
                    case "html":
                        return convertToHtml(content);
                        
                    case "pdf":
                        // TODO: Implement PDF export
                        throw new UnsupportedOperationException("PDF export not yet implemented");
                        
                    default:
                        throw new IllegalArgumentException("Unsupported export format: " + format);
                }
            } catch (Exception e) {
                logger.error("Error exporting document: {} to format: {}", documentId, format, e);
                throw new RuntimeException("Failed to export document", e);
            }
        });
    }
    
    // Helper methods
    
    private void updateDocumentChapterList(String documentId, String chapterId, boolean add) throws Exception {
        DocumentReference docRef = firestore.collection(DOCUMENTS_COLLECTION).document(documentId);
        
        if (add) {
            docRef.update("chapterIds", FieldValue.arrayUnion(chapterId)).get();
        } else {
            docRef.update("chapterIds", FieldValue.arrayRemove(chapterId)).get();
        }
        
        updateDocumentCounts(documentId);
    }
    
    private void updateChapterSceneList(String chapterId, String sceneId, boolean add) throws Exception {
        DocumentReference chapterRef = firestore.collection(CHAPTERS_COLLECTION).document(chapterId);
        
        if (add) {
            chapterRef.update("sceneIds", FieldValue.arrayUnion(sceneId)).get();
        } else {
            chapterRef.update("sceneIds", FieldValue.arrayRemove(sceneId)).get();
        }
        
        // Update chapter counts
        updateChapterCounts(chapterId);
    }
    
    private void updateDocumentTimestamp(String documentId) {
        try {
            firestore.collection(DOCUMENTS_COLLECTION)
                    .document(documentId)
                    .update("updatedAt", Instant.now())
                    .get();
        } catch (Exception e) {
            logger.warn("Failed to update document timestamp: {}", documentId, e);
        }
    }
    
    private void updateChapterTimestamp(String chapterId) {
        try {
            firestore.collection(CHAPTERS_COLLECTION)
                    .document(chapterId)
                    .update("updatedAt", Instant.now())
                    .get();
        } catch (Exception e) {
            logger.warn("Failed to update chapter timestamp: {}", chapterId, e);
        }
    }
    
    private void updateDocumentCounts(String documentId) {
        try {
            List<Chapter> chapters = getChaptersByDocument(documentId);
            int totalWordCount = 0;
            int totalSceneCount = 0;
            
            for (Chapter chapter : chapters) {
                totalWordCount += chapter.getWordCount();
                totalSceneCount += chapter.getSceneCount();
            }
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("chapterCount", chapters.size());
            updates.put("sceneCount", totalSceneCount);
            updates.put("wordCount", totalWordCount);
            
            firestore.collection(DOCUMENTS_COLLECTION)
                    .document(documentId)
                    .update(updates)
                    .get();
        } catch (Exception e) {
            logger.warn("Failed to update document counts: {}", documentId, e);
        }
    }
    
    private void updateChapterCounts(String chapterId) {
        try {
            Chapter chapter = getChapter(chapterId);
            if (chapter == null) return;
            
            List<Scene> scenes = getScenesByChapter(chapterId);
            int totalWordCount = scenes.stream()
                    .mapToInt(Scene::getWordCount)
                    .sum();
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("sceneCount", scenes.size());
            updates.put("wordCount", totalWordCount);
            
            firestore.collection(CHAPTERS_COLLECTION)
                    .document(chapterId)
                    .update(updates)
                    .get();
                    
            // Also update document counts
            updateDocumentCounts(chapter.getDocumentId());
        } catch (Exception e) {
            logger.warn("Failed to update chapter counts: {}", chapterId, e);
        }
    }
    
    private void softDeleteChapters(String documentId) {
        try {
            List<Chapter> chapters = getChaptersByDocument(documentId);
            WriteBatch batch = firestore.batch();
            
            for (Chapter chapter : chapters) {
                DocumentReference ref = firestore.collection(CHAPTERS_COLLECTION).document(chapter.getId());
                batch.update(ref, "active", false, "updatedAt", Instant.now());
                
                // Also delete scenes
                softDeleteScenes(chapter.getId());
            }
            
            batch.commit().get();
        } catch (Exception e) {
            logger.error("Error soft deleting chapters for document: {}", documentId, e);
        }
    }
    
    private void softDeleteScenes(String chapterId) {
        try {
            List<Scene> scenes = getScenesByChapter(chapterId);
            WriteBatch batch = firestore.batch();
            
            for (Scene scene : scenes) {
                DocumentReference ref = firestore.collection(SCENES_COLLECTION).document(scene.getId());
                batch.update(ref, "active", false, "updatedAt", Instant.now());
                
                // Delete chunked content if exists
                if (scene.getStorageRef() != null) {
                    deleteChunkedContent(scene.getStorageRef());
                }
            }
            
            batch.commit().get();
        } catch (Exception e) {
            logger.error("Error soft deleting scenes for chapter: {}", chapterId, e);
        }
    }
    
    private Scene chunkSceneContent(Scene scene) {
        try {
            String bucketName = documentConfig.getStorageBucket();
            String objectName = String.format("scenes/%s/%s/content.txt", 
                    scene.getDocumentId(), scene.getId());
            
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("text/plain")
                    .build();
                    
            storage.create(blobInfo, scene.getContent().getBytes(StandardCharsets.UTF_8));
            
            scene.setStorageRef(objectName);
            scene.setContent(""); // Clear inline content
            
            logger.info("Chunked scene content to: {}", objectName);
            return scene;
        } catch (Exception e) {
            logger.error("Error chunking scene content", e);
            throw new RuntimeException("Failed to chunk scene content", e);
        }
    }
    
    private Scene loadChunkedContent(Scene scene) {
        try {
            String bucketName = documentConfig.getStorageBucket();
            BlobId blobId = BlobId.of(bucketName, scene.getStorageRef());
            
            Blob blob = storage.get(blobId);
            if (blob != null && blob.exists()) {
                String content = new String(blob.getContent(), StandardCharsets.UTF_8);
                scene.setContent(content);
            }
            
            return scene;
        } catch (Exception e) {
            logger.error("Error loading chunked content for scene: {}", scene.getId(), e);
            // Return scene without content rather than failing
            return scene;
        }
    }
    
    private void deleteChunkedContent(String storageRef) {
        try {
            String bucketName = documentConfig.getStorageBucket();
            BlobId blobId = BlobId.of(bucketName, storageRef);
            storage.delete(blobId);
            logger.info("Deleted chunked content: {}", storageRef);
        } catch (Exception e) {
            logger.warn("Failed to delete chunked content: {}", storageRef, e);
        }
    }
    
    private int countWords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        return content.trim().split("\\s+").length;
    }
    
    private String convertToHtml(String markdown) {
        // Basic markdown to HTML conversion
        // TODO: Use a proper markdown library
        return markdown
                .replaceAll("^# (.+)$", "<h1>$1</h1>")
                .replaceAll("^## (.+)$", "<h2>$1</h2>")
                .replaceAll("\\n\\n", "</p><p>")
                .replaceAll("^(.+)$", "<p>$1</p>");
    }
}