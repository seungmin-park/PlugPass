import type { App } from 'vue'
import type { TestDiagnostics } from './vueWarnings'

export function monitorVueApp(app: App, diagnostics: TestDiagnostics, originalWarn: Console['warn'], originalError: Console['error']): void {
  let warnHandler = app.config.warnHandler
  let errorHandler = app.config.errorHandler
  const monitoredVueWarn: NonNullable<App['config']['warnHandler']> = (message, instance, trace) => {
    diagnostics.record('Vue warn', message + trace)
    if (warnHandler) warnHandler(message, instance, trace)
    else originalWarn(`[Vue warn] ${message}${trace}`)
  }
  const monitoredVueError: NonNullable<App['config']['errorHandler']> = (error, instance, info) => {
    diagnostics.record('Vue error', `${String(error)} (${info})`)
    if (errorHandler) errorHandler(error, instance, info)
    else originalError(error)
  }
  // A later custom handler can forward diagnostics but cannot replace monitoring.
  Object.defineProperty(app.config, 'warnHandler', {
    configurable: true,
    get: () => monitoredVueWarn,
    set: (handler: App['config']['warnHandler']) => { if (handler !== monitoredVueWarn) warnHandler = handler },
  })
  Object.defineProperty(app.config, 'errorHandler', {
    configurable: true,
    get: () => monitoredVueError,
    set: (handler: App['config']['errorHandler']) => { if (handler !== monitoredVueError) errorHandler = handler },
  })
}
