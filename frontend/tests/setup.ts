import { config, enableAutoUnmount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, vi } from 'vitest'
import type { Plugin } from 'vue'
import { monitorVueApp } from './vueAppDiagnostics'
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


beforeEach(() => {
  diagnostics = new TestDiagnostics()
  originalConsole = console
  originalWarn = console.warn
  originalError = console.error
  monitoredConsole = monitorConsole(originalConsole, diagnostics)
  globalThis.console = monitoredConsole
  originalPlugins = config.global.plugins
  const diagnosticsPlugin: Plugin = { install: app => monitorVueApp(app, diagnostics, originalWarn, originalError) }
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
