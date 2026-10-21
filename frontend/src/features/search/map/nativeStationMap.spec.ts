import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { NativeMapPlugin } from './nativeMapBridge'
import { createNativeStationMap } from './nativeStationMap'

const boundary = vi.hoisted(() => {
  let notify: (event: unknown) => void = () => {}
  const remove = vi.fn(async () => {})
  const plugin = {
    addListener: vi.fn<NativeMapPlugin['addListener']>(async (_event, listener) => { notify = listener; return { remove } }),
    create: vi.fn<NativeMapPlugin['create']>(async () => {}),
    updateLayout: vi.fn<NativeMapPlugin['updateLayout']>(async () => {}),
    updateStations: vi.fn<NativeMapPlugin['updateStations']>(async () => {}),
    selectStation: vi.fn<NativeMapPlugin['selectStation']>(async () => {}),
    moveTo: vi.fn<NativeMapPlugin['moveTo']>(async () => {}),
    showArea: vi.fn<NativeMapPlugin['showArea']>(async () => {}),
    zoom: vi.fn<NativeMapPlugin['zoom']>(async () => {}),
    destroy: vi.fn<NativeMapPlugin['destroy']>(async () => {}),
  }
  return { plugin, remove, emit: (event: unknown) => notify(event) }
})
vi.mock('./nativeMapBridge', () => ({ nativeMap: boundary.plugin }))

function mapContainer(): HTMLElement {
  const element = document.createElement('div')
  document.body.append(element)
  vi.spyOn(element, 'getBoundingClientRect').mockReturnValue(new DOMRect(16, 600, 361, 280))
  return element
}
function callbacks() {
  return { ready: vi.fn(), failed: vi.fn(), selectStation: vi.fn(), areaChanged: vi.fn(), zoomChanged: vi.fn(), interacted: vi.fn() }
}
const center = { latitude: 37.5, longitude: 127 }
const stations = [{ id: 1, name: '충전소 <1>', ...center }]

