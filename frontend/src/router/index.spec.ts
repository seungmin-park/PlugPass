import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import App from '../App.vue'
import { createPinia } from 'pinia'
import router from './index'

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
  it('없는 화면은 안내와 검색 복귀 링크를 제공한다', async () => {
    await router.push('/missing-screen')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('화면을 찾을 수 없습니다')
    expect(wrapper.findAll('a').map(link => link.text())).toContain('주변 검색으로 돌아가기')
    wrapper.unmount()
  })
})
