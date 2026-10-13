import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import App from '../../App.vue'
import router from '../../router'
import { httpClient } from '../api/httpClient'
import { jsonResponse } from '../../../tests/httpResponse'
import search from '../../../tests/fixtures/search.json'
import detail from '../../../tests/fixtures/detail.json'
import alternatives from '../../../tests/fixtures/alternative.json'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter가 필요합니다')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', excludeStationId: '2' }

describe('실제 조회 상태에 연결한 진행 표시', () => {
  it.each([
    { path: '/stations', messages: ['주변 충전소를 조회하고 있습니다.'] },
    { path: '/stations/2', messages: ['충전소 상세를 조회하고 있습니다.'] },
    { path: '/alternatives', messages: ['수집 상태를 조회하고 있습니다.', '대체 후보를 조회하고 있습니다.'] },
  ])('$path의 응답 대기 중 원을 표시하고 성공하면 제거한다', async ({ path, messages }) => {
    let finish!: () => void
    const pending = new Promise<void>(resolve => { finish = resolve })
    const responses: Record<string, unknown> = { '/stations': search, '/stations/2': detail, '/recommendations': alternatives }
    httpClient.defaults.adapter = async config => { await pending; return jsonResponse(config, responses[config.url ?? '']) }
    await router.push({ path, query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.findAll('.loading-indicator').map(indicator => indicator.text())).toEqual(messages)
    expect(wrapper.findAll('.loading-spinner')).toHaveLength(messages.length)
    finish()
    await flushPromises()
    expect(wrapper.find('.loading-spinner').exists()).toBe(false)
  })

  it('위치 확인 중 원을 표시하고 실패하면 제거한다', async () => {
    const getCurrentPosition = vi.fn<Geolocation['getCurrentPosition']>()
    vi.stubGlobal('navigator', Object.create(navigator, { geolocation: { value: { getCurrentPosition } } }))
    await router.push('/stations')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await wrapper.get('#current-location').trigger('click')
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
    expect(wrapper.get('.loading-indicator').text()).toContain('현재 위치를 확인하고 있습니다')
    getCurrentPosition.mock.calls[0]?.[1]?.({ code: 1, message: 'denied', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3 })
    await flushPromises()
    expect(wrapper.find('.loading-spinner').exists()).toBe(false)
    expect(wrapper.get('[role="alert"]').text()).toContain('위치 권한이 거부되었습니다')
  })
})