beforeEach(() => {
  vi.clearAllMocks()
  vi.useFakeTimers()
  vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} })
})
afterEach(() => {
  document.body.replaceChildren()
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('Capacitor 지도 세션', () => {
  it('생성 전에 이벤트를 연결하고 초기 좌표·충전소·문서 위치를 전달한다', async () => {
    const session = await createNativeStationMap(mapContainer(), stations, center, callbacks())
    expect(boundary.plugin.create).toHaveBeenCalledWith({ sessionId: expect.any(String), center, stations,
      layout: { frame: { x: 16, y: 600, width: 361, height: 280 }, clip: { x: 16, y: 600, width: 361, height: 280 }, visible: true, viewportHeight: window.innerHeight } })
    expect(boundary.plugin.addListener.mock.invocationCallOrder[0]).toBeLessThan(boundary.plugin.create.mock.invocationCallOrder[0] ?? 0)
    await session.destroy()
  })

  it('생성 완료 전에 도착한 준비와 실제 영역·확대 상태를 전달한다', async () => {
    const events = callbacks()
    boundary.plugin.create.mockImplementationOnce(async options => { boundary.emit({ sessionId: options.sessionId, type: 'ready' }) })
    const session = await createNativeStationMap(mapContainer(), stations, center, events)
    const sessionId = boundary.plugin.create.mock.calls[0]?.[0].sessionId
    const area = { south: 37.49, north: 37.51, west: 126.99, east: 127.01 }
    const zoom = { canZoomIn: true, canZoomOut: false, description: '지도 확대 수준 7단계' }
    boundary.emit({ sessionId, type: 'settled', area, zoom })
    expect(events.ready).toHaveBeenCalledOnce()
    expect(events.areaChanged).toHaveBeenCalledWith(area)
    expect(events.zoomChanged).toHaveBeenCalledWith(zoom)
    await session.destroy()
  })

  it('사용자의 native 지도 이동은 진행 중 GPS를 취소할 상호작용 이벤트를 전달한다', async () => {
    const events = callbacks()
    const session = await createNativeStationMap(mapContainer(), stations, center, events)
    const sessionId = boundary.plugin.create.mock.calls[0]?.[0].sessionId
    boundary.emit({ sessionId, type: 'moving', userInitiated: true })
    expect(events.interacted).toHaveBeenCalledOnce()
    expect(events.areaChanged).toHaveBeenCalledWith(null)
    events.interacted.mockClear()
    boundary.emit({ sessionId, type: 'moving', userInitiated: false })
    expect(events.interacted).not.toHaveBeenCalled()
    await session.destroy()
  })

  it('선택과 명령은 같은 세션으로 보내고 종료 후 이전 이벤트는 무시한다', async () => {
    const events = callbacks()
    const session = await createNativeStationMap(mapContainer(), stations, center, events)
    const sessionId = boundary.plugin.create.mock.calls[0]?.[0].sessionId
    boundary.emit({ sessionId: 'previous-session', type: 'selected', stationId: 1 })
    boundary.emit({ sessionId, type: 'selected', stationId: 2 })
    expect(events.selectStation).not.toHaveBeenCalled()
    boundary.emit({ sessionId, type: 'selected', stationId: 1 })
    expect(events.selectStation).toHaveBeenCalledWith(1)
    await session.selectStation(1)
    await session.zoomIn()
    await session.zoomOut()
    expect(boundary.plugin.selectStation).toHaveBeenCalledWith({ sessionId, stationId: 1 })
    expect(boundary.plugin.zoom.mock.calls.map(call => call[0])).toEqual([{ sessionId, direction: 'in' }, { sessionId, direction: 'out' }])
    await session.destroy()
    await session.destroy()
    boundary.emit({ sessionId, type: 'selected', stationId: 1 })
    boundary.emit({ sessionId, type: 'failed' })
    expect(events.selectStation).toHaveBeenCalledOnce()
    expect(events.failed).not.toHaveBeenCalled()
    expect(boundary.remove).toHaveBeenCalledOnce()
    expect(boundary.plugin.destroy).toHaveBeenCalledExactlyOnceWith({ sessionId })
  })

  it('생성 실패는 listener와 부분 생성된 native 지도를 정리하고 원래 오류를 전달한다', async () => {
    const failure = new Error('인증 실패')
    boundary.plugin.create.mockRejectedValueOnce(failure)
    await expect(createNativeStationMap(mapContainer(), stations, center, callbacks())).rejects.toBe(failure)
    expect(boundary.remove).toHaveBeenCalledOnce()
    expect(boundary.plugin.destroy).toHaveBeenCalledOnce()
  })

  it('native 명령이 끝나지 않아도 종료는 즉시 자원을 해제한다', async () => {
    let finish: () => void = () => {}
    boundary.plugin.moveTo.mockImplementationOnce(() => new Promise<void>(resolve => { finish = resolve }))
    const session = await createNativeStationMap(mapContainer(), stations, center, callbacks())
    const moving = session.moveTo({ latitude: 37.6, longitude: 127.1 })
    await session.destroy()
    expect(boundary.plugin.destroy).toHaveBeenCalledOnce()
    finish()
    await moving
  })

  it('패널 표시와 문서 이동은 배치를 갱신하고 종료 후에는 더 보내지 않는다', async () => {
    const container = mapContainer()
    const session = await createNativeStationMap(container, stations, center, callbacks())
    vi.mocked(container.getBoundingClientRect).mockReturnValue(new DOMRect(16, 300, 361, 280))
    window.dispatchEvent(new Event('resize'))
    await vi.advanceTimersByTimeAsync(20)
    expect(boundary.plugin.updateLayout).toHaveBeenLastCalledWith(expect.objectContaining({ layout: expect.objectContaining({ frame: { x: 16, y: 300, width: 361, height: 280 } }) }))
    const dialog = document.createElement('section')
    dialog.setAttribute('role', 'dialog')
    document.body.append(dialog)
    await vi.advanceTimersByTimeAsync(20)
    expect(boundary.plugin.updateLayout).toHaveBeenLastCalledWith(expect.objectContaining({ layout: expect.objectContaining({ visible: false }) }))
    await session.destroy()
    boundary.plugin.updateLayout.mockClear()
    dialog.remove()
    window.dispatchEvent(new Event('resize'))
    await vi.advanceTimersByTimeAsync(20)
    expect(boundary.plugin.updateLayout).not.toHaveBeenCalled()
  })

  it('잘못된 native 영역을 검색 위치로 전달하지 않는다', async () => {
    const events = callbacks()
    const session = await createNativeStationMap(mapContainer(), stations, center, events)
    const sessionId = boundary.plugin.create.mock.calls[0]?.[0].sessionId
    boundary.emit({ sessionId, type: 'settled', area: { south: 30, north: 40, west: 120, east: 130 },
      zoom: { canZoomIn: true, canZoomOut: true, description: '지도 확대 수준 5단계' } })
    expect(events.areaChanged).toHaveBeenCalledWith(null)
    await session.destroy()
  })

  it('키보드로 줄어든 실제 표시 높이를 native 잘림 영역에 전달한다', async () => {
    const viewport = Object.assign(new EventTarget(), { height: 300 })
    vi.stubGlobal('visualViewport', viewport)
    const session = await createNativeStationMap(mapContainer(), stations, center, callbacks())
    expect(boundary.plugin.create).toHaveBeenCalledWith(expect.objectContaining({ layout: expect.objectContaining({ viewportHeight: 300 }) }))
    viewport.height = 700
    viewport.dispatchEvent(new Event('resize'))
    await vi.advanceTimersByTimeAsync(20)
    expect(boundary.plugin.updateLayout).toHaveBeenLastCalledWith(expect.objectContaining({ layout: expect.objectContaining({ viewportHeight: 700 }) }))
    await session.destroy()
  })
})
