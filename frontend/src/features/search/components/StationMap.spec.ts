import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { installKakaoMapFake } from '../../../../tests/kakaoMapFake'
import StationMap from './StationMap.vue'
import * as mapLoader from '../map/loadStationMap'

const originalLoader = mapLoader.loadStationMap
beforeEach(installKakaoMapFake)
afterEach(() => { vi.unstubAllEnvs(); Reflect.deleteProperty(window, 'kakao') })

describe('지도 인스턴스의 수명과 표시 경계', () => {
  it('지도 인스턴스 생성이 늦어져도 준비 전에는 오류 대신 로딩을 유지한다', async () => {
    const createMap = await originalLoader()
    let finishCreation!: () => void
    const creation = new Promise<void>(resolve => { finishCreation = resolve })
    vi.spyOn(mapLoader, 'loadStationMap').mockResolvedValue(async (...args) => {
      await creation
      return createMap(...args)
    })
    const wrapper = mount(StationMap, { props: {
      stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await flushPromises()
    const loadingBeforeReady = wrapper.find('.loading-indicator').exists()
    const errorBeforeReady = wrapper.find('[role="alert"]').exists()
    finishCreation()
    await flushPromises()
    expect(loadingBeforeReady).toBe(true)
    expect(errorBeforeReady).toBe(false)
    expect(wrapper.find('button[aria-label="지도 확대"]').exists()).toBe(true)
  })

  it('지도 확대·축소 조작을 보여주고 버튼으로 지도의 확대 수준을 바꾼다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await vi.waitFor(() => expect(wrapper.find('.kakao-test-map').exists()).toBe(true))
    expect(wrapper.get('.station-map-canvas').attributes('tabindex')).toBe('0')
    await wrapper.get('button[aria-label="지도 확대"]').trigger('click')
    expect(wrapper.get('[role="status"]').text()).toContain('확대')
    await wrapper.get('button[aria-label="지도 축소"]').trigger('click')
    expect(wrapper.get('[role="status"]').text()).toContain('확대 수준')
    wrapper.get('.station-map-canvas').element.dispatchEvent(new KeyboardEvent('keydown', { key: '+' }))
    await vi.waitFor(() => expect(wrapper.get('[role="status"]').text()).toContain('4단계'))
  })
  it('단일 충전소를 표시하고 해제한 컨테이너에 다시 지도를 만들 수 있다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '한 곳', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(1))
    const canvas = wrapper.get<HTMLDivElement>('.station-map-canvas').element
    wrapper.unmount()
    await flushPromises()
    expect(canvas.querySelectorAll('.kakao-station-marker')).toHaveLength(0)
    const createMap = await originalLoader()
    const session = await createMap(canvas, [{ id: 2, name: '새 충전소', latitude: 37.5, longitude: 127 }],
      { latitude: 37.5, longitude: 127 }, { selectStation: () => {}, ready: () => {}, failed: () => {} })
    expect(canvas.querySelector('[aria-label="새 충전소 지도 마커"]')).not.toBeNull()
    await session.destroy()
  })

  it('빈 결과는 지도와 출처를 유지하며 존재하지 않는 선택을 만들지 않는다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await vi.waitFor(() => expect(wrapper.find('.kakao-test-map').exists()).toBe(true))
    expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(0)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })

  it('같은 좌표의 두 충전소도 각각의 이름과 선택 ID를 유지한다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '동일 위치 첫 충전소', latitude: 37.5, longitude: 127 },
        { id: 2, name: '동일 위치 다른 충전소', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: 1,
    } })
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    expect(wrapper.get('[aria-label="동일 위치 첫 충전소 지도 마커"]').attributes('aria-pressed')).toBe('true')
    await wrapper.get('[aria-label="동일 위치 다른 충전소 지도 마커"]').trigger('click')
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
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
    expect(wrapper.get('.loading-indicator').text()).toBe('지도를 불러오고 있습니다.')
    await flushPromises()
    wrapper.unmount()
    finishLoading(createMap)
    await flushPromises()
    expect(canvas.querySelectorAll('.kakao-test-map')).toHaveLength(0)
  })

  it('SDK 대기 중 데이터가 바뀌어도 한 번 로딩한 지도에 최신 결과를 표시한다', async () => {
    const createMap = await originalLoader()
    let finishPrevious!: (factory: typeof createMap) => void
    vi.spyOn(mapLoader, 'loadStationMap').mockImplementationOnce(() => new Promise(resolve => { finishPrevious = resolve }))
      .mockImplementation(originalLoader)
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '이전 충전소', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await wrapper.setProps({ stations: [{ id: 2, name: '새 충전소', latitude: 38, longitude: 128 }], center: { latitude: 38, longitude: 128 } })
    expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(0)
    finishPrevious(createMap)
    await flushPromises()
    expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(1)
    expect(wrapper.find('[aria-label="새 충전소 지도 마커"]').exists()).toBe(true)
    expect(wrapper.find('[aria-label="이전 충전소 지도 마커"]').exists()).toBe(false)
  })

  it('충전소 이름을 HTML로 삽입하지 않고 마커 라벨에 텍스트로 넣는다', async () => {
    const wrapper = mount(StationMap, { props: {
      stations: [{ id: 1, name: '<img src=x onerror=alert(1)>', latitude: 37.5, longitude: 127 }],
      center: { latitude: 37.5, longitude: 127 }, selectedStationId: null,
    } })
    await vi.waitFor(() => expect(wrapper.find('.kakao-station-marker').exists()).toBe(true))
    expect(wrapper.get('.kakao-station-marker').attributes('aria-label')).toBe('<img src=x onerror=alert(1)> 지도 마커')
    expect(wrapper.findAll('.kakao-station-marker img')).toHaveLength(0)
  })
})
