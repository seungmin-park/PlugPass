import { afterEach, describe, expect, it, vi } from 'vitest'

afterEach(() => {
  document.head.querySelectorAll('script[data-plugpass-kakao-map]').forEach(script => script.remove())
  Reflect.deleteProperty(window, 'kakao')
  vi.restoreAllMocks()
  vi.unstubAllEnvs()
  vi.useRealTimers()
})

describe('카카오 지도 SDK 로딩', () => {
  it('키가 없는 환경에서는 이전 지도 구현을 불러오지 않는다', async () => {
    vi.stubEnv('VITE_KAKAO_JS_KEY', '')
    const { loadStationMap } = await import('./loadStationMap')
    await expect(loadStationMap()).rejects.toThrow('카카오 지도 키')
  })
  it('JavaScript 키로 SDK를 한 번만 요청하고 준비 완료까지 기다린다', async () => {
    vi.resetModules()
    const { loadKakaoMaps } = await import('./kakaoStationMap')
    const first = loadKakaoMaps('test-js-key')
    const second = loadKakaoMaps('test-js-key')
    const script = document.head.querySelector<HTMLScriptElement>('script[data-plugpass-kakao-map]')
    expect(script?.src).toContain('appkey=test-js-key')
    expect(script?.src).toContain('autoload=false')
    expect(document.head.querySelectorAll('script[data-plugpass-kakao-map]')).toHaveLength(1)

    const maps = { load: (ready: () => void) => ready() }
    Object.defineProperty(window, 'kakao', { configurable: true, value: { maps } })
    script?.dispatchEvent(new Event('load'))
    expect(await first).toBe(maps)
    expect(await second).toBe(maps)
  })

  it('다운로드 실패를 호출자에게 알려주고 재시도할 수 있다', async () => {
    vi.resetModules()
    const { loadKakaoMaps } = await import('./kakaoStationMap')
    const first = loadKakaoMaps('test-js-key')
    document.head.querySelector('script[data-plugpass-kakao-map]')?.dispatchEvent(new Event('error'))
    await expect(first).rejects.toThrow('카카오 지도 SDK')
    expect(document.head.querySelectorAll('script[data-plugpass-kakao-map]')).toHaveLength(0)

    const retry = loadKakaoMaps('test-js-key')
    expect(document.head.querySelectorAll('script[data-plugpass-kakao-map]')).toHaveLength(1)
    document.head.querySelector('script[data-plugpass-kakao-map]')?.dispatchEvent(new Event('error'))
    await expect(retry).rejects.toThrow('카카오 지도 SDK')
  })

  it('SDK 준비가 멈춰도 시간 제한 뒤 다음 요청을 새로 시작한다', async () => {
    vi.useFakeTimers()
    vi.resetModules()
    const { loadKakaoMaps } = await import('./kakaoStationMap')
    const first = loadKakaoMaps('test-js-key')
    const firstFailure = expect(first).rejects.toThrow('카카오 지도 SDK')
    Object.defineProperty(window, 'kakao', { configurable: true, value: { maps: { load: () => {} } } })
    document.head.querySelector('script[data-plugpass-kakao-map]')?.dispatchEvent(new Event('load'))
    await vi.advanceTimersByTimeAsync(9_000)
    await firstFailure
    expect(document.head.querySelectorAll('script[data-plugpass-kakao-map]')).toHaveLength(0)
    const retry = loadKakaoMaps('test-js-key')
    expect(document.head.querySelectorAll('script[data-plugpass-kakao-map]')).toHaveLength(1)
    document.head.querySelector('script[data-plugpass-kakao-map]')?.dispatchEvent(new Event('error'))
    await expect(retry).rejects.toThrow('카카오 지도 SDK')
  })

  it('지도 키가 설정되면 지도 선택 경계가 카카오 SDK를 사용한다', async () => {
    vi.stubEnv('VITE_KAKAO_JS_KEY', 'test-js-key')
    const { loadStationMap } = await import('./loadStationMap')
    const pending = loadStationMap()
    await vi.waitFor(() => expect(document.head.querySelector('script[data-plugpass-kakao-map]')).not.toBeNull())
    const maps = { load: (ready: () => void) => ready() }
    Object.defineProperty(window, 'kakao', { configurable: true, value: { maps } })
    document.head.querySelector('script[data-plugpass-kakao-map]')?.dispatchEvent(new Event('load'))
    const createMap = await pending
    expect(createMap.name).toBe('createKakaoStationMap')
  })
})

describe('카카오 지도와 검색 상태 연결', () => {
  it('마커를 선택하면 충전소 ID를 알리고 카드 선택은 지도 중심을 이동시킨다', async () => {
    const setCenter = vi.fn()
    let level = 5
    let gestures: { scrollwheel: boolean; keyboardShortcuts: boolean } | null = null
    class FakeMap {
      constructor(public container: HTMLElement, options: { scrollwheel: boolean; keyboardShortcuts: boolean }) { gestures = options }
      setCenter = setCenter
      getBounds() { return { getSouthWest: () => ({ getLat: () => 37.4, getLng: () => 126.9 }),
        getNorthEast: () => ({ getLat: () => 37.6, getLng: () => 127.1 }) } }
      setBounds() {}
      getLevel() { return level }
      setLevel(nextLevel: number) { level = nextLevel }
    }
    class FakeLatLng { constructor(public latitude: number, public longitude: number) {} }
    class FakeLatLngBounds { constructor(public southWest: FakeLatLng, public northEast: FakeLatLng) {} }
    class FakeOverlay {
      private content: Node
      constructor(options: { content: Node; map: FakeMap }) { this.content = options.content; options.map.container.append(this.content) }
      setMap(target: null) { if (target === null) this.content.parentNode?.removeChild(this.content) }
    }
    Object.defineProperty(window, 'kakao', { configurable: true, value: { maps: {
      Map: FakeMap, LatLng: FakeLatLng, LatLngBounds: FakeLatLngBounds, CustomOverlay: FakeOverlay,
      event: { addListener: vi.fn(), removeListener: vi.fn() },
    } } })
    const { createKakaoStationMap } = await import('./kakaoStationMap')
    const selectStation = vi.fn()
    const tilesLoaded = vi.fn()
    const container = document.createElement('div')
    document.body.append(container)
    const map = createKakaoStationMap(container, [{ id: 7, name: '시청 충전소', latitude: 37.5, longitude: 127 }],
      { latitude: 37.4, longitude: 126.9 }, { selectStation, tilesLoaded, tilesFailed: vi.fn() })
    expect(gestures).toMatchObject({ scrollwheel: true, keyboardShortcuts: true })
    const marker = container.querySelector<HTMLButtonElement>('[aria-label="시청 충전소 지도 마커"]')
    expect(marker).not.toBeNull()
    marker?.click()
    expect(selectStation).toHaveBeenCalledWith(7)
    map.selectStation(7)
    expect(marker?.getAttribute('aria-pressed')).toBe('true')
    expect(setCenter).toHaveBeenCalled()
    expect(map.zoomIn()).toBe(4)
    expect(level).toBe(4)
    expect(map.zoomOut()).toBe(5)
    expect(level).toBe(5)
    level = 1
    expect(map.zoomIn()).toBe(1)
    level = 14
    expect(map.zoomOut()).toBe(14)
    map.destroy()
    expect(container.querySelector('[aria-label="시청 충전소 지도 마커"]')).toBeNull()
    container.remove()
  })
})
