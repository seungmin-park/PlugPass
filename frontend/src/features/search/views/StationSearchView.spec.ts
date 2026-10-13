import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import { AxiosError, type AxiosAdapter } from 'axios'
import StationSearchView from './StationSearchView.vue'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import searchResponse from '../../../../tests/fixtures/search.json'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter가 필요합니다')
const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' }
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
function searchRouter() {
  return createRouter({ history: createMemoryHistory(), routes: [{ path: '/stations', component: StationSearchView }, { path: '/other', component: { template: '<p>다른 화면</p>' } }] })
}

describe('위치 선택부터 실제 API 계약까지 검색 화면', () => {
  it('초기 화면에서 위치·API를 자동 요청하지 않고 현재 위치와 주소 검색을 제공한다', async () => {
    const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } })
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push('/stations')
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.findAll('button').map(button => button.text())).toContain('현재 위치 사용')
    expect(wrapper.find('#example-location').exists()).toBe(false)
    expect(getCurrentPosition).not.toHaveBeenCalled()
    expect(adapter).not.toHaveBeenCalled()
    await wrapper.get('.search-form').trigger('submit')
    expect(wrapper.text()).toContain('검색할 위치를 먼저 선택해 주세요')
    expect(adapter).not.toHaveBeenCalled()
  })
  it('현재 위치와 미제출 조건은 검색 버튼을 누를 때 URL·API로 확정한다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push('/stations')
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    vi.stubGlobal('navigator', { userAgent: navigator.userAgent, platform: navigator.platform, geolocation: { getCurrentPosition: (success: PositionCallback) => success({ coords: { latitude: 37.5, longitude: 127, accuracy: 1, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) }) } })
    await wrapper.get('#current-location').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('현재 위치: 37.5 / 127')
    await wrapper.get('input[value="NACS"]').setValue()
    expect(router.currentRoute.value.query).toEqual({})
    expect(adapter).not.toHaveBeenCalled()
    await wrapper.get('.search-form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ ...query, radiusMeters: '1000', connector: 'NACS' })
    expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ params: { latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'NACS', limit: 20 } }))
    expect(wrapper.text()).toContain('가까운 충전소')
  })
  it('직접 URL의 임의 반경과 커넥터·limit을 복원하고 결과·조회 시각을 표시한다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push({ path: '/stations', query: { ...query, radiusMeters: '1500', connector: 'NACS', limit: '2' } })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.findAll<HTMLInputElement>('input:checked').map(input => input.element.value)).toEqual(['NACS'])
    expect(wrapper.findAll('article')).toHaveLength(2)
    expect(wrapper.text()).toContain('최대 2개 표시')
    expect(wrapper.text()).toContain('직선거리')
    expect(wrapper.text()).toContain('이용 가능 보고 1대')
    expect(wrapper.text()).toContain('최신성 확인 불가')
    expect(wrapper.text()).toContain('조회 시각')
    expect(wrapper.text()).toContain('전체 수집 성공 시각')
    await wrapper.get('.search-form').trigger('submit')
    await flushPromises()
    expect(adapter).toHaveBeenCalledTimes(2)
    expect(router.currentRoute.value.query.limit).toBe('2')
  })
  it('검색 후 조건과 수집 세부 정보는 접고 결과를 먼저 보여 준다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('details.search-panel').exists()).toBe(true)
    expect(wrapper.get<HTMLDetailsElement>('details.search-panel').element.open).toBe(false)
    expect(wrapper.get('details.search-panel > summary').text()).toContain('검색 조건 변경')
    expect(wrapper.get<HTMLDetailsElement>('details.collection-details').element.open).toBe(false)
    expect(wrapper.get('.station-list').element.compareDocumentPosition(wrapper.get('.collection-details').element) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })
  it('뒤로 가면 URL 조건을 복원하며 미제출 입력과 이전 결과를 남기지 않는다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('input[value="NACS"]').setValue()
    expect(adapter).toHaveBeenCalledTimes(1)
    await wrapper.get('.search-form').trigger('submit')
    await flushPromises()
    await wrapper.get('input[value="AC_SLOW"]').setValue()
    router.back()
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual(query)
    expect(wrapper.findAll<HTMLInputElement>('input:checked').map(input => input.element.value)).toEqual(['DC_COMBO'])
    expect(adapter).toHaveBeenCalledTimes(3)
  })
  it('부분 누락 URL은 필드 오류를 안내하고 API를 호출하지 않는다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push({ path: '/stations', query: { latitude: '37.5' } })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('검색 조건을 확인해 주세요')
    expect(wrapper.text()).toContain('경도 조건')
    expect(adapter).not.toHaveBeenCalled()
  })
  it.each([
    ['CURRENT', '수집 정상'],
    ['DELAYED', '갱신이 지연되고 있습니다. 마지막으로 저장된 정보를 표시합니다'],
    ['NOT_READY', '전체 데이터 준비 전'],
  ])('수집 상태 %s를 충전기 관측 시각과 구분한다', async (state, message) => {
    const collection = { state, lastCatalogSuccessAt: '2026-10-13T00:00:00Z', lastStatusSuccessAt: null,
      lastAttemptAt: '2026-10-13T00:00:00Z', lastAttemptStatus: 'SUCCESS', statusIntervalSeconds: 300 }
    httpClient.defaults.adapter = async config => jsonResponse(config, { ...searchResponse, collection })
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain(message)
    expect(wrapper.text()).toContain('상태 변경분 수집 성공 시각')
    expect(wrapper.findAll('article')).toHaveLength(searchResponse.stations.length)
  })
  it.each([
    [{ dataReady: false, lastSuccessfulRunAt: null, stations: [] }, '아직 전체 수집이 완료되지 않았습니다'],
    [{ ...searchResponse, dataReady: false, lastSuccessfulRunAt: null }, '전체 수집 완료 전 정보'],
    [{ dataReady: true, lastSuccessfulRunAt: '2026-10-05T00:00:00Z', stations: [] }, '조건에 맞는 충전소가 없습니다'],
  ])('준비 전·부분 결과·정상 빈 결과를 구분한다: %s', async (response, message) => {
    httpClient.defaults.adapter = async config => jsonResponse(config, response)
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain(message)
    expect(wrapper.findAll('article')).toHaveLength(response.stations.length)
    expect(wrapper.text()).not.toContain('전체 20개')
  })
  it.each([[400, '반경 오류'], [503, '검색 서버 오류']])('HTTP %s를 화면 오류로 보여 주고 명시적인 재시도로 회복한다', async (status, message) => {
    const adapter = vi.fn<AxiosAdapter>()
    adapter.mockImplementationOnce(async config => { throw new AxiosError('failed', 'ERR_BAD_RESPONSE', config, undefined,
      jsonResponse(config, { code: 'INVALID_REQUEST', message: '검색 서버 오류', fields: status === 400 ? { radiusMeters: '반경 오류' } : {} }, status)) })
    adapter.mockImplementationOnce(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain(message)
    expect(wrapper.findAll('article')).toHaveLength(0)
    expect(adapter).toHaveBeenCalledTimes(1)
    await wrapper.get('#retry').trigger('click')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.findAll('article')).toHaveLength(2)
    expect(adapter).toHaveBeenCalledTimes(2)
  })
  it('요청 중에는 로딩을 표시하고 새로고침으로 다시 조회한다', async () => {
    let finish: () => void = () => {}
    const adapter = vi.fn<AxiosAdapter>(config => new Promise(resolve => { finish = () => resolve(jsonResponse(config, searchResponse)) }))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push({ path: '/stations', query })
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('주변 충전소를 조회하고 있습니다')
    expect(wrapper.findAll('article')).toHaveLength(0)
    finish()
    await flushPromises()
    await wrapper.get('#refresh').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('article')).toHaveLength(0)
    finish()
    await flushPromises()
    expect(wrapper.findAll('article')).toHaveLength(2)
    expect(adapter).toHaveBeenCalledTimes(2)
  })
  it('위치 권한 거부와 늦은 위치 응답 후에도 현재 위치 재시도로 검색할 수 있다', async () => {
    const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } })
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push('/stations')
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await wrapper.get('#current-location').trigger('click')
    getCurrentPosition.mock.calls[0]?.[1]?.({ code: 1, message: 'denied', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3 })
    await flushPromises()
    expect(wrapper.text()).toContain('위치 권한이 거부되었습니다')
    await wrapper.get('#current-location').trigger('click')
    vi.stubGlobal('navigator', { userAgent: navigator.userAgent, platform: navigator.platform, geolocation: { getCurrentPosition: (success: PositionCallback) => success({ coords: { latitude: 37.5, longitude: 127, accuracy: 1, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) }) } })
    await wrapper.get('#current-location').trigger('click')
    await flushPromises()
    getCurrentPosition.mock.calls[1]?.[0]({ coords: { latitude: 38, longitude: 128, accuracy: 1, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) })
    await wrapper.get('.search-form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual(query)
    expect(adapter).toHaveBeenCalledTimes(1)
  })
  it('현재 위치 성공 좌표를 제출하고 화면 이탈 뒤 callback은 조회를 시작하지 않는다', async () => {
    const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } })
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    const router = searchRouter()
    await router.push('/stations')
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await wrapper.get('#current-location').trigger('click')
    const position: GeolocationPosition = { coords: { latitude: 38, longitude: 128, accuracy: 1, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) }
    getCurrentPosition.mock.calls[0]?.[0](position)
    await flushPromises()
    await wrapper.get('.search-form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ ...query, latitude: '38', longitude: '128' })
    await wrapper.get('#current-location').trigger('click')
    wrapper.unmount()
    getCurrentPosition.mock.calls[1]?.[0](position)
    await flushPromises()
    expect(adapter).toHaveBeenCalledTimes(1)
  })
  it('새 위치 요청이 완료되면 이전 성공 좌표를 덮어쓴다', async () => {
    const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } })
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const router = searchRouter()
    await router.push('/stations')
    const wrapper = mount(StationSearchView, { global: { plugins: [router, createPinia()] } })
    await wrapper.get('#current-location').trigger('click')
    getCurrentPosition.mock.calls[0]?.[0]({ coords: { latitude: 38, longitude: 128, accuracy: 1, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) })
    vi.stubGlobal('navigator', { userAgent: navigator.userAgent, platform: navigator.platform, geolocation: { getCurrentPosition: (success: PositionCallback) => success({ coords: { latitude: 37.5, longitude: 127, accuracy: 1, altitude: null, altitudeAccuracy: null, heading: null, speed: null, toJSON: () => ({}) }, timestamp: 0, toJSON: () => ({}) }) } })
    await wrapper.get('#current-location').trigger('click')
    await flushPromises()
    await wrapper.get('.search-form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual(query)
  })

})
