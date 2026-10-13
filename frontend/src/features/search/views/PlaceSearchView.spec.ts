import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import { AxiosError, type AxiosAdapter } from 'axios'
import StationSearchView from './StationSearchView.vue'
import * as mapLoader from '../map/loadStationMap'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import searchResponse from '../../../../tests/fixtures/search.json'
const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios adapter가 필요합니다')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })

it('주소를 검색하고 후보를 고른 뒤에만 그 위치의 충전소를 조회한다', async () => {
  const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, config.url === '/places'
    ? { places: [{ name: '선택할 장소', address: '서울 주소', latitude: 37.5, longitude: 127 }] } : searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
  expect(wrapper.find('#place-query').exists()).toBe(true)
  await wrapper.get('#place-query').setValue(' 서울 시청 ')
  expect(adapter).not.toHaveBeenCalled()
  await wrapper.get('.place-search-form').trigger('submit')
  await flushPromises()
  expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ url: '/places', params: { query: '서울 시청' } }))
  await wrapper.get('.place-result').trigger('click')
  await flushPromises()
  expect(wrapper.text()).toContain('선택할 장소: 37.5 / 127')
  expect(adapter).toHaveBeenCalledTimes(2)
  expect(wrapper.find('#station-map-panel').exists()).toBe(true)
  expect(adapter).toHaveBeenLastCalledWith(expect.objectContaining({ url: '/stations', params: expect.objectContaining({ latitude: 37.5, longitude: 127, connector: 'DC_COMBO' }) }))
  expect(router.currentRoute.value.query.latitude).toBe('37.5')
})
it('주소 검색 키가 없어도 안내 뒤 현재 위치로 충전소 검색을 계속한다', async () => {
  const adapter = vi.fn<AxiosAdapter>()
  adapter.mockImplementationOnce(async config => { throw new AxiosError('failed', 'ERR_BAD_RESPONSE', config, undefined,
    jsonResponse(config, { code: 'PLACE_SEARCH_NOT_CONFIGURED', message: '주소 검색 준비 중입니다. 지도나 현재 위치로 검색해 주세요', fields: {} }, 503)) })
  adapter.mockImplementation(async config => jsonResponse(config, searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
  expect(wrapper.find('#place-query').exists()).toBe(true)
  await wrapper.get('#place-query').setValue('서울')
  await wrapper.get('.place-search-form').trigger('submit')
  await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain('주소 검색 준비 중')
  vi.stubGlobal('navigator', { userAgent: navigator.userAgent, platform: navigator.platform, geolocation: { getCurrentPosition: (success: PositionCallback) => success({ coords: { latitude: 37.5, longitude: 127, accuracy: 1, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) }) } })
    await wrapper.get('#current-location').trigger('click')
    await flushPromises()
  await wrapper.get('.search-form').trigger('submit')
  await flushPromises()
  expect(wrapper.findAll('article')).toHaveLength(2)
})

it('영역 검색 중 규격을 바꾸고 주소를 선택하면 이전 영역 없이 새 위치를 검색하고 결과에 포커스를 둔다', async () => {
  vi.spyOn(mapLoader, 'loadStationMap').mockRejectedValue(new Error('지도 불러오기 실패'))
  const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, config.url === '/places'
    ? { places: [{ name: '새 장소', address: '새 주소', latitude: 37.6, longitude: 127.1 }] } : searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20&south=37.49&north=37.51&west=126.99&east=127.01')
  const wrapper = mount(StationSearchView, { attachTo: document.body, global: { plugins: [router, createPinia()] } })
  await flushPromises()
  await wrapper.get('input[value="NACS"]').setValue()
  await wrapper.get('#place-query').setValue('새 주소')
  await wrapper.get('.place-search-form').trigger('submit')
  await flushPromises()
  await wrapper.get('.place-result').trigger('click')
  await flushPromises()
  expect(adapter).toHaveBeenLastCalledWith(expect.objectContaining({ url: '/stations', params: { latitude: 37.6, longitude: 127.1, radiusMeters: 1000, connector: 'NACS', limit: 20 } }))
  expect(router.currentRoute.value.query.south).toBeUndefined()
  expect(wrapper.findAll('article')).toHaveLength(2)
  expect(document.activeElement?.id).toBe('results-heading')
})
