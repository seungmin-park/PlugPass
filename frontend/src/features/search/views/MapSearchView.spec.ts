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
  const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
  vi.stubGlobal('navigator', { userAgent: navigator.userAgent, geolocation: { getCurrentPosition } })
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
  const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
  expect(wrapper.find('#show-map').exists()).toBe(false)
  await wrapper.get('#vehicle-choice').setValue('kona-electric-2020-kr')
  expect(getCurrentPosition).toHaveBeenCalledTimes(1)
  await vi.waitFor(() => expect(wrapper.find('.kakao-test-zoom-in').exists()).toBe(true))
  await wrapper.get('.kakao-test-zoom-in').trigger('click')
  await vi.waitFor(() => expect(wrapper.get<HTMLButtonElement>('#search-map-area').element.disabled).toBe(false))
  expect(adapter).not.toHaveBeenCalled()
  const control = wrapper.get('.kakao-test-zoom-in').element
  await wrapper.get('#search-map-area').trigger('click')
  await flushPromises()
  expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ url: '/stations/area', params: expect.objectContaining({ connectors: 'DC_COMBO', south: expect.any(Number), north: expect.any(Number) }) }))
  expect(router.currentRoute.value.query.south).toBeDefined()
  const success = getCurrentPosition.mock.calls[0]?.[0]
  if (!success) throw new Error('GPS 요청 callback이 필요합니다')
  success({ coords: { latitude: 35.1, longitude: 129.1, accuracy: 10, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) })
  await flushPromises()
  expect(adapter).toHaveBeenCalledTimes(1)
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
  expect(wrapper.find('#show-map').exists()).toBe(false)
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
  await vi.waitFor(() => expect(wrapper.find('.kakao-test-zoom-in').exists()).toBe(true))
  await wrapper.get('.station-map-canvas').trigger('keydown', { key: 'ArrowRight' })
  expect(wrapper.get<HTMLButtonElement>('#search-map-area').element.disabled).toBe(true)
  await wrapper.get('.search-form').trigger('submit')
  expect(adapter).not.toHaveBeenCalled()
  await vi.waitFor(() => expect(wrapper.get<HTMLButtonElement>('#search-map-area').element.disabled).toBe(false))
})

it('지도 지역을 먼저 고른 뒤 차량을 선택해도 GPS로 지역을 덮어쓰지 않는다', async () => {
  const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
  vi.stubGlobal('navigator', { userAgent: navigator.userAgent, geolocation: { getCurrentPosition } })
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
  const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
  await vi.waitFor(() => expect(wrapper.get<HTMLButtonElement>('#search-map-area').element.disabled).toBe(false))
  await wrapper.get('#search-map-area').trigger('click')
  await wrapper.get('#vehicle-choice').setValue('kona-electric-2020-kr')
  expect(getCurrentPosition).not.toHaveBeenCalled()
  await wrapper.get('.search-form').trigger('submit')
  await flushPromises()
  expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ url: '/stations/area' }))
})

it('유효한 수동 조건으로 지도 이동 중 제출하면 GPS 대신 이동 완료를 기다린다', async () => {
  const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
  vi.stubGlobal('navigator', { userAgent: navigator.userAgent, geolocation: { getCurrentPosition } })
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
  const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
  httpClient.defaults.adapter = adapter
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
  await wrapper.get('#vehicle-choice').setValue('manual')
  await vi.waitFor(() => expect(wrapper.find('.kakao-test-zoom-in').exists()).toBe(true))
  await wrapper.get('.station-map-canvas').trigger('keydown', { key: 'ArrowRight' })
  await wrapper.get('.search-form').trigger('submit')
  expect(getCurrentPosition).not.toHaveBeenCalled()
  expect(adapter).not.toHaveBeenCalled()
  expect(wrapper.text()).toContain('지도가 준비되고 이동이 끝난 후 검색해 주세요')
})

it.each(['wheel', 'click'])('지도 %s 조작 후 차량을 선택하면 지도 지역을 유지한다', async interaction => {
  const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
  vi.stubGlobal('navigator', { userAgent: navigator.userAgent, geolocation: { getCurrentPosition } })
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(640)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(400)
  httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }] })
  await router.push('/stations')
  const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
  await vi.waitFor(() => expect(wrapper.find('.kakao-test-zoom-in').exists()).toBe(true))
  await wrapper.get('.kakao-test-zoom-in').trigger(interaction)
  await wrapper.get('#vehicle-choice').setValue('kona-electric-2020-kr')
  expect(getCurrentPosition).not.toHaveBeenCalled()
})
