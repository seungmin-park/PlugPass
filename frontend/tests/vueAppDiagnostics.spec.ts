import { createApp } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import { TestDiagnostics } from './vueWarnings'
import { monitorVueApp } from './vueAppDiagnostics'

describe('Vue 진단 handler의 재설정', () => {
  it('같은 error handler를 다시 대입해도 재귀 없이 실제 오류를 수집한다', () => {
    const app = createApp({})
    const diagnostics = new TestDiagnostics()
    const originalError = vi.fn<Console['error']>()
    monitorVueApp(app, diagnostics, vi.fn(), originalError)
    const errorHandler = app.config.errorHandler
    if (!errorHandler) throw new Error('오류 handler 필요')
    app.config.errorHandler = errorHandler
    expect(() => app.config.errorHandler?.(new Error('실제 렌더 오류'), null, 'render')).not.toThrow()
    expect(originalError).toHaveBeenCalledTimes(1)
    expect(() => diagnostics.assertEmpty()).toThrow('실제 렌더 오류')
  })
  it('같은 warn handler를 다시 대입해도 경고를 누락하거나 재귀 호출하지 않는다', () => {
    const app = createApp({})
    const diagnostics = new TestDiagnostics()
    const originalWarn = vi.fn<Console['warn']>()
    monitorVueApp(app, diagnostics, originalWarn, vi.fn())
    const warnHandler = app.config.warnHandler
    if (!warnHandler) throw new Error('경고 handler 필요')
    app.config.warnHandler = warnHandler
    expect(() => app.config.warnHandler?.('실제 Vue 경고', null, 'trace')).not.toThrow()
    expect(originalWarn).toHaveBeenCalledTimes(1)
    expect(() => diagnostics.assertEmpty()).toThrow('실제 Vue 경고')
  })
})
