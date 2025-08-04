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
      console.log('No chapter structure found, saving as Chapter 1, Scene 1')
      await this.ensureChapterExists(documentId, 1, 'Chapter 1')
      await this.updateScene(documentId, 1, 1, sceneContent.trim())
    }
    // Save the last scene if we have structured content
    else if (currentChapter > 0 && currentScene > 0 && sceneContent.trim()) {
      await this.updateScene(documentId, currentChapter, currentScene, sceneContent.trim())
    }
    
    // Update document metadata
    const wordCount = content.split(/\s+/).filter(word => word.length > 0).length
    await axios.put(`${API_URL}/document/${documentId}`, 
      { 
        currentWordCount: wordCount,
        status: 'IN_PROGRESS'
      }, 
      { headers: this.getAuthHeader() }
    )
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
        console.log('[EditorService] Scene not found, creating new scene')
        try {
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
          console.error('[EditorService] Failed to create scene:', createError)
          throw createError
        }
      } else {
        console.error('[EditorService] Unexpected error:', error)
        throw error
      }
    }
  }

  async createChapter(documentId: string, title: string): Promise<Chapter> {
    // First, get the document to find the next chapter number
    const docResponse = await axios.get(`${API_URL}/document/${documentId}`, {
      headers: this.getAuthHeader()
    })
    
    const doc = docResponse.data
    const nextChapterNumber = (doc.chapters?.length || 0) + 1
    
    // Create the chapter with the proper endpoint format
    await axios.post(
      `${API_URL}/chapter/${documentId}/${nextChapterNumber}`,
      { 
        title,
        summary: '',
        scenes: []
      },
      { headers: this.getAuthHeader() }
    )
    
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