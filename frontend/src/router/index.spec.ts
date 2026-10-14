import { httpClient } from '../shared/api/httpClient'
import { jsonResponse } from '../../tests/httpResponse'
import searchResponse from '../../tests/fixtures/search.json'
import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect, afterEach } from 'vitest'
import App from '../App.vue'
import { createPinia } from 'pinia'
import router from './index'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter가 필요합니다')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })

describe('웹앱 화면 이동', () => {
  it('첫 경로는 주변 검색으로 이동한다', async () => {
    await router.push('/')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/stations')
  })
  it('검색 본문으로 건너뛰는 링크와 포커스 대상을 제공한다', async () => {
    await router.push('/stations')
    const wrapper = mount(App, { attachTo: document.body, global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('a[href="#main-content"]').exists()).toBe(true)
    expect(wrapper.get('a[href="#main-content"]').text()).toBe('본문으로 건너뛰기')
    expect(wrapper.get('main#main-content').attributes('tabindex')).toBe('-1')
    await wrapper.get('a[href="#main-content"]').trigger('click')
    expect(document.activeElement).toBe(wrapper.get('main#main-content').element)
    expect(router.currentRoute.value.path).toBe('/stations')
    wrapper.unmount()
  })
  it('충전소 찾기는 다른 화면에서도 검색 조건을 보존한다', async () => {
    const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' }
    await router.push({ path: '/missing-screen', query })
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    const link = wrapper.get('nav[aria-label="주요 메뉴"] a')
    expect(link.text()).toBe('충전소 찾기')
    expect(link.attributes('href')).toContain('connector=DC_COMBO')
    wrapper.unmount()
  })
  it('검색 화면의 충전소 찾기는 조건을 펼치고 포커스를 이동한다', async () => {
    await router.push('/stations')
    const wrapper = mount(App, { attachTo: document.body, global: { plugins: [router, createPinia()] } })
    await flushPromises()
    const conditions = wrapper.get('details.search-panel')
    ;(conditions.element as HTMLDetailsElement).open = false
    await wrapper.get('nav[aria-label="주요 메뉴"] a').trigger('click')
    await flushPromises()
    expect((conditions.element as HTMLDetailsElement).open).toBe(true)
    expect(document.activeElement).toBe(conditions.get('summary').element)
    wrapper.unmount()
  })
  it('기본 limit을 생략한 검색 화면에서도 헤더는 URL을 바꾸거나 조건을 다시 닫지 않는다', async () => {
    httpClient.defaults.adapter = async config => jsonResponse(config, searchResponse)
    const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO' }
    await router.push({ path: '/stations', query })
    const wrapper = mount(App, { attachTo: document.body, global: { plugins: [router, createPinia()] } })
    await flushPromises()
    await wrapper.get('nav[aria-label="주요 메뉴"] a').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual(query)
    expect((wrapper.get('#search-conditions').element as HTMLDetailsElement).open).toBe(true)
    expect(document.activeElement).toBe(wrapper.get('#search-conditions summary').element)
    wrapper.unmount()
  })
  it('없는 화면은 안내와 검색 복귀 링크를 제공한다', async () => {
    await router.push('/missing-screen')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('화면을 찾을 수 없습니다')
    expect(wrapper.findAll('a').map(link => link.text())).toContain('주변 검색으로 돌아가기')
    wrapper.unmount()
  })
})
