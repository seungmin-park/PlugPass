import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, expect, it, vi } from 'vitest'
import StationMap from './StationMap.vue'
import * as loader from '../map/loadStationMap'
import type { StationMapCallbacks, StationMapSession } from '../map/stationMap'

class Deferred<T> {
  resolve!: (value: T) => void
  reject!: (reason: Error) => void
  readonly promise = new Promise<T>((resolve, reject) => { this.resolve = resolve; this.reject = reject })
}

// Only the external SDK boundary is controlled. Vue owns the real lifecycle and selection.
class ControlledMap implements StationMapSession {
  zoomIn = vi.fn(async () => {})
  zoomOut = vi.fn(async () => {})
  selectStation = vi.fn<StationMapSession['selectStation']>(async () => {})
  updateStations = vi.fn<StationMapSession['updateStations']>(async () => {})
  moveTo = vi.fn<StationMapSession['moveTo']>(async () => {})
  showArea = vi.fn<StationMapSession['showArea']>(async () => {})
  destroy = vi.fn(async () => {})
}

afterEach(() => vi.useRealTimers())

it('생성 중 화면을 떠나면 늦게 반환한 지도도 해제하고 이벤트를 무시한다', async () => {
  const creation = new Deferred<StationMapSession>()
  const map = new ControlledMap()
  let events!: StationMapCallbacks
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(async (_container, _stations, _center, callbacks) => {
    events = callbacks
    return creation.promise
  })
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  wrapper.unmount()
  creation.resolve(map)
  await flushPromises()
  events.selectStation(7)
  events.areaChanged?.({ south: 37.4, north: 37.6, west: 126.9, east: 127.1 })
  expect(map.destroy).toHaveBeenCalledTimes(1)
  expect(map.selectStation).not.toHaveBeenCalled()
  expect(wrapper.emitted('select')).toBeUndefined()
  expect(wrapper.emitted('area-change')).toBeUndefined()
})

it('지도 생성 중 바뀐 충전소와 중심·선택을 준비된 지도에 전달한다', async () => {
  const creation = new Deferred<StationMapSession>()
  const map = new ControlledMap()
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(async () => creation.promise)
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  const stations = [{ id: 7, name: '새 충전소', latitude: 37.6, longitude: 127.1 }]
  await wrapper.setProps({ stations, center: { latitude: 37.6, longitude: 127.1 }, selectedStationId: 7 })
  creation.resolve(map)
  await flushPromises()
  expect(map.updateStations).toHaveBeenLastCalledWith(stations)
  expect(map.moveTo).toHaveBeenLastCalledWith({ latitude: 37.6, longitude: 127.1 })
  expect(map.selectStation).toHaveBeenLastCalledWith(7)
})

it('서로 다른 SDK의 확대 숫자를 해석하지 않고 가능한 조작과 설명을 표시한다', async () => {
  const map = new ControlledMap()
  let events!: StationMapCallbacks
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(async (_container, _stations, _center, callbacks) => {
    events = callbacks
    return map
  })
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  events.zoomChanged?.({ canZoomIn: false, canZoomOut: true, description: '최대 확대' })
  await flushPromises()
  expect(wrapper.get<HTMLButtonElement>('[aria-label="지도 확대"]').element.disabled).toBe(true)
  expect(wrapper.get<HTMLButtonElement>('[aria-label="지도 축소"]').element.disabled).toBe(false)
  expect(wrapper.text()).toContain('최대 확대')
})

it('지도 생성 실패는 전체 화면 새로고침 없이 다시 시도할 수 있다', async () => {
  const map = new ControlledMap()
  const create = vi.fn().mockRejectedValueOnce(new Error('인증 실패')).mockResolvedValueOnce(map)
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(create)
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain('충전소 목록')
  await wrapper.get('#retry-map').trigger('click')
  await flushPromises()
  expect(create).toHaveBeenCalledTimes(2)
  expect(wrapper.find('[role="alert"]').exists()).toBe(false)
})

it('종료 전 지도 생성이 시간 초과되면 늦은 결과를 해제하고 재시도 상태를 유지한다', async () => {
  vi.useFakeTimers()
  const creation = new Deferred<StationMapSession>()
  const map = new ControlledMap()
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(async () => creation.promise)
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  await vi.advanceTimersByTimeAsync(10_000)
  expect(wrapper.find('#retry-map').exists()).toBe(true)
  creation.resolve(map)
  await flushPromises()
  expect(map.destroy).toHaveBeenCalledTimes(1)
  expect(wrapper.find('[role="alert"]').exists()).toBe(true)
  expect(wrapper.find('[aria-label="지도 확대"]').exists()).toBe(false)
})

