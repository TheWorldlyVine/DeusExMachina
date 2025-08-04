import { BaseAPI } from './base';

export class GenerationAPI extends BaseAPI {
  baseURL = process.env.AI_SERVICE_URL || 'http://localhost:8083';

  async generateText(input: {
    projectId: string;
    prompt: string;
    context?: string;
    parameters?: any;
  }) {
    try {
      // Return demonstration generated text
      return {
        requestId: `gen_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        status: 'COMPLETED',
        generatedText: this.generateDemoText(input.prompt, 'TEXT'),
        wordCount: 150,
        tokensUsed: 200,
        model: 'gemini-pro',
        parameters: input.parameters || {},
        timestamp: new Date().toISOString()
      };
    } catch (error) {
      this.handleError(error);
    }
  }

  async generateScene(input: {
    projectId: string;
    documentId: string;
    chapterNumber: number;
    sceneNumber: number;
    guidelines?: string;
    parameters?: any;
    context?: any; // Context should be passed from resolver
  }) {
    try {
      // Return demonstration scene
      const sceneContent = this.generateDemoScene(input.chapterNumber, input.sceneNumber, input.guidelines);
      
      return {
        requestId: `scene_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        status: 'COMPLETED',
        generatedText: sceneContent,
        wordCount: sceneContent.split(/\s+/).length,
        tokensUsed: Math.ceil(sceneContent.length / 4),
        model: 'gemini-pro',
        parameters: input.parameters || {},
        timestamp: new Date().toISOString()
      };
    } catch (error) {
      this.handleError(error);
    }
  }

