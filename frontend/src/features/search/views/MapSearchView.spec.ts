import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { installKakaoMapFake } from '../../../../tests/kakaoMapFake'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import type { AxiosAdapter } from 'axios'
import StationSearchView from './StationSearchView.vue'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import searchResponse from '../../../../tests/fixtures/search.json'
import { readVehicleProfile } from '../../vehicle/vehicleProfile'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios adapter가 필요합니다')
beforeEach(installKakaoMapFake)
afterEach(() => { httpClient.defaults.adapter = originalAdapter; window.localStorage.clear(); vi.unstubAllEnvs(); Reflect.deleteProperty(window, 'kakao') })

it('검색 전 지도를 열고 이동한 영역으로 검색하며 응답 뒤에도 지도를 유지한다', async () => {
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
  const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
  expect(wrapper.find('#show-map').exists()).toBe(true)
  await wrapper.get('#vehicle-choice').setValue('kona-electric-2020-kr')
  vi.stubGlobal('navigator', { userAgent: navigator.userAgent, platform: navigator.platform, geolocation: { getCurrentPosition: (success: PositionCallback) => success({ coords: { latitude: 37.5, longitude: 127, accuracy: 1, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) }) } })
    await wrapper.get('#current-location').trigger('click')
    await flushPromises()
  await wrapper.get('#show-map').trigger('click')
  await vi.waitFor(() => expect(wrapper.find('.kakao-test-zoom-in').exists()).toBe(true))
  await wrapper.get('.kakao-test-zoom-in').trigger('click')
  await vi.waitFor(() => expect(wrapper.get<HTMLButtonElement>('#search-map-area').element.disabled).toBe(false))
  expect(adapter).not.toHaveBeenCalled()
  const control = wrapper.get('.kakao-test-zoom-in').element
  await wrapper.get('#search-map-area').trigger('click')
  await flushPromises()
  expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ url: '/stations/area', params: expect.objectContaining({ connectors: 'DC_COMBO', south: expect.any(Number), north: expect.any(Number) }) }))
  expect(router.currentRoute.value.query.south).toBeDefined()
  expect(wrapper.get('.kakao-test-zoom-in').element).toBe(control)
  expect(wrapper.findAll('.kakao-station-marker')).toHaveLength(2)
  expect(readVehicleProfile(window.localStorage)).toEqual({ vehicleId: 'kona-electric-2020-kr', adapterIds: [], vehicleSupportConfirmed: false })
})

it('지나치게 축소한 지도는 확대 안내를 보여 주고 검색하지 않는다', async () => {
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
  const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
  expect(wrapper.find('#show-map').exists()).toBe(true)
  await wrapper.get('#show-map').trigger('click')
  await vi.waitFor(() => expect(wrapper.find('.kakao-test-zoom-out').exists()).toBe(true))
  for (let step = 0; step < 5; step += 1) await wrapper.get('.kakao-test-zoom-out').trigger('click')
  await vi.waitFor(() => expect(wrapper.text()).toContain('지도를 확대해 주세요'))
  expect(wrapper.text()).toContain('지도를 확대해 주세요')
  expect(wrapper.get<HTMLButtonElement>('#search-map-area').element.disabled).toBe(true)
  expect(adapter).not.toHaveBeenCalled()
})

it('지도가 움직이는 동안은 이전 영역으로 검색하지 않고 이동 완료 후에만 확정한다', async () => {
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
  const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { attachTo: document.body, global: { plugins: [router, createPinia()] } })
  await wrapper.get('#show-map').trigger('click')
  await vi.waitFor(() => expect(wrapper.find('.kakao-test-zoom-in').exists()).toBe(true))
  await wrapper.get('.station-map-canvas').trigger('keydown', { key: 'ArrowRight' })
  expect(wrapper.get<HTMLButtonElement>('#search-map-area').element.disabled).toBe(true)
  await wrapper.get('.search-form').trigger('submit')
  expect(adapter).not.toHaveBeenCalled()
  await vi.waitFor(() => expect(wrapper.get<HTMLButtonElement>('#search-map-area').element.disabled).toBe(false))
})
