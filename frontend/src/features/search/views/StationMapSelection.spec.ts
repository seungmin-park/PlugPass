import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import StationSearchView from './StationSearchView.vue'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import searchResponse from '../../../../tests/fixtures/search.json'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter가 필요합니다')
const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' }
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
function searchRouter() {
  return createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
}

describe('검색 지도의 목록 선택', () => {
  it('실제 SDK 마커를 클릭하면 같은 ID의 카드만 선택한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.leaflet-marker-icon')).toHaveLength(2))
    await wrapper.get('[aria-label="대체 충전소 지도 마커"]').trigger('click')
    expect(wrapper.get('button[aria-label="대체 충전소 지도에서 선택"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('[aria-label="대체 충전소 지도 마커"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('[aria-label="가까운 충전소 지도 마커"]').attributes('aria-pressed')).toBe('false')
    expect(wrapper.text()).toContain('선택한 충전소: 대체 충전소')
  })

  it.each([{ label: 'Enter', key: 'Enter' }, { label: 'Space', key: ' ' }])('카드 선택을 실제 SDK 마커에 반영하고 $label 키로 다른 카드를 선택한다', async ({ key }) => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.leaflet-marker-icon')).toHaveLength(2))
    await wrapper.get('button[aria-label="가까운 충전소 지도에서 선택"]').trigger('click')
    expect(wrapper.get('[aria-label="가까운 충전소 지도 마커"]').attributes('aria-pressed')).toBe('true')
    await wrapper.get('[aria-label="대체 충전소 지도 마커"]').trigger('keydown', { key })
    expect(wrapper.get('button[aria-label="대체 충전소 지도에서 선택"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('[aria-label="대체 충전소 지도 마커"]').attributes('tabindex')).toBe('0')
    expect(wrapper.find('.leaflet-control-attribution a[href="https://www.openstreetmap.org/copyright"]').exists()).toBe(true)
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

  it('카드에서 선택한 충전소 하나만 선택 상태로 표시하고 상세 링크를 유지한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await wrapper.get('button[aria-label="가까운 충전소 지도에서 선택"]').trigger('click')
    expect(wrapper.get('button[aria-label="가까운 충전소 지도에서 선택"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('button[aria-label="대체 충전소 지도에서 선택"]').attributes('aria-pressed')).toBe('false')
    await wrapper.get('button[aria-label="대체 충전소 지도에서 선택"]').trigger('click')
    expect(wrapper.get('button[aria-label="가까운 충전소 지도에서 선택"]').attributes('aria-pressed')).toBe('false')
    expect(wrapper.get('button[aria-label="대체 충전소 지도에서 선택"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.findAll('article a').map(link => link.text())).toEqual(['가까운 충전소 상세 보기', '대체 충전소 상세 보기'])
  })

  it('새 조건으로 검색하면 같은 ID가 다시 반환돼도 이전 선택을 지운다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await wrapper.get('button[aria-label="가까운 충전소 지도에서 선택"]').trigger('click')
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
    await wrapper.get('button[aria-label="가까운 충전소 지도에서 선택"]').trigger('click')
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
