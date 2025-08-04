import TurndownService from 'turndown'

// Initialize Turndown for HTML to Markdown conversion
const turndownService = new TurndownService({
  headingStyle: 'atx',
  codeBlockStyle: 'fenced',
})

// Configure turndown to preserve chapter and scene structure
turndownService.addRule('preserveChapterHeadings', {
  filter: ['h1'],
  replacement: function(content: string) {
    // Check if this is a chapter heading
    if (content.match(/Chapter \d+:/)) {
      return `# ${content}\n\n`
    }
    return `# ${content}\n\n`
  }
})

turndownService.addRule('preserveSceneHeadings', {
  filter: ['h2'],
  replacement: function(content: string) {
    return `## ${content}\n\n`
  }
})

/**
 * Convert HTML content from TipTap to Markdown for backend storage
 */
export function htmlToMarkdown(html: string): string {
  // Remove empty paragraphs that TipTap might create
  const cleanHtml = html.replace(/<p>\s*<\/p>/g, '')
  
  // Convert to markdown
  const markdown = turndownService.turndown(cleanHtml)
  
  // Clean up excessive newlines
  return markdown.replace(/\n{3,}/g, '\n\n').trim()
}

/**
 * Convert Markdown content from backend to HTML for TipTap editor
 */
export function markdownToHtml(markdown: string): string {
  // This is a simple conversion - for a production app, you might want to use
  // a proper markdown parser like marked or markdown-it
  
  let html = markdown
  
  // Convert headings
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
  
  // Convert bold and italic
  html = html.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  html = html.replace(/_(.+?)_/g, '<em>$1</em>')
  
  // Convert lists
  html = html.replace(/^\* (.+)$/gm, '<li>$1</li>')
  html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
  
  // Wrap consecutive list items
  html = html.replace(/(<li>.*<\/li>\n?)+/g, (match) => {
    const isOrdered = match.includes('1.')
    return isOrdered ? `<ol>${match}</ol>` : `<ul>${match}</ul>`
  })
  
  // Convert blockquotes
  html = html.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
  
  // Convert horizontal rules
  html = html.replace(/^---$/gm, '<hr>')
  
  // Convert paragraphs (lines that aren't already HTML tags)
  const lines = html.split('\n')
  const processedLines = lines.map(line => {
    const trimmed = line.trim()
    if (trimmed && !trimmed.startsWith('<') && !trimmed.endsWith('>')) {
      return `<p>${trimmed}</p>`
    }
    return line
  })
  
  html = processedLines.join('\n')
  
  // Clean up empty lines
  html = html.replace(/\n{2,}/g, '\n').trim()
  
  return html
}

/**
 * Extract chapter and scene structure from content
 */
export function extractStructure(content: string, isHtml: boolean = true) {
  const text = isHtml ? htmlToMarkdown(content) : content
  const lines = text.split('\n')
  
  const structure = {
    chapters: [] as Array<{
      number: number
      title: string
      startLine: number
      scenes: Array<{
        number: number
        title: string
        startLine: number
      }>
    }>
  }
  
  let currentChapter: typeof structure.chapters[0] | null = null
  let sceneNumber = 0
  
  lines.forEach((line, index) => {
    // Check for chapter heading
    const chapterMatch = line.match(/^# Chapter (\d+): (.+)$/)
    if (chapterMatch) {
      currentChapter = {
        number: parseInt(chapterMatch[1]),
        title: chapterMatch[2],
        startLine: index,
        scenes: []
      }
      structure.chapters.push(currentChapter)
      sceneNumber = 0
    }
    // Check for scene heading
    else if (line.startsWith('## ') && currentChapter) {
      sceneNumber++
      currentChapter.scenes.push({
        number: sceneNumber,
        title: line.substring(3),
        startLine: index
      })
    }
  })
  
  return structure
}