import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { AxiosError, type AxiosAdapter } from 'axios'
import App from '../../../App.vue'
import router from '../../../router'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import detail from '../../../../tests/fixtures/detail.json'
import search from '../../../../tests/fixtures/search.json'
import { formatTimestamp } from '../../charging-info/presentation'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter가 필요합니다')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' }

describe('상세의 정보 근거와 이동', () => {
  it('검색 카드에서 ID와 확정 조건을 상세로 전달한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, config.url === '/stations' ? search : detail)
    await router.push({ path: '/stations', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    const detailLink = wrapper.findAll('a').find(link => link.text().includes('가까운 충전소 상세'))
    expect(detailLink?.exists()).toBe(true)
    await detailLink?.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/stations/2')
    expect(router.currentRoute.value.query).toEqual(query)
    expect(wrapper.text()).toContain('충전소 상세')
  })
  it('충전기별 보고·최신성·관측과 수집 시각을 구분한다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, detail))
    httpClient.defaults.adapter = adapter
    await router.push({ path: '/stations/2', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('가까운 충전소')
    expect(wrapper.text()).toContain('이용 가능 보고')
    expect(wrapper.text()).toContain('최신성 확인 불가')
    expect(wrapper.text()).toContain('관측 시각: 확인 불가')
    expect(wrapper.text()).toContain(`수집 시각: ${formatTimestamp(detail.chargers[0]?.collectedAt ?? null)}`)
    expect(wrapper.text()).toContain('관측 시각이 제공되지 않았습니다')
    expect(wrapper.text()).toContain('원본 상태 코드: 2')
    expect(wrapper.text()).toContain('운영 시간: 24시간')
    expect(wrapper.text()).toContain('조회 시각:')
    expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ url: '/stations/2' }))
    const alternatives = wrapper.findAll('a').find(link => link.text() === '다른 충전소 찾기')
    expect(alternatives?.attributes('href')).toContain('excludeStationId=2')
    const list = wrapper.findAll('a').find(link => link.text() === '검색 목록으로 돌아가기')
    await list?.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual(query)
    expect(router.currentRoute.value.path).toBe('/stations')
  })
  it('조건 없는 직접 링크는 상세만 조회하고 조건 선택을 안내한다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, detail))
    httpClient.defaults.adapter = adapter
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('대체 후보를 찾으려면 검색 조건을 먼저 선택해 주세요')
    expect(wrapper.findAll('a').find(link => link.text() === '검색 조건 선택')?.attributes('href')).toBe('#/stations')
    expect(adapter).toHaveBeenCalledTimes(1)
  })
  it('충전기가 없는 상세는 빈 정보 안내를 제공한다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, { ...detail, chargers: [] })
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('충전기 정보가 없습니다')
  })
  it('알 수 없는 코드와 누락 조건은 보수적으로 표시하고 원본 비고는 텍스트다', async () => {
    const charger = detail.chargers[0]
    if (!charger) throw new Error('충전기 응답 필요')
    httpClient.defaults.adapter = async config => jsonResponse(config, { ...detail, chargers: [{ ...charger,
      status: 'NEW_STATUS', freshness: 'NEW_FRESHNESS', reasonCode: 'constructor', useTime: null, limitYn: null,
      note: '<img src=x onerror=alert(1)>', sourceObservedAt: '2026-10-05T00:00:00Z' }] })
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('상태 확인 불가')
    expect(wrapper.text()).toContain('최신성 확인 불가')
    expect(wrapper.text()).toContain('상세 확인이 필요한 사유')
    expect(wrapper.text()).toContain('이용 조건 확인 필요')
    expect(wrapper.text()).toContain(`<img src=x onerror=alert(1)>`)
    expect(wrapper.find('.charger-detail img').exists()).toBe(false)
    expect(wrapper.text()).toContain(`관측 시각: ${formatTimestamp('2026-10-05T00:00:00Z')}`)
  })
  it('없는 ID의 404는 안내하고 재시도 후 상세를 표시한다', async () => {
    let attempts = 0
    httpClient.defaults.adapter = async config => {
      attempts += 1
      if (attempts === 1) throw new AxiosError('404', 'ERR_BAD_REQUEST', config, undefined,
        { ...jsonResponse(config, { code: 'STATION_NOT_FOUND', message: '충전소를 찾을 수 없습니다', fields: {} }), status: 404 })
      return jsonResponse(config, detail)
    }
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('충전소를 찾을 수 없습니다')
    await wrapper.get('#retry').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('가까운 충전소')
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(attempts).toBe(2)
  })
  it.each(['0', '9007199254740992', 'abc'])('잘못된 ID %s는 HTTP 호출 전에 거부한다', async stationId => {
    const adapter = vi.fn<AxiosAdapter>()
    httpClient.defaults.adapter = adapter
    await router.push(`/stations/${stationId}`)
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('충전소 ID')
    expect(adapter).not.toHaveBeenCalled()
    expect(wrapper.find('#retry').exists()).toBe(false)
  })
  it('느린 이전 ID의 성공이 새 ID 결과를 덮지 않는다', async () => {
    let finishFirst: () => void = () => {}
    httpClient.defaults.adapter = config => config.url === '/stations/2'
      ? new Promise(resolve => { finishFirst = () => resolve(jsonResponse(config, detail)) })
      : Promise.resolve(jsonResponse(config, { ...detail, id: 3, name: '새 충전소' }))
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.findAll('[role="status"]').map(message => message.text())).toContain('충전소 상세를 조회하고 있습니다.')
    await router.push('/stations/3')
    await flushPromises()
    finishFirst()
    await flushPromises()
    expect(wrapper.text()).toContain('새 충전소')
    expect(wrapper.text()).not.toContain('가까운 충전소')
  })
})
