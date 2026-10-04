import { describe, expect, it } from 'vitest'
import { TestDiagnostics } from './vueWarnings'

describe('테스트 진단 수집', () => {
  it('진단이 없으면 통과한다', () => {
    expect(() => new TestDiagnostics().assertEmpty()).not.toThrow()
  })
  it('진단의 종류와 내용을 담아 실패한다', () => {
    const diagnostics = new TestDiagnostics()
    diagnostics.record('Vue warn', '잘못된 props')
    diagnostics.record('console.error', '외부 오류')
    expect(() => diagnostics.assertEmpty()).toThrow('[Vue warn] 잘못된 props\n[console.error] 외부 오류')
  })
  it('다음 테스트의 수집 상태와 공유하지 않는다', () => {
    const previous = new TestDiagnostics()
    previous.record('Vue warn', '이전 테스트')
    const current = new TestDiagnostics()
    expect(() => current.assertEmpty()).not.toThrow()
    expect(() => previous.assertEmpty()).toThrow('이전 테스트')
  })
})
