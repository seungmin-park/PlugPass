import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { installKakaoMapFake } from '../../../../tests/kakaoMapFake'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import StationSearchView from './StationSearchView.vue'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import searchResponse from '../../../../tests/fixtures/search.json'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter가 필요합니다')
const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' }
beforeEach(installKakaoMapFake)
afterEach(() => { httpClient.defaults.adapter = originalAdapter; vi.unstubAllEnvs(); Reflect.deleteProperty(window, 'kakao') })
function searchRouter() {
  return createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
}

describe('검색 지도의 목록 선택', () => {
  it('Android 카드 길찾기는 웹 패널 없이 운영체제 지도 요청을 바로 전달한다', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Android')
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    const directions = wrapper.get('a[aria-label="가까운 충전소 길찾기"]')
    expect(directions.attributes('href')).toContain('scheme=geo;')
    expect(wrapper.find('.station-action-sheet').exists()).toBe(false)
  })

  it('실제 SDK 마커를 클릭하면 같은 ID의 카드만 선택한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    await wrapper.get('[aria-label="대체 충전소 지도 마커"]').trigger('click')
    expect(wrapper.findAll('.station-card-selected')).toHaveLength(1)
    expect(wrapper.get('.station-card-selected h3').text()).toBe('대체 충전소')
    expect(wrapper.get('[aria-label="대체 충전소 지도 마커"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('[aria-label="가까운 충전소 지도 마커"]').attributes('aria-pressed')).toBe('false')
    expect(wrapper.find('[role="dialog"]').exists()).toBe(true)
    expect(wrapper.get('[role="dialog"]').text()).toContain('대체 충전소')
    expect(wrapper.get('[role="dialog"]').text()).toContain('길찾기')
    expect(wrapper.get('[role="dialog"]').text()).toContain('별도 이용 제한이 보고되지 않았습니다. 현장 안내를 확인해 주세요')
  })

  it('마커 패널에서도 제한 사유가 비어 있으면 방문 전 확인을 안내한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, { ...searchResponse, stations: searchResponse.stations.map(station => ({
      ...station, chargers: station.chargers.map(charger => ({ ...charger, limitYn: 'Y', limitDetail: null })),
    })) })
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    await wrapper.get('[aria-label="대체 충전소 지도 마커"]').trigger('click')
    expect(wrapper.get('.station-action-sheet .access-summary').text()).toContain('이용 제한이 보고됐습니다. 방문 전 운영 주체에 확인해 주세요')
  })

  it('iPhone 카드 길찾기는 바로 연결하고 마커를 누르면 해당 충전소 패널을 연다', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('iPhone')
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    expect(wrapper.get('a[aria-label="가까운 충전소 길찾기"]').attributes('href')).toBe('kakaomap://route?ep=37.5,127&by=car')
    expect(wrapper.find('.station-action-sheet').exists()).toBe(false)
    await wrapper.get('[aria-label="대체 충전소 지도 마커"]').trigger('click')
    expect(wrapper.get('.station-action-sheet h2').text()).toBe('대체 충전소')
    expect(wrapper.get('#navigation-primary').attributes('href')).toBe('kakaomap://route?ep=37.5,127.001&by=car')
  })

  it('하단 패널의 길찾기 링크에서도 Escape로 닫을 수 있다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    await wrapper.get('[aria-label="대체 충전소 지도 마커"]').trigger('click')
    await wrapper.get('#navigation-web').trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('.station-action-sheet').exists()).toBe(false)
  })

  it('iPhone 카드 길찾기 링크는 추가 선택 패널 없이 키보드로도 누를 수 있다', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('iPhone')
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const host = document.createElement('div')
    document.body.append(host)
    const wrapper = mount(StationSearchView, { attachTo: host, global: { plugins: [router, createPinia()] } })
    try {
      await flushPromises()
      const directions = wrapper.get('a[aria-label="가까운 충전소 길찾기"]')
      ;(directions.element as HTMLElement).focus()
      expect(document.activeElement).toBe(directions.element)
      expect(directions.attributes('href')).toBe('kakaomap://route?ep=37.5,127&by=car')
      expect(wrapper.find('.station-action-sheet').exists()).toBe(false)
    } finally {
      wrapper.unmount()
      host.remove()
    }
  })

  it('마커 선택을 카드 강조에 반영하고 다른 마커로 전환한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    await wrapper.get('[aria-label="가까운 충전소 지도 마커"]').trigger('click')
    expect(wrapper.get('[aria-label="가까운 충전소 지도 마커"]').attributes('aria-pressed')).toBe('true')
    await wrapper.get('[aria-label="대체 충전소 지도 마커"]').trigger('click')
    expect(wrapper.get('.station-card-selected h3').text()).toBe('대체 충전소')
    expect(wrapper.get('[aria-label="대체 충전소 지도 마커"]').element.tagName).toBe('BUTTON')
    expect(wrapper.find('.kakao-test-map').exists()).toBe(true)
  })
  it('지도를 요청하기 전에는 목록을 유지하고 지도 보기 버튼을 제공한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.findAll('article')).toHaveLength(2)
    expect(wrapper.find('#show-map').exists()).toBe(true)
    expect(wrapper.find('[aria-label="검색 결과 지도"]').exists()).toBe(false)
  })

  it('마커에서 선택한 충전소 하나만 카드에 강조하고 상세 링크를 유지한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    await wrapper.get('[aria-label="가까운 충전소 지도 마커"]').trigger('click')
    expect(wrapper.get('.station-card-selected h3').text()).toBe('가까운 충전소')
    await wrapper.get('[aria-label="대체 충전소 지도 마커"]').trigger('click')
    expect(wrapper.findAll('.station-card-selected')).toHaveLength(1)
    expect(wrapper.get('.station-card-selected h3').text()).toBe('대체 충전소')
    expect(wrapper.findAll('article a').filter(link => link.text().includes('상세 보기')).map(link => link.text())).toEqual(['가까운 충전소 상세 보기', '대체 충전소 상세 보기'])
  })

  it('새 조건으로 검색하면 같은 ID가 다시 반환돼도 이전 선택을 지운다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    await wrapper.get('[aria-label="가까운 충전소 지도 마커"]').trigger('click')
    await router.push({ path: '/stations', query: { ...query, radiusMeters: '3000' } })
    await flushPromises()
    expect(wrapper.findAll('button[aria-pressed="true"]')).toHaveLength(0)
    expect(wrapper.findAll('article')).toHaveLength(2)
  })

  it('같은 조건의 새로고침도 이전 선택을 지운다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2))
    await wrapper.get('[aria-label="가까운 충전소 지도 마커"]').trigger('click')
    await wrapper.get('#refresh').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('button[aria-pressed="true"]')).toHaveLength(0)
  })

  it('빈 결과에서도 지도 탐색을 요청할 수 있지만 없는 카드 선택은 표시하지 않는다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, { ...searchResponse, stations: [] })
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('#show-map').exists()).toBe(true)
    expect(wrapper.findAll('article')).toHaveLength(0)
    expect(wrapper.text()).toContain('조건에 맞는 충전소가 없습니다')
  })
})
