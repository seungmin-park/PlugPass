import { config, enableAutoUnmount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, vi } from 'vitest'
import type { App, Plugin } from 'vue'
import { TestDiagnostics } from './vueWarnings'
import { monitorConsole } from './consoleDiagnostics'

let unmountComponents: () => void
enableAutoUnmount(cleanup => { unmountComponents = cleanup })

let diagnostics: TestDiagnostics
let originalWarn: typeof console.warn
let originalError: typeof console.error
let originalConsole: Console
let monitoredConsole: Console
let originalPlugins: typeof config.global.plugins

function monitorVue(app: App): void {
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
    set: (handler: App['config']['warnHandler']) => { warnHandler = handler },
  })
  Object.defineProperty(app.config, 'errorHandler', {
    configurable: true,
    get: () => monitoredVueError,
    set: (handler: App['config']['errorHandler']) => { errorHandler = handler },
  })
}

beforeEach(() => {
  diagnostics = new TestDiagnostics()
  originalConsole = console
  originalWarn = console.warn
  originalError = console.error
  monitoredConsole = monitorConsole(originalConsole, diagnostics)
  globalThis.console = monitoredConsole
  originalPlugins = config.global.plugins
  const diagnosticsPlugin: Plugin = { install: monitorVue }
  config.global.plugins = [...originalPlugins, diagnosticsPlugin]
})

afterEach(async () => {
  try {
    unmountComponents()
    await flushPromises()
    if (console !== monitoredConsole) {
      diagnostics.record('monitoring', 'console 진단 수집기를 교체하면 안 됩니다')
    }
    diagnostics.assertEmpty()
  } finally {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
    globalThis.console = originalConsole
    originalConsole.warn = originalWarn
    originalConsole.error = originalError
    config.global.plugins = originalPlugins
  }
})