it('마커 갱신이 끝난 뒤에 선택을 전달해 비동기 명령의 순서를 지킨다', async () => {
  const map = new ControlledMap()
  const update = new Deferred<void>()
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(async () => map)
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  map.updateStations.mockImplementationOnce(() => update.promise)
  map.selectStation.mockClear()
  await wrapper.setProps({ stations: [{ id: 7, name: '추가한 충전소', latitude: 37.5, longitude: 127 }] })
  await wrapper.setProps({ selectedStationId: 7 })
  await flushPromises()
  expect(map.selectStation).not.toHaveBeenCalled()
  update.resolve()
  await flushPromises()
  expect(map.selectStation).toHaveBeenLastCalledWith(7)
})

it('비동기 명령 실패를 재시도 안내로 바꾸고 해당 지도의 늦은 선택을 차단한다', async () => {
  const map = new ControlledMap()
  let events!: StationMapCallbacks
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(async (_container, _stations, _center, callbacks) => {
    events = callbacks
    return map
  })
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  map.selectStation.mockRejectedValueOnce(new Error('native 연결 종료'))
  await wrapper.setProps({ selectedStationId: 7 })
  await flushPromises()
  expect(wrapper.find('#retry-map').exists()).toBe(true)
  events.selectStation(8)
  expect(wrapper.emitted('select')).toBeUndefined()
  expect(map.destroy).toHaveBeenCalledTimes(1)
})

it('시간 초과한 이전 지도의 해제가 새로 만든 지도 화면을 지우지 않는다', async () => {
  vi.useFakeTimers()
  const creation = new Deferred<StationMapSession>()
  const previousMap = new ControlledMap()
  const nextMap = new ControlledMap()
  const create = vi.fn<Awaited<ReturnType<typeof loader.loadStationMap>>>()
    .mockImplementationOnce(async container => {
      previousMap.destroy.mockImplementation(async () => { container.replaceChildren() })
      return creation.promise
    }).mockImplementationOnce(async container => {
      const marker = document.createElement('button')
      marker.textContent = '새 지도 마커'
      container.append(marker)
      return nextMap
    })
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(create)
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  await vi.advanceTimersByTimeAsync(10_000)
  await wrapper.get('#retry-map').trigger('click')
  await flushPromises()
  creation.resolve(previousMap)
  await flushPromises()
  expect(wrapper.get('.station-map-canvas').text()).toBe('새 지도 마커')
  expect(previousMap.destroy).toHaveBeenCalledTimes(1)
  expect(nextMap.destroy).not.toHaveBeenCalled()
})

it('준비 중 SDK 실패 이벤트가 오면 늦은 준비 이벤트에도 오류와 재시도를 유지한다', async () => {
  const creation = new Deferred<StationMapSession>()
  const map = new ControlledMap()
  let events!: StationMapCallbacks
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(async (_container, _stations, _center, callbacks) => {
    events = callbacks
    return creation.promise
  })
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  events.failed()
  events.ready()
  creation.resolve(map)
  await flushPromises()
  expect(wrapper.get('section').attributes('aria-busy')).toBe('false')
  expect(wrapper.find('#retry-map').exists()).toBe(true)
  expect(wrapper.find('[aria-label="지도 확대"]').exists()).toBe(false)
  expect(map.destroy).toHaveBeenCalledTimes(1)
})

it('화면 이탈은 응답이 멈춘 지도 명령을 기다리지 않고 지도를 해제한다', async () => {
  const map = new ControlledMap()
  const update = new Deferred<void>()
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(async () => map)
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  map.updateStations.mockImplementationOnce(() => update.promise)
  await wrapper.setProps({ stations: [{ id: 7, name: '응답 대기 중', latitude: 37.5, longitude: 127 }] })
  await flushPromises()
  wrapper.unmount()
  await flushPromises()
  const destroyedBeforeCommandFinished = map.destroy.mock.calls.length
  update.resolve()
  await flushPromises()
  expect(destroyedBeforeCommandFinished).toBe(1)
})

it('실패한 지도의 진행 중 명령이 끝나지 않아도 새 지도는 준비할 수 있다', async () => {
  const previous = new ControlledMap()
  const next = new ControlledMap()
  const update = new Deferred<void>()
  let previousEvents!: StationMapCallbacks
  const create = vi.fn<Awaited<ReturnType<typeof loader.loadStationMap>>>()
    .mockImplementationOnce(async (_container, _stations, _center, events) => { previousEvents = events; return previous })
    .mockResolvedValueOnce(next)
  vi.spyOn(loader, 'loadStationMap').mockResolvedValue(create)
  const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
  await flushPromises()
  previous.updateStations.mockImplementationOnce(() => update.promise)
  await wrapper.setProps({ stations: [{ id: 7, name: '새 충전소', latitude: 37.5, longitude: 127 }] })
  await flushPromises()
  previousEvents.failed()
  await flushPromises()
  await wrapper.get('#retry-map').trigger('click')
  await flushPromises()
  const nextReadyBeforePreviousFinished = wrapper.find('[aria-label="지도 확대"]').exists()
  update.reject(new Error('이전 명령의 늦은 실패'))
  await flushPromises()
  expect(nextReadyBeforePreviousFinished).toBe(true)
  expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  expect(next.destroy).not.toHaveBeenCalled()
})
