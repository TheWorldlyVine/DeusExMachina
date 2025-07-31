import { DefaultReporter } from 'vitest/reporters'

export default class QuietReporter extends DefaultReporter {
  constructor(options: any) {
    super({
      ...options,
      verbose: false,
      hideSkipped: true,
    })
  }

  onTaskUpdate() {
    // Suppress task update logs
  }

  onWatcherRerun() {
    // Suppress watcher rerun logs
  }

  onCollected() {
    // Suppress collection logs
  }

  onFinished(files = this.ctx.state.getFiles(), errors = this.ctx.state.getUnhandledErrors()) {
    // Only show summary and errors
    super.onFinished(files, errors)
  }
}