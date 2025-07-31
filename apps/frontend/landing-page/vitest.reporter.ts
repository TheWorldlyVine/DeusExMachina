import type { Reporter, Vitest, File } from 'vitest'
import { DefaultReporter } from 'vitest/reporters'

export default class QuietReporter extends DefaultReporter implements Reporter {
  ctx!: Vitest
  
  constructor(options?: Record<string, unknown>) {
    super({
      ...options,
      verbose: false,
      hideSkipped: true,
    })
  }

  onTaskUpdate(): void {
    // Suppress task update logs
  }

  onWatcherRerun(): void {
    // Suppress watcher rerun logs
  }

  onCollected(): void {
    // Suppress collection logs
  }

  onFinished(files?: File[], errors?: unknown[]): void {
    // Only show summary and errors
    const finalFiles = files || this.ctx.state.getFiles()
    const finalErrors = errors || this.ctx.state.getUnhandledErrors()
    super.onFinished(finalFiles, finalErrors)
  }
}