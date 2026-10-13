import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { AxiosError } from 'axios'
import App from '../../../App.vue'
import router from '../../../router'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
import detail from '../../../../tests/fixtures/detail.json'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios 기본 adapter가 필요합니다')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })

describe('상세에서 외부 지도 길찾기', () => {
  it.each(['iPhone', 'iPad', 'Macintosh'])('%s의 iOS 앱 링크는 목적지만 전달하고 설치 대안을 제공한다', async device => {
    vi.stubGlobal('navigator', Object.create(navigator, { userAgent: { value: `Mozilla/5.0 (${device})` }, maxTouchPoints: { value: 5 } }))
    httpClient.defaults.adapter = async config => jsonResponse(config, detail)
    await router.push('/stations/2?latitude=36&longitude=128&radiusMeters=1000&connector=DC_COMBO')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('summary').exists()).toBe(true)
    expect(wrapper.find('summary').text()).toBe('길찾기')
    expect(wrapper.find('a#navigation-kakao').attributes('href')).toBe('kakaomap://route?ep=37.5,127&by=car')
    expect(wrapper.find('a#navigation-naver').exists()).toBe(true)
    const naver = new URL(wrapper.find('a#navigation-naver').attributes('href') ?? '')
    expect(naver.protocol).toBe('nmap:')
    expect(naver.hostname + naver.pathname).toBe('route/car')
    expect(Object.fromEntries(naver.searchParams)).toEqual({ dlat: '37.5', dlng: '127', dname: '가까운 충전소', appname: window.location.origin + window.location.pathname })
    expect(wrapper.find('a#install-kakao').attributes('href')).toBe('https://apps.apple.com/app/id304608425')
    expect(wrapper.find('a#install-naver').attributes('href')).toBe('https://apps.apple.com/app/id311867728')
    expect(wrapper.text()).toContain('앱이 열리지 않으면')
    expect(wrapper.find('a#navigation-web').attributes('href')).toContain('https://map.kakao.com/link/to/')
  })

  it('Android는 정확한 패키지와 미설치 fallback을 포함한 intent를 만든다', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Mozilla/5.0 (Linux; Android 15)')
    httpClient.defaults.adapter = async config => jsonResponse(config, detail)
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('a#navigation-kakao').exists()).toBe(true)
    const kakao = wrapper.find('a#navigation-kakao').attributes('href')
    expect(kakao).toBe('intent://route?ep=37.5,127&by=car#Intent;scheme=kakaomap;package=net.daum.android.map;S.browser_fallback_url=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dnet.daum.android.map;end')
    const naver = wrapper.find('a#navigation-naver').attributes('href') ?? ''
    expect(naver).toContain('intent://route/car?dlat=37.5&dlng=127&dname=')
    expect(naver).toContain(';scheme=nmap;package=com.nhn.android.nmap;')
    expect(naver).toContain('S.browser_fallback_url=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dcom.nhn.android.nmap;end')
    expect(wrapper.find('a#install-naver').attributes('href')).toBe('https://play.google.com/store/apps/details?id=com.nhn.android.nmap')
  })

  it('데스크톱에서는 앱 스킴 대신 목적지 웹 길찾기를 제공한다', async () => {
    vi.stubGlobal('navigator', Object.create(navigator, { userAgent: { value: 'Mozilla/5.0 (Macintosh)' }, maxTouchPoints: { value: 0 } }))
    httpClient.defaults.adapter = async config => jsonResponse(config, detail)
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('a#navigation-web').exists()).toBe(true)
    expect(wrapper.find('a#navigation-web').attributes('href')).toBe('https://map.kakao.com/link/to/%EA%B0%80%EA%B9%8C%EC%9A%B4%20%EC%B6%A9%EC%A0%84%EC%86%8C,37.5,127')
    expect(wrapper.findAll('a[href^="intent:"], a[href^="nmap:"], a[href^="kakaomap:"]')).toHaveLength(0)
  })

  it('충전소 이름의 한글·쉼표·URL 구분자를 데이터로 인코딩한다', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('iPhone')
    httpClient.defaults.adapter = async config => jsonResponse(config, { ...detail, name: '충전소, A/B &?#=끝' })
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('a#navigation-naver').exists()).toBe(true)
    const naver = new URL(wrapper.find('a#navigation-naver').attributes('href') ?? '')
    expect(naver.searchParams.get('dname')).toBe('충전소, A/B &?#=끝')
    expect([...naver.searchParams.keys()]).toEqual(['dlat', 'dlng', 'dname', 'appname'])
    const web = wrapper.find('a#navigation-web').attributes('href')
    expect(web).toBe('https://map.kakao.com/link/to/%EC%B6%A9%EC%A0%84%EC%86%8C%2C%20A%2FB%20%26%3F%23%3D%EB%81%9D,37.5,127')
  })

  it.each([{ latitude: 91 }, { longitude: -181 }, { latitude: null }, { name: '' }])('잘못된 상세 %j는 길찾기를 제공하지 않는다', async invalid => {
    httpClient.defaults.adapter = async config => jsonResponse(config, { ...detail, ...invalid })
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.find('summary').exists()).toBe(false)
  })

  it('새 상세를 기다리는 동안 이전 링크를 제거하고 새 목적지로 교체한다', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('iPhone')
    let finishNext: () => void = () => {}
    httpClient.defaults.adapter = config => config.url === '/stations/2'
      ? Promise.resolve(jsonResponse(config, detail))
      : new Promise(resolve => { finishNext = () => resolve(jsonResponse(config, { ...detail, id: 3, name: '다음 충전소', latitude: 38, longitude: 128 })) })
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('a#navigation-kakao').exists()).toBe(true)
    await router.push('/stations/3')
    await flushPromises()
    expect(wrapper.find('a#navigation-kakao').exists()).toBe(false)
    finishNext()
    await flushPromises()
    expect(wrapper.find('a#navigation-kakao').attributes('href')).toBe('kakaomap://route?ep=38,128&by=car')
    expect(wrapper.find('details').attributes('open')).toBeUndefined()
  })

  it('조회 실패에는 링크를 만들지 않고 재시도 성공 후 제공한다', async () => {
    let attempts = 0
    httpClient.defaults.adapter = async config => {
      attempts += 1
      if (attempts === 1) throw new AxiosError('unavailable', 'ERR_NETWORK', config)
      return jsonResponse(config, detail)
    }
    await router.push('/stations/2')
    const wrapper = mount(App, { global: { plugins: [router, createPinia()] } })
    await flushPromises()
    expect(wrapper.find('summary').exists()).toBe(false)
    await wrapper.get('#retry').trigger('click')
    await flushPromises()
    expect(wrapper.find('summary').exists()).toBe(true)
  })
})
