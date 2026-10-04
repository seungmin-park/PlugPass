import { describe, expect, it, vi } from 'vitest'
import { monitorConsole } from './consoleDiagnostics'
import { TestDiagnostics } from './vueWarnings'

describe('console 진단 감시 수명', () => {
  it('실제 출력을 유지하며 경고를 기록한다', () => {
    const source = { warn: vi.fn(), error: vi.fn() }
    const diagnostics = new TestDiagnostics()
    const monitored = monitorConsole(source, diagnostics)
    monitored.warn('실제 경고')
    expect(source.warn).toHaveBeenCalledWith('실제 경고')
    expect(() => diagnostics.assertEmpty()).toThrow('[console.warn] 실제 경고')
  })
  it('mock을 복원해도 이미 발생한 진단을 잃지 않는다', () => {
    const source = { warn: vi.fn(), error: vi.fn() }
    const diagnostics = new TestDiagnostics()
    const monitored = monitorConsole(source, diagnostics)
    const mock = vi.spyOn(monitored, 'error').mockImplementation(() => {})
    monitored.error('숨기려 한 오류')
    mock.mockRestore()
    expect(() => diagnostics.assertEmpty()).toThrow('console.error')
  })
})
