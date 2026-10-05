import { mount, flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import StationMap from './StationMap.vue'
import * as mapLoader from '../map/loadStationMap'

const originalLoader = mapLoader.loadStationMap

describe('지도 인스턴스의 수명과 표시 경계', () => {
  it('단일 충전소를 표시하고 해제한 컨테이너에 다시 지도를 만들 수 있다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '한 곳', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await vi.waitFor(() => expect(wrapper.findAll('.leaflet-marker-icon')).toHaveLength(1))
    const canvas = wrapper.get<HTMLDivElement>('.station-map-canvas').element
    wrapper.unmount()
    expect(canvas.querySelectorAll('.leaflet-marker-icon')).toHaveLength(0)
    const createMap = await originalLoader()
    const session = createMap(canvas, [{ id: 2, name: '새 충전소', latitude: 37.5, longitude: 127 }],
      { latitude: 37.5, longitude: 127 }, { selectStation: () => {}, tilesLoaded: () => {}, tilesFailed: () => {} })
    expect(canvas.querySelector('[aria-label="새 충전소 지도 마커"]')).not.toBeNull()
    session.destroy()
  })

  it('빈 결과는 지도와 출처를 유지하며 존재하지 않는 선택을 만들지 않는다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await vi.waitFor(() => expect(wrapper.find('.leaflet-control-attribution').exists()).toBe(true))
    expect(wrapper.findAll('.leaflet-marker-icon')).toHaveLength(0)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })

  it('같은 좌표의 두 충전소도 각각의 이름과 선택 ID를 유지한다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '동일 위치 첫 충전소', latitude: 37.5, longitude: 127 },
        { id: 2, name: '동일 위치 다른 충전소', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: 1,
    } })
    await vi.waitFor(() => expect(wrapper.findAll('.leaflet-marker-icon')).toHaveLength(2))
    expect(wrapper.get('[aria-label="동일 위치 첫 충전소 지도 마커"]').attributes('aria-pressed')).toBe('true')
    await wrapper.get('[aria-label="동일 위치 다른 충전소 지도 마커"]').trigger('keydown', { key: 'Enter' })
    expect(wrapper.emitted('select')).toEqual([[2]])
    await wrapper.setProps({ selectedStationId: 2 })
    expect(wrapper.get('[aria-label="동일 위치 첫 충전소 지도 마커"]').attributes('aria-pressed')).toBe('false')
    expect(wrapper.get('[aria-label="동일 위치 다른 충전소 지도 마커"]').attributes('aria-pressed')).toBe('true')
  })

  it('SDK 대기 중 화면을 떠나면 늦게 완료해도 지도 DOM을 만들지 않는다', async () => {
    const createMap = await originalLoader()
    let finishLoading!: (factory: typeof createMap) => void
    vi.spyOn(mapLoader, 'loadStationMap').mockImplementation(() => new Promise(resolve => { finishLoading = resolve }))
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '이전 화면', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    const canvas = wrapper.get('.station-map-canvas').element
    wrapper.unmount()
    finishLoading(createMap)
    await flushPromises()
    expect(canvas.querySelectorAll('.leaflet-pane')).toHaveLength(0)
  })

  it('SDK 대기 중 표시 데이터가 바뀌면 늦은 이전 로딩이 새 지도를 덮지 않는다', async () => {
    const createMap = await originalLoader()
    let finishPrevious!: (factory: typeof createMap) => void
    vi.spyOn(mapLoader, 'loadStationMap').mockImplementationOnce(() => new Promise(resolve => { finishPrevious = resolve }))
      .mockImplementation(originalLoader)
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '이전 충전소', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await wrapper.setProps({ stations: [{ id: 2, name: '새 충전소', latitude: 38, longitude: 128 }], center: { latitude: 38, longitude: 128 } })
    await vi.waitFor(() => expect(wrapper.find('[aria-label="새 충전소 지도 마커"]').exists()).toBe(true))
    finishPrevious(createMap)
    await flushPromises()
    expect(wrapper.findAll('.leaflet-marker-icon')).toHaveLength(1)
    expect(wrapper.find('[aria-label="이전 충전소 지도 마커"]').exists()).toBe(false)
  })

  it('충전소 이름을 HTML로 삽입하지 않고 마커 라벨에 텍스트로 넣는다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '<img src=x onerror=alert(1)>', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await vi.waitFor(() => expect(wrapper.find('.leaflet-marker-icon').exists()).toBe(true))
    expect(wrapper.get('.leaflet-marker-icon').attributes('aria-label')).toBe('<img src=x onerror=alert(1)> 지도 마커')
    expect(wrapper.findAll('.leaflet-marker-icon img')).toHaveLength(0)
  })
})
