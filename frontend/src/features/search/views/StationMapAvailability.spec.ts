import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import StationSearchView from './StationSearchView.vue'
import * as mapLoader from '../map/loadStationMap'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import searchResponse from '../../../../tests/fixtures/search.json'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter가 필요합니다')
const originalLoader = mapLoader.loadStationMap
const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' }
afterEach(() => { httpClient.defaults.adapter = originalAdapter; vi.useRealTimers() })
function searchRouter() {
  return createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
}

describe('지도 실패와 목록의 독립성', () => {
  it('SDK 로딩이 실패해도 목록·상세 링크를 유지하고 화면 새로고침을 제공한다', async () => {
    vi.spyOn(mapLoader, 'loadStationMap').mockRejectedValue(new Error('SDK 다운로드 실패'))
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.findAll('article a').map(link => link.text())).toEqual(['가까운 충전소 상세 보기', '대체 충전소 상세 보기'])
    expect(wrapper.find('#reload-map').exists()).toBe(true)
    expect(router.currentRoute.value.query).toEqual(query)
  })

  it('SDK가 10초 동안 응답하지 않으면 목록을 유지하고 늦게 완료돼도 실패 화면을 덮지 않는다', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    const createMap = await originalLoader()
    let finishLoading!: (factory: typeof createMap) => void
    vi.spyOn(mapLoader, 'loadStationMap').mockImplementation(() => new Promise(resolve => { finishLoading = resolve }))
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.advanceTimersByTimeAsync(10_000)
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.findAll('article')).toHaveLength(2)
    finishLoading(createMap)
    await flushPromises()
    expect(wrapper.findAll('.leaflet-marker-icon')).toHaveLength(0)
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
  })

  it('타일 요청 실패는 안내하고 목록 선택·상세 이동을 유지한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('#show-map').trigger('click')
    await vi.waitFor(() => expect(wrapper.findAll('.leaflet-tile')).not.toHaveLength(0))
    await wrapper.get('.leaflet-tile').trigger('error')
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('배경 지도를 불러오지 못했습니다')
    await wrapper.get('button[aria-label="가까운 충전소 지도에서 선택"]').trigger('click')
    expect(wrapper.get('button[aria-label="가까운 충전소 지도에서 선택"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.findAll('article a')).toHaveLength(2)
  })
})
