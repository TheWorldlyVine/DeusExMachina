import { Document, Chapter, Scene } from '@/types/document';
import { saveAs } from 'file-saver';
import jsPDF from 'jspdf';
import { Document as DocxDocument, Packer, Paragraph, TextRun, HeadingLevel } from 'docx';

interface ExportOptions {
  includeMetadata?: boolean;
  includeChapterNumbers?: boolean;
  includeSceneBreaks?: boolean;
  fontSize?: number;
  fontFamily?: string;
}

export class ExportService {
  /**
   * Export document as plain text
   */
  static async exportAsTxt(
    document: Document,
    chapters: Chapter[],
    options: ExportOptions = {}
  ): Promise<void> {
    let content = `${document.title}\n${'='.repeat(document.title.length)}\n\n`;
    
    if (document.description) {
      content += `${document.description}\n\n`;
    }

    if (options.includeMetadata) {
      content += `Author: ${document.authorName || 'Unknown'}\n`;
      content += `Created: ${new Date(document.createdAt).toLocaleDateString()}\n`;
      content += `Word Count: ${document.wordCount.toLocaleString()}\n\n`;
      content += '-'.repeat(50) + '\n\n';
    }

    chapters.forEach((chapter, index) => {
      if (options.includeChapterNumbers) {
        content += `Chapter ${chapter.chapterNumber}: ${chapter.title}\n`;
      } else {
        content += `${chapter.title}\n`;
      }
      content += '-'.repeat(chapter.title.length) + '\n\n';

      chapter.scenes.forEach((scene, sceneIndex) => {
        if (scene.title) {
          content += `${scene.title}\n\n`;
        }
        
        content += scene.content + '\n\n';
        
        if (options.includeSceneBreaks && sceneIndex < chapter.scenes.length - 1) {
          content += '* * *\n\n';
        }
      });

      if (index < chapters.length - 1) {
        content += '\n\n';
      }
    });

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    saveAs(blob, `${document.title}.txt`);
  }

