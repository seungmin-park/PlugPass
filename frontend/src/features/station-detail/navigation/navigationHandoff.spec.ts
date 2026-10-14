import { describe, expect, it, vi } from 'vitest'
import { scheduleIosInstallFallback } from './navigationHandoff'

describe('iPhone 카카오맵 연결 대안', () => {
  it('앱 전환 없이 페이지가 남으면 앱스토어를 연다', () => {
    vi.useFakeTimers()
    const openStore = vi.fn()
    try {
      scheduleIosInstallFallback(document, 'https://apps.apple.com/app/id304608425', openStore)
      vi.advanceTimersByTime(3000)
      expect(openStore).toHaveBeenCalledExactlyOnceWith('https://apps.apple.com/app/id304608425')
    } finally { vi.useRealTimers() }
  })

  it('앱 전환으로 페이지가 숨겨지면 앱스토어로 보내지 않는다', () => {
    vi.useFakeTimers()
    const openStore = vi.fn()
    const originalHidden = Object.getOwnPropertyDescriptor(document, 'hidden')
    try {
      scheduleIosInstallFallback(document, 'https://apps.apple.com/app/id304608425', openStore)
      Object.defineProperty(document, 'hidden', { configurable: true, value: true })
      document.dispatchEvent(new Event('visibilitychange'))
      vi.advanceTimersByTime(3000)
      expect(openStore).not.toHaveBeenCalled()
    } finally {
      if (originalHidden) Object.defineProperty(document, 'hidden', originalHidden)
      else Reflect.deleteProperty(document, 'hidden')
      vi.useRealTimers()
    }
  })

  it('페이지를 떠난 뒤에는 늦은 앱스토어 이동을 실행하지 않는다', () => {
    vi.useFakeTimers()
    const openStore = vi.fn()
    try {
      scheduleIosInstallFallback(document, 'https://apps.apple.com/app/id304608425', openStore)
      window.dispatchEvent(new Event('pagehide'))
      vi.advanceTimersByTime(3000)
      expect(openStore).not.toHaveBeenCalled()
    } finally { vi.useRealTimers() }
  })
})
