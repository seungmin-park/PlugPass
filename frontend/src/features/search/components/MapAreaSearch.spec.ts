import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { installKakaoMapFake } from '../../../../tests/kakaoMapFake'
import StationMap from './StationMap.vue'
import SearchForm from './SearchForm.vue'

describe('지도 영역으로 검색 범위를 선택한다', () => {
  beforeEach(installKakaoMapFake)
  afterEach(() => { vi.unstubAllEnvs(); Reflect.deleteProperty(window, 'kakao') })
  it('반경을 묻지 않고 목록에 없는 차량의 확인한 충전구를 제출한다', async () => {
    const wrapper = mount(SearchForm)
    await wrapper.get('#vehicle-choice').setValue('manual')
    expect(wrapper.findAll('legend').map(legend => legend.text())).toEqual(['내 차량 충전구 직접 확인'])
    await wrapper.get('input[value="NACS"]').setValue()
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('submit')).toEqual([[{ connector: 'NACS' }]])
  })
  it('실제 지도 확대가 끝나면 새 화면의 경계를 전달한다', async () => {
    vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
    const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
    await vi.waitFor(() => expect(wrapper.emitted('area-change')).toHaveLength(1))
    const initialArea = wrapper.emitted('area-change')?.[0]?.[0]
    await wrapper.get('.kakao-test-zoom-in').trigger('click')
    await vi.waitFor(() => expect(wrapper.emitted('area-change')?.length).toBeGreaterThan(1))
    expect(wrapper.emitted('area-change')?.slice(-1)[0]?.[0]).not.toEqual(initialArea)
  })
  it('결과 교체는 지도 DOM과 사용자가 선택한 영역을 유지한다', async () => {
    vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
    const wrapper = mount(StationMap, { props: { stations: [], center: { latitude: 37.5, longitude: 127 }, selectedStationId: null } })
    await vi.waitFor(() => expect(wrapper.find('.kakao-test-zoom-in').exists()).toBe(true))
    const zoomControl = wrapper.get('.kakao-test-zoom-in').element
    await wrapper.get('.kakao-test-zoom-in').trigger('click')
    const areas = wrapper.emitted('area-change')?.length
    await wrapper.setProps({ stations: [{ id: 1, name: '새 충전소', latitude: 37.51, longitude: 127.01 }] })
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(1))
    expect(wrapper.get('.kakao-test-zoom-in').element).toBe(zoomControl)
    expect(wrapper.emitted('area-change')?.length).toBe(areas)
  })
})