  /**
   * Export document as PDF
   */
  static async exportAsPdf(
    document: Document,
    chapters: Chapter[],
    options: ExportOptions = {}
  ): Promise<void> {
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
    });

    const pageWidth = pdf.internal.pageSize.width;
    const pageHeight = pdf.internal.pageSize.height;
    const margin = 20;
    const lineHeight = options.fontSize || 12;
    let yPosition = margin;

    // Title page
    pdf.setFontSize(24);
    pdf.text(document.title, pageWidth / 2, yPosition, { align: 'center' });
    yPosition += lineHeight * 2;

    if (document.description) {
      pdf.setFontSize(14);
      const descLines = pdf.splitTextToSize(document.description, pageWidth - 2 * margin);
      pdf.text(descLines, pageWidth / 2, yPosition, { align: 'center' });
      yPosition += descLines.length * lineHeight;
    }

    if (options.includeMetadata) {
      yPosition = pageHeight - 50;
      pdf.setFontSize(10);
      pdf.text(`Author: ${document.authorName || 'Unknown'}`, margin, yPosition);
      yPosition += lineHeight;
      pdf.text(`Created: ${new Date(document.createdAt).toLocaleDateString()}`, margin, yPosition);
      yPosition += lineHeight;
      pdf.text(`Word Count: ${document.wordCount.toLocaleString()}`, margin, yPosition);
    }

    // Add new page for content
    pdf.addPage();
    yPosition = margin;

    // Chapters
    pdf.setFontSize(options.fontSize || 12);
    
    chapters.forEach((chapter, chapterIndex) => {
      // Chapter title
      pdf.setFontSize(16);
      const chapterTitle = options.includeChapterNumbers
        ? `Chapter ${chapter.chapterNumber}: ${chapter.title}`
        : chapter.title;
      
      if (yPosition + lineHeight * 2 > pageHeight - margin) {
        pdf.addPage();
        yPosition = margin;
      }
      
      pdf.text(chapterTitle, margin, yPosition);
      yPosition += lineHeight * 2;
      pdf.setFontSize(options.fontSize || 12);

      // Scenes
      chapter.scenes.forEach((scene, sceneIndex) => {
        if (scene.title) {
          pdf.setFontSize(14);
          if (yPosition + lineHeight > pageHeight - margin) {
            pdf.addPage();
            yPosition = margin;
          }
          pdf.text(scene.title, margin, yPosition);
          yPosition += lineHeight * 1.5;
          pdf.setFontSize(options.fontSize || 12);
        }

        // Scene content
        const lines = pdf.splitTextToSize(scene.content, pageWidth - 2 * margin);
        
        lines.forEach((line: string) => {
          if (yPosition + lineHeight > pageHeight - margin) {
            pdf.addPage();
            yPosition = margin;
          }
          pdf.text(line, margin, yPosition);
          yPosition += lineHeight;
        });

        if (options.includeSceneBreaks && sceneIndex < chapter.scenes.length - 1) {
          yPosition += lineHeight;
          pdf.text('* * *', pageWidth / 2, yPosition, { align: 'center' });
          yPosition += lineHeight * 2;
        }
      });

      // Add extra space between chapters
      if (chapterIndex < chapters.length - 1) {
        yPosition += lineHeight * 2;
      }
    });

    pdf.save(`${document.title}.pdf`);
  }

  /**
   * Export document as DOCX
   */
  static async exportAsDocx(
    document: Document,
    chapters: Chapter[],
    options: ExportOptions = {}
  ): Promise<void> {
    const doc = new DocxDocument({
      sections: [
        {
          properties: {},
          children: [
            // Title
            new Paragraph({
              text: document.title,
              heading: HeadingLevel.TITLE,
              alignment: 'center',
              spacing: { after: 400 },
            }),

            // Description
            ...(document.description
              ? [
                  new Paragraph({
                    text: document.description,
                    alignment: 'center',
                    spacing: { after: 600 },
                  }),
                ]
              : []),

            // Metadata
            ...(options.includeMetadata
              ? [
                  new Paragraph({
                    text: `Author: ${document.authorName || 'Unknown'}`,
                    spacing: { before: 1200 },
                  }),
                  new Paragraph({
                    text: `Created: ${new Date(document.createdAt).toLocaleDateString()}`,
                  }),
                  new Paragraph({
                    text: `Word Count: ${document.wordCount.toLocaleString()}`,
                    spacing: { after: 1200 },
                  }),
                ]
              : []),

            // Page break before content
            new Paragraph({
              text: '',
              pageBreakBefore: true,
            }),

            // Chapters
            ...chapters.flatMap((chapter, chapterIndex) => [
              // Chapter title
              new Paragraph({
                text: options.includeChapterNumbers
                  ? `Chapter ${chapter.chapterNumber}: ${chapter.title}`
                  : chapter.title,
                heading: HeadingLevel.HEADING_1,
                spacing: { before: 400, after: 400 },
              }),

              // Scenes
              ...chapter.scenes.flatMap((scene, sceneIndex) => [
                // Scene title
                ...(scene.title
                  ? [
                      new Paragraph({
                        text: scene.title,
                        heading: HeadingLevel.HEADING_2,
                        spacing: { before: 200, after: 200 },
                      }),
                    ]
                  : []),

                // Scene content paragraphs
                ...scene.content.split('\n\n').map(
                  (paragraph) =>
                    new Paragraph({
                      text: paragraph,
                      spacing: { after: 200 },
                    })
                ),

                // Scene break
                ...(options.includeSceneBreaks && sceneIndex < chapter.scenes.length - 1
                  ? [
                      new Paragraph({
                        text: '* * *',
                        alignment: 'center',
                        spacing: { before: 400, after: 400 },
                      }),
                    ]
                  : []),
              ]),

              // Chapter break
              ...(chapterIndex < chapters.length - 1
                ? [
                    new Paragraph({
                      text: '',
                      pageBreakBefore: true,
                    }),
                  ]
                : []),
            ]),
          ],
        },
      ],
    });

    const blob = await Packer.toBlob(doc);
    saveAs(blob, `${document.title}.docx`);
  }

  /**
   * Export document as EPUB (basic implementation)
   */
  static async exportAsEpub(
    document: Document,
    chapters: Chapter[],
    options: ExportOptions = {}
  ): Promise<void> {
    // This is a simplified EPUB export - for production, use a proper EPUB library
    const epubContent = {
      title: document.title,
      author: document.authorName || 'Unknown',
      description: document.description,
      chapters: chapters.map((chapter) => ({
        title: chapter.title,
        content: chapter.scenes.map((scene) => scene.content).join('\n\n'),
      })),
    };

    // For now, export as JSON - in production, use proper EPUB generator
    const blob = new Blob([JSON.stringify(epubContent, null, 2)], {
      type: 'application/json',
    });
    saveAs(blob, `${document.title}.json`);
    
    // Show message to user
    console.warn('EPUB export is not fully implemented. Exported as JSON instead.');
  }
}