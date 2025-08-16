import axios, { AxiosError } from 'axios'
import type { Chapter, Scene, EditorState } from '@/types/editor'
import type { Document, Chapter as DocumentChapter, Scene as DocumentScene } from '@/types/document'
import { htmlToMarkdown, markdownToHtml } from '@/utils/contentConverter'

const API_URL = import.meta.env.VITE_DOCUMENT_API_URL || 'http://localhost:8080'
console.log('[EditorService] Using API URL:', API_URL)

class EditorService {
  private getAuthHeader() {
    const token = localStorage.getItem('auth_token')
    console.log('[EditorService] Auth token exists:', !!token)
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  async loadDocument(documentId: string): Promise<EditorState> {
    // Get the document from the document service
    const response = await axios.get(`${API_URL}/document/${documentId}`, {
      headers: this.getAuthHeader()
    })
    
    const doc = response.data
    
    // Ensure document has at least one chapter
    if (!doc.chapters || doc.chapters.length === 0) {
      console.log('[EditorService] No chapters found, creating default chapter')
      await this.createDefaultChapter(documentId)
      
      // Try to reload the document to get the new chapter
      try {
        const reloadResponse = await axios.get(`${API_URL}/document/${documentId}`, {
          headers: this.getAuthHeader()
        })
        doc.chapters = reloadResponse.data.chapters || []
      } catch (reloadError) {
        console.error('[EditorService] Failed to reload document after creating default chapter:', reloadError)
        // Create a frontend-only default chapter if backend fails
        doc.chapters = [{
          id: `${documentId}-ch1`,
          chapterNumber: 1,
          title: 'Chapter 1',
          summary: 'The beginning of your story',
          scenes: [{
            id: `${documentId}-ch1-sc1`,
            sceneNumber: 1,
            title: 'Opening Scene',
            content: 'Start writing your story here...',
            type: 'NARRATIVE',
            wordCount: 5
          }],
          wordCount: 5,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }]
      }
    }
    
    // Extract content and convert from markdown to HTML
    const markdownContent = this.extractFullContent(doc)
    const htmlContent = markdownToHtml(markdownContent)
    
    // Transform the backend response to match EditorState interface
    return {
      documentId: doc.id,
      content: htmlContent,
      chapters: doc.chapters || [],
      metadata: {
        lastSaved: new Date(doc.updatedAt),
        version: '1.0.0',
        collaborators: [],
        locked: false
      }
    }
  }
  
  private extractFullContent(doc: Document & { chapters?: DocumentChapter[] }): string {
    // Extract all scene content and combine into a single string
    let fullContent = ''
    
    if (doc.chapters && doc.chapters.length > 0) {
      doc.chapters.forEach((chapter: DocumentChapter, chapterIndex: number) => {
        if (chapterIndex > 0) fullContent += '\n\n'
        fullContent += `# Chapter ${chapter.chapterNumber}: ${chapter.title}\n\n`
        
        if (chapter.scenes && chapter.scenes.length > 0) {
          chapter.scenes.forEach((scene: DocumentScene, sceneIndex: number) => {
            if (sceneIndex > 0) fullContent += '\n\n'
            if (scene.title) {
              fullContent += `## ${scene.title}\n\n`
            }
            fullContent += scene.content || ''
          })
        }
      })
    }
    
    return fullContent
  }

  async saveContent(documentId: string, htmlContent: string): Promise<void> {
    console.log('[EditorService] saveContent called for document:', documentId)
    console.log('[EditorService] HTML content length:', htmlContent.length)
    
    // Convert HTML to markdown for backend storage
    const content = htmlToMarkdown(htmlContent)
    console.log('[EditorService] Markdown content:', content.substring(0, 200) + '...')
    
    // Parse the content to identify chapters and scenes
    const lines = content.split('\n')
    let currentChapter = 0
    let currentScene = 0
    let sceneContent = ''
    let hasStructuredContent = false
    
    for (const line of lines) {
      // Check if this is a chapter heading
      if (line.startsWith('# Chapter ')) {
        hasStructuredContent = true
        // Save previous scene if any
        if (currentChapter > 0 && currentScene > 0 && sceneContent.trim()) {
          await this.updateScene(documentId, currentChapter, currentScene, sceneContent.trim())
        }
        
        // Parse chapter number
        const match = line.match(/# Chapter (\d+):/)
        if (match) {
          currentChapter = parseInt(match[1])
          currentScene = 0
          sceneContent = ''
        }
      }
      // Check if this is a scene heading
      else if (line.startsWith('## ')) {
        // Save previous scene if any
        if (currentChapter > 0 && currentScene > 0 && sceneContent.trim()) {
          await this.updateScene(documentId, currentChapter, currentScene, sceneContent.trim())
        }
        
        currentScene++
        sceneContent = ''
      }
      // Add to scene content
      else if (currentChapter > 0) {
        sceneContent += line + '\n'
      } else if (!hasStructuredContent) {
        // If no structure found yet, accumulate content
        sceneContent += line + '\n'
      }
    }
    
    // If no structured content was found, save everything as Chapter 1, Scene 1
    if (!hasStructuredContent && sceneContent.trim()) {
      console.log('[EditorService] No chapter structure found, saving as Chapter 1, Scene 1')
      console.log('[EditorService] Content to save:', sceneContent.substring(0, 100) + '...')
      await this.ensureChapterExists(documentId, 1, 'Chapter 1')
      await this.updateScene(documentId, 1, 1, sceneContent.trim())
    }
    // Save the last scene if we have structured content
    else if (currentChapter > 0 && currentScene > 0 && sceneContent.trim()) {
      console.log(`[EditorService] Saving last scene - Chapter ${currentChapter}, Scene ${currentScene}`)
      await this.updateScene(documentId, currentChapter, currentScene, sceneContent.trim())
    } else if (currentChapter > 0 && currentScene === 0) {
      // We have a chapter but no scenes were detected, create scene 1
      console.log(`[EditorService] Chapter ${currentChapter} has no scenes, creating Scene 1`)
      await this.updateScene(documentId, currentChapter, 1, sceneContent.trim() || 'New scene')
    }
    
    // Update document metadata
    try {
      const wordCount = content.split(/\s+/).filter(word => word.length > 0).length
      console.log(`[EditorService] Updating document metadata - wordCount: ${wordCount}`)
      
      const response = await axios.put(`${API_URL}/document/${documentId}`, 
        { 
          currentWordCount: wordCount,
          status: 'IN_PROGRESS'
        }, 
        { headers: this.getAuthHeader() }
      )
      console.log('[EditorService] Document metadata updated successfully:', response.status)
    } catch (metadataError) {
      const axiosError = metadataError as AxiosError
      console.error('[EditorService] Failed to update document metadata:', axiosError.response?.data || axiosError.message)
      throw metadataError
    }
  }
  
  private async ensureChapterExists(documentId: string, chapterNumber: number, title: string): Promise<void> {
    try {
      // Try to get the chapter first
      await axios.get(
        `${API_URL}/chapter/${documentId}/${chapterNumber}`,
        { headers: this.getAuthHeader() }
      )
    } catch (error) {
      const axiosError = error as AxiosError
      if (axiosError.response?.status === 404) {
        // Chapter doesn't exist, create it
        await axios.post(
          `${API_URL}/chapter/${documentId}/${chapterNumber}`,
          { title },
          { headers: this.getAuthHeader() }
        )
      } else {
        throw error
      }
    }
  }
  
  private async updateScene(documentId: string, chapterNumber: number, sceneNumber: number, content: string): Promise<void> {
    console.log(`[EditorService] Updating scene: ${documentId}/${chapterNumber}/${sceneNumber}`)
    try {
      const response = await axios.put(
        `${API_URL}/scene/${documentId}/${chapterNumber}/${sceneNumber}`,
        { content },
        { headers: this.getAuthHeader() }
      )
      console.log('[EditorService] Scene updated successfully:', response.status)
    } catch (error) {
      const axiosError = error as AxiosError
      console.log('[EditorService] Update scene error:', axiosError.response?.status, axiosError.message)
      
      // If scene doesn't exist, create it
      if (axiosError.response?.status === 404) {
        console.log('[EditorService] Scene not found, need to create it')
        // First ensure the chapter exists
        await this.ensureChapterExists(documentId, chapterNumber, `Chapter ${chapterNumber}`)
        
        try {
          // The Java backend expects all three parameters in URL: documentId/chapterNumber/sceneNumber
          const createResponse = await axios.post(
            `${API_URL}/scene/${documentId}/${chapterNumber}/${sceneNumber}`,
            { 
              content,
              title: `Scene ${sceneNumber}`,
              type: 'NARRATIVE'
            },
            { headers: this.getAuthHeader() }
          )
          console.log('[EditorService] Scene created successfully:', createResponse.status)
        } catch (createError) {
          const axiosCreateError = createError as AxiosError
          console.error('[EditorService] Failed to create scene:', axiosCreateError.response?.data || axiosCreateError.message)
          throw createError
        }
      } else {
        console.error('[EditorService] Unexpected error:', error)
        throw error
      }
    }
  }

  private async createDefaultChapter(documentId: string): Promise<void> {
    try {
      // First check if the document exists
      console.log('[EditorService] Creating default chapter for document:', documentId)
      const endpoint = `${API_URL}/chapter/${documentId}/1`
      const payload = { 
        title: 'Chapter 1',
        summary: 'The beginning of your story'
      }
      console.log('[EditorService] POST to:', endpoint)
      console.log('[EditorService] Payload:', payload)
      
      // Try to create chapter 1
      await axios.post(endpoint, payload, { headers: this.getAuthHeader() })
      console.log('[EditorService] Default chapter created')
      
      // Also create a default scene (all three parameters in URL: documentId/chapterNumber/sceneNumber)
      try {
        await axios.post(
          `${API_URL}/scene/${documentId}/1/1`, // documentId/chapterNumber/sceneNumber
          { 
            title: 'Opening Scene',
            content: 'Start writing your story here...',
            type: 'NARRATIVE'
          },
          { headers: this.getAuthHeader() }
        )
        console.log('[EditorService] Default scene created')
      } catch (sceneError) {
        const axiosSceneError = sceneError as AxiosError
        console.error('[EditorService] Failed to create default scene:', axiosSceneError.response?.data || axiosSceneError.message)
        console.error('[EditorService] Scene creation endpoint:', `${API_URL}/scene/${documentId}/1/1`)
        // Continue even if scene creation fails - the document will still have a chapter
      }
    } catch (error) {
      const axiosError = error as AxiosError
      console.error('[EditorService] Failed to create default chapter:', error)
      
      // If it's a 404, the document might not exist yet
      if (axiosError.response?.status === 404) {
        console.error('[EditorService] Chapter creation failed - document might not exist. Document ID:', documentId)
        console.error('[EditorService] Chapter creation endpoint:', `${API_URL}/chapter/${documentId}/1`)
      }
    }
  }

  async createChapter(documentId: string, title: string): Promise<Chapter> {
    try {
      // First, get the document to find the next chapter number
      const docResponse = await axios.get(`${API_URL}/document/${documentId}`, {
        headers: this.getAuthHeader()
      })
      
      const doc = docResponse.data
      const nextChapterNumber = (doc.chapters?.length || 0) + 1
      
      // Create the chapter with the proper endpoint format
      try {
        await axios.post(
          `${API_URL}/chapter/${documentId}/${nextChapterNumber}`,
          { 
            title,
            summary: '',
            scenes: []
          },
          { headers: this.getAuthHeader() }
        )
        console.log(`[EditorService] Chapter ${nextChapterNumber} created successfully`)
      } catch (createError) {
        const axiosError = createError as AxiosError
        console.error('[EditorService] Failed to create chapter:', axiosError.response?.data || axiosError.message)
        console.error('[EditorService] Chapter creation endpoint:', `${API_URL}/chapter/${documentId}/${nextChapterNumber}`)
        throw createError
      }
      
      // Transform to match frontend Chapter interface
      return {
        id: `${documentId}-ch${nextChapterNumber}`,
        documentId,
        title,
        chapterNumber: nextChapterNumber,
        summary: '',
        createdAt: new Date(),
        updatedAt: new Date(),
        wordCount: 0,
        sceneCount: 0,
        scenes: []
      }
    } catch (error) {
      console.error('[EditorService] Error in createChapter:', error)
      throw error
    }
  }

  async createScene(chapterId: string, title: string): Promise<Scene> {
    // Parse the chapterId to extract documentId and chapterNumber
    // Expected format: "documentId-chN" where N is the chapter number
    const parts = chapterId.split('-ch')
    const documentId = parts[0]
    const chapterNumber = parseInt(parts[1] || '1')
    
    // Get the chapter to find the next scene number
    const docResponse = await axios.get(`${API_URL}/document/${documentId}`, {
      headers: this.getAuthHeader()
    })
    
    const doc = docResponse.data
    const chapter = doc.chapters?.find((ch: DocumentChapter) => ch.chapterNumber === chapterNumber)
    const nextSceneNumber = (chapter?.scenes?.length || 0) + 1
    
    // Create the scene with the proper endpoint format
    await axios.post(
      `${API_URL}/scene/${documentId}/${chapterNumber}/${nextSceneNumber}`,
      { 
        title,
        content: '',
        type: 'NARRATIVE'
      },
      { headers: this.getAuthHeader() }
    )
    
    // Return the created scene
    return {
      id: `${documentId}-ch${chapterNumber}-sc${nextSceneNumber}`,
      chapterId,
      documentId,
      content: '',
      sceneNumber: nextSceneNumber,
      title,
      type: 'NARRATIVE',
      createdAt: new Date(),
      updatedAt: new Date(),
      wordCount: 0
    }
  }
}

export const editorService = new EditorService()