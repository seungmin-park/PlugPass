import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { AxiosError, type AxiosAdapter } from 'axios'
import App from '../../../App.vue'
import router from '../../../router'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import alternative from '../../../../tests/fixtures/alternative.json'
import search from '../../../../tests/fixtures/search.json'
import detail from '../../../../tests/fixtures/detail.json'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter 필요')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20', excludeStationId: '2' }

describe('같은 조건의 대체 후보 화면', () => {
  it('같은 조건과 제외 ID로 두 API를 조회하고 후보 상세는 실제로 새로 조회한다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config,
      config.url === '/recommendations' ? alternative : config.url === '/stations' ? search : { ...detail, id: 3, name: '대체 충전소' }))
    httpClient.defaults.adapter = adapter
    await router.push({ path: '/alternatives', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('대체 충전소 후보')
    expect(wrapper.text()).toContain('이용 전 확인 필요')
    expect(wrapper.text()).toContain('제외 충전소 ID: 2')
    expect(wrapper.text()).toContain('직선거리 88m')
    expect(wrapper.text()).toContain('관측 시각이 제공되지 않았습니다')
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url: '/recommendations', params: {
      latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO', limit: 20, excludeStationId: 2 } }))
    await wrapper.get('a#candidate-3').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/stations/3')
    expect(router.currentRoute.value.query).toEqual({ latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' })
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url: '/stations/3' }))
    expect(wrapper.text()).toContain('충전소 ID: 3')
  })
  it('세 그룹과 서버 순서를 유지하고 알 수 없는 사유를 추측하지 않는다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, config.url === '/stations' ? search : {
      preferred: [{ id: 7, name: '서버 첫 후보', distanceMeters: 200, reasonCodes: ['RECENT_AVAILABLE'] },
        { id: 5, name: '서버 둘째 후보', distanceMeters: 100, reasonCodes: ['constructor'] }],
      requiresConfirmation: alternative.requiresConfirmation,
      excluded: [{ id: 9, name: '제한된 충전소', distanceMeters: 300, reasonCodes: ['ACCESS_RESTRICTED'] }],
    })
    await router.push({ path: '/alternatives', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.findAll('.candidate-group h2').map(heading => heading.text())).toEqual(['우선 후보', '이용 전 확인 필요', '추천에서 제외된 이유'])
    expect(wrapper.findAll('.candidate-group h3').map(heading => heading.text())).toEqual(['서버 첫 후보', '서버 둘째 후보', '대체 충전소', '제한된 충전소'])
    expect(wrapper.text()).toContain('상세 확인이 필요한 사유')
    expect(wrapper.text()).toContain('이용 제한이 보고되었습니다')
    expect(wrapper.text()).not.toContain('호환 충전기')
    expect(wrapper.text()).not.toContain('전체 건수')
  })
  it('빈 세 그룹은 각각 후보 없음으로 표시한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, config.url === '/stations' ? search : {
      preferred: [], requiresConfirmation: [], excluded: [] })
    await router.push({ path: '/alternatives', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.findAll('.candidate-group [role="status"]').map(message => message.text())).toEqual(['해당 그룹의 후보가 없습니다.', '해당 그룹의 후보가 없습니다.', '해당 그룹의 후보가 없습니다.'])
  })
  it('수집 준비 전이어도 성공한 후보는 함께 표시한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, config.url === '/stations' ? { ...search, dataReady: false } : alternative)
    await router.push({ path: '/alternatives', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('전체 수집 완료 전 정보')
    expect(wrapper.text()).toContain('대체 충전소')
  })
  it('수집 상태만 실패하면 후보를 유지하고 수집 상태만 다시 조회한다', async () => {
    let metadataRequests = 0
    let candidateRequests = 0
    httpClient.defaults.adapter = async config => {
      if (config.url === '/recommendations') { candidateRequests += 1; return jsonResponse(config, alternative) }
      metadataRequests += 1
      if (metadataRequests === 1) throw new AxiosError('503', 'ERR_BAD_RESPONSE', config, undefined,
        { ...jsonResponse(config, { code: 'SERVER_ERROR', message: '수집 상태 오류', fields: {} }), status: 503 })
      return jsonResponse(config, search)
    }
    await router.push({ path: '/alternatives', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('수집 상태 확인 실패')
    expect(wrapper.text()).toContain('대체 충전소')
    await wrapper.get('#retry-metadata').trigger('click')
    await flushPromises()
    expect(metadataRequests).toBe(2)
    expect(candidateRequests).toBe(1)
    expect(wrapper.text()).not.toContain('수집 상태 확인 실패')
  })
  it('후보만 실패하면 성공한 수집 상태를 유지하고 후보만 재시도한다', async () => {
    let candidateRequests = 0
    const adapter = vi.fn<AxiosAdapter>(async config => {
      if (config.url === '/stations') return jsonResponse(config, search)
      candidateRequests += 1
      if (candidateRequests === 1) throw new AxiosError('503', 'ERR_BAD_RESPONSE', config, undefined,
        { ...jsonResponse(config, { code: 'SERVER_ERROR', message: '후보 조회 오류', fields: {} }), status: 503 })
      return jsonResponse(config, alternative)
    })
    httpClient.defaults.adapter = adapter
    await router.push({ path: '/alternatives', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('수집 상태 조회 시각')
    expect(wrapper.text()).toContain('후보 조회 오류')
    await wrapper.get('#retry-candidates').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('대체 충전소')
    expect(adapter.mock.calls.filter(([config]) => config.url === '/stations')).toHaveLength(1)
  })
  it.each([{}, { ...query, latitude: '' }, { ...query, excludeStationId: '0' }, { ...query, excludeStationId: ['2', '3'] }])('불완전하거나 중복된 직접 링크 %s는 HTTP 전에 안내한다', async input => {
    const adapter = vi.fn<AxiosAdapter>()
    httpClient.defaults.adapter = adapter
    await router.push({ path: '/alternatives', query: input })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('검색 조건과 제외 충전소 ID를 확인해 주세요')
    expect(adapter).not.toHaveBeenCalled()
    expect(wrapper.find('#retry-candidates').exists()).toBe(false)
  })
})
