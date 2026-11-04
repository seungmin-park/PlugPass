import { afterEach, expect, it, vi } from 'vitest'

vi.mock('@capacitor/core', () => ({ Capacitor: { getPlatform: () => 'ios' } }))
vi.mock('./nativeStationMap', () => ({ createNativeStationMap: vi.fn() }))
afterEach(() => vi.unstubAllEnvs())

it('iOS 앱은 JavaScript 키 없이 저장소 소유 native 지도를 선택한다', async () => {
  vi.stubEnv('VITE_KAKAO_JS_KEY', '')
  const { loadStationMap } = await import('./loadStationMap')
  const { createNativeStationMap } = await import('./nativeStationMap')
  await expect(loadStationMap()).resolves.toBe(createNativeStationMap)
})