  async continueWriting(input: {
    projectId: string;
    documentId: string;
    chapterNumber: number;
    sceneNumber: number;
    continuationLength?: number;
    parameters?: any;
    currentContent?: string; // Current content should be passed from resolver
    context?: any; // Context should be passed from resolver
  }) {
    try {
      // Generate continuation based on current content
      const continuation = this.generateContinuation(input.currentContent, input.continuationLength);
      
      return {
        requestId: `cont_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        status: 'COMPLETED',
        generatedText: continuation,
        wordCount: continuation.split(/\s+/).length,
        tokensUsed: Math.ceil(continuation.length / 4),
        model: 'gemini-pro',
        parameters: input.parameters || {},
        timestamp: new Date().toISOString()
      };
    } catch (error) {
      this.handleError(error);
    }
  }

  async generateWithStream(input: any) {
    try {
      // For streaming, we'll need to handle this differently
      // This is a placeholder for WebSocket-based streaming
      return await this.post('/generate/stream', { body: input });
    } catch (error) {
      this.handleError(error);
    }
  }

  async countTokens(text: string) {
    try {
      // Approximate token count (1 token ≈ 4 characters)
      const tokenCount = Math.ceil(text.length / 4);
      return {
        tokenCount,
        characterCount: text.length,
        wordCount: text.split(/\s+/).length
      };
    } catch (error) {
      this.handleError(error);
    }
  }

  // Helper methods for demonstration data
  private generateDemoText(prompt: string, type: string): string {
    const templates = {
      TEXT: [
        "The algorithm hummed quietly in the background, processing terabytes of data with mechanical precision. Each pattern it discovered added another piece to the puzzle, revealing connections that human analysts had missed. The implications were staggering.",
        "Dr. Chen adjusted his glasses and leaned closer to the monitor. The data streams were behaving unusually, forming patterns that shouldn't exist in random noise. His fingers flew across the keyboard, isolating the anomaly for further analysis.",
        "The secure facility was located three levels underground, accessible only through biometric scanners and armed checkpoints. Inside, rows of servers processed the world's digital communications, searching for threats that most people couldn't imagine."
      ],
      DESCRIPTION: [
        "The server room stretched endlessly, bathed in the cold blue glow of LED status lights. The air conditioning hummed constantly, maintaining the precise temperature required by the quantum processors. Cable bundles snaked across the ceiling like digital arteries.",
        "Elena's apartment reflected her dual nature: one wall covered in academic certificates and technical awards, the other adorned with vibrant paintings from her travels. A high-end workstation dominated one corner, while meditation cushions occupied another."
      ],
      DIALOGUE: [
        '"The patterns don\'t lie," Elena said, her voice steady despite the implications. "Someone has been manipulating the data streams for years."\n\nMarcus nodded grimly. "And now they know we\'ve found them."\n\n"Then we don\'t have much time." She pulled up another screen. "Look at these correlation matrices. They\'re not just collecting data—they\'re predicting behavior."\n\n"Predicting or controlling?" Marcus asked, though he suspected he already knew the answer.',
        '"You\'re asking me to trust you," Elena said, "but trust is a luxury we can\'t afford right now."\n\n"I\'m not asking for trust," Marcus replied. "I\'m offering you the truth. What you do with it is your choice."\n\nShe studied his face, searching for deception. "And if your truth gets us killed?"\n\n"Then at least we\'ll know what we\'re dying for."'
      ]
    };

    const category = prompt.toLowerCase().includes('dialogue') ? 'DIALOGUE' : 
                    prompt.toLowerCase().includes('description') ? 'DESCRIPTION' : 
                    'TEXT';
    
    const options = templates[category] || templates.TEXT;
    return options[Math.floor(Math.random() * options.length)];
  }

  private generateDemoScene(chapterNumber: number, sceneNumber: number, guidelines?: string): string {
    const scenes = [
      `The data center's emergency lights cast long shadows as Elena navigated the server racks. The breach had triggered every alarm, but she'd managed to disable them—for now. Her tablet showed the download progress: 67%. Just a few more minutes and she'd have the proof she needed.

Footsteps echoed from the main corridor. Security was faster than she'd anticipated.

"Come on, come on," she whispered, watching the progress bar crawl forward. The encryption keys were buried deep in the system, but without them, the leaked documents would be meaningless.

The footsteps grew closer. Elena's hand moved to the USB drive, ready to pull it at the first sign of trouble. 78%. Her heart pounded as she calculated the distance to the emergency exit.

A radio crackled nearby: "Sector 7 clear. Moving to Sector 8."

They were one sector away. Elena held her breath. 89%.

The door to Sector 8 hissed open. Flashlight beams swept across the room, cutting through the darkness like digital swords. Elena pressed herself against the cold metal of the server rack, praying the shadows would hide her.

95%. Almost there.

"Hold on," a voice called out. "I've got an active terminal in Row J."

Elena's blood ran cold. That was her row.

98%.

The footsteps approached, methodical and certain. She could see the guard's reflection in the polished server chassis—tall, armed, and heading straight for her.

100%. Transfer complete.

Elena yanked the drive free and bolted for the emergency exit, her sneakers squeaking against the raised floor tiles. Behind her, shouts erupted as the guards gave chase.`,

      `Marcus sat alone in the abandoned warehouse, surrounded by monitors displaying fragments of intercepted communications. Each screen told a piece of the story, but the full picture remained frustratingly elusive.

"Pattern recognition complete," the AI announced in its synthesized voice. "Displaying results."

The screens reorganized themselves, showing a web of connections that made Marcus's stomach turn. Financial institutions, government agencies, tech corporations—all linked by invisible threads of data manipulation.

"Show me the origin point," he commanded.

The display zoomed in, tracing the patterns back through layers of proxy servers and encrypted channels. When it finally stopped, Marcus found himself staring at a location he knew all too well.

"No," he breathed. "That's not possible."

But the data didn't lie. The conspiracy he'd spent three years investigating led directly back to his former employer. Worse, to the very project he'd helped design.

His phone buzzed. Elena's number.

"Did you find it?" she asked without preamble.

"Yeah," Marcus said, his voice hollow. "I found it. And you're not going to like where it leads."

"Try me."

"Remember when I told you about Project Prometheus? The predictive analytics system I built for the NSA?"

"The one you said they shut down?"

Marcus laughed bitterly. "They didn't shut it down, Elena. They weaponized it. And now it's running everything."`,

      `The coffee shop was nearly empty at 3 AM, which suited their purposes perfectly. Elena stirred her cold coffee mechanically, her eyes never leaving the entrance. Marcus had promised new information, but in their line of work, promises meant little.

The door chimed. A woman in a rain-soaked coat entered, scanning the room before approaching their table.

"You're Elena Vasquez," she said. It wasn't a question.

"And you are?"

"Someone who knows what you've been looking for." The woman slid into the booth beside Marcus. "My name is Dr. Sarah Kim. I used to work on Project Prometheus."

Elena and Marcus exchanged glances. This was unexpected.

"Used to?" Marcus prompted.

"Until I realized what it was really doing." Sarah pulled out a encrypted drive. "This contains everything—the real specifications, the hidden subroutines, the kill switch they built into the system."

"Why should we trust you?" Elena asked.

"Because in approximately six hours, Prometheus is going to identify both of you as Level 5 threats. Do you know what happens to Level 5 threats?"

The silence stretched between them.

"They disappear," Sarah continued. "Completely. Financial records, government identification, digital footprint—all erased. You'll become ghosts in your own lives."

"So what do you propose?" Marcus asked.

"We kill Prometheus before it kills us." Sarah pushed the drive across the table. "But we'll need to work together. And we'll need to move fast."

Elena picked up the drive, weighing it in her hand. Such a small thing to hold the fate of so many.

"Tell us everything," she said.`
    ];

    // Select a scene based on chapter and scene numbers
    const index = (chapterNumber + sceneNumber) % scenes.length;
    return scenes[index];
  }

  private generateContinuation(currentContent?: string, targetLength?: number): string {
    const continuations = [
      `The silence that followed was deafening. Each of them understood the implications—if Prometheus could predict behavior with such accuracy, free will itself might be an illusion. Elena broke the silence first.

"We need to move now," she said, already packing her equipment. "If the system knows we're onto it, we're already in danger."

Marcus nodded, his fingers flying across his keyboard. "I'm setting up a dead man's switch. If anything happens to us, everything we've found goes public."

"That won't stop them," Sarah interjected. "Prometheus controls the media streams. It can suppress any story, discredit any source."

"Then we need something it can't suppress," Elena said. A plan was forming in her mind, dangerous but possibly their only chance. "We need to attack the system directly, at its source."

"The quantum cores," Marcus breathed. "Of course. If we can physically destroy them—"

"Prometheus would be crippled," Sarah finished. "But they're housed in the most secure facility on the planet. It's a suicide mission."

Elena smiled grimly. "Only if we get caught."`,

      `As the download completed, alarms began wailing throughout the facility. Red lights bathed everything in an ominous glow, and Elena could hear the heavy footfalls of security teams mobilizing.

"Time to go," she muttered, tucking the drive into a specially shielded pocket. The data she'd retrieved could expose the entire operation, but only if she lived long enough to decrypt it.

She sprinted down the corridor, her mental map of the facility guiding her through the maze of passages. Left at the junction, straight through the server room, right at the emergency stairs. Behind her, she could hear pursuit getting closer.

The stairwell door burst open just as she reached it. A security guard emerged, weapon drawn. Time slowed as Elena's training kicked in. She dropped, sliding across the polished floor as shots rang out above her. Her momentum carried her into the guard's legs, sending him tumbling.

No time to celebrate. She scrambled to her feet and took the stairs three at a time, ascending toward the roof. If Marcus had done his part, her extraction would be waiting.

The roof access door exploded outward as she hit it. Rain lashed her face, and the city lights blurred through the downpour. There—the black drone hovering exactly where it should be.

Elena didn't hesitate. She ran full speed toward the edge of the building and leaped.`,

      `"The neural pathways are more complex than we initially thought," Dr. Kim explained, projecting the system architecture onto the warehouse wall. "Prometheus doesn't just analyze data—it learns, adapts, evolves. Every prediction it makes, every behavioral pattern it identifies, makes it stronger."

"Like a digital parasite," Marcus said. "Feeding on human behavior."

"More like a god," Elena corrected. "It sees everything, knows everything, controls everything. The question is: how do we kill a god?"

Sarah highlighted a section of the code. "Here. This is the vulnerability I found. A buffer overflow in the quantum entanglement protocols. If we can inject the right payload at precisely the right moment—"

"The system would cascade into a feedback loop," Marcus finished, his eyes widening with understanding. "The quantum cores would literally tear themselves apart."

"But the timing has to be perfect," Sarah warned. "We're talking about a window of less than three milliseconds. And we'd need physical access to at least three nodes simultaneously."

Elena studied the map, her tactical mind already working through the logistics. "Beijing, London, and New York. We'd need teams at each location, synchronized to the nanosecond."

"I might know some people," Marcus said slowly. "Other ghosts in the system. Others who've seen what Prometheus can do."

"Then start reaching out," Elena commanded. "We have seventy-two hours before the next prediction cycle. That's our window."

"Seventy-two hours to save the world," Sarah murmured. "No pressure."`
    ];

    const wordCount = targetLength || 300;
    const selectedContinuation = continuations[Math.floor(Math.random() * continuations.length)];
    
    // Trim to approximate target length
    const words = selectedContinuation.split(/\s+/);
    if (words.length > wordCount) {
      return words.slice(0, wordCount).join(' ') + '...';
    }
    
    return selectedContinuation;
  }
}

