import { describe, expect, it } from 'vitest'
import { createNavigationLinks } from './navigationLinks'

describe('길찾기 목적지 경계', () => {
  it('Android는 특정 지도 앱을 고정하지 않는 공통 위치 요청과 카카오맵 설치 대안을 만든다', () => {
    const links = createNavigationLinks({ name: '시흥 충전소', latitude: 37.5, longitude: 127 }, 'android')
    expect(links?.directHref).toContain('intent://0,0?q=37.5%2C127')
    expect(links?.directHref).toContain('scheme=geo;')
    expect(links?.directHref).not.toContain('package=')
    expect(links?.directHref).toContain(encodeURIComponent('https://play.google.com/store/apps/details?id=net.daum.android.map'))
  })

  it('iPhone은 카카오맵 앱 주소로 목적지를 한 번에 전달한다', () => {
    const links = createNavigationLinks({ name: '시흥 충전소', latitude: 37.5, longitude: 127 }, 'ios')
    expect(links?.directHref).toBe('kakaomap://route?ep=37.5,127&by=car')
  })

  it.each([
    { name: '', latitude: 37.5, longitude: 127 },
    { name: '  ', latitude: 37.5, longitude: 127 },
    { name: '충전소', latitude: NaN, longitude: 127 },
    { name: '충전소', latitude: Infinity, longitude: 127 },
    { name: '충전소', latitude: 90.01, longitude: 127 },
    { name: '충전소', latitude: -90.01, longitude: 127 },
    { name: '충전소', latitude: 37.5, longitude: Infinity },
    { name: '충전소', latitude: 37.5, longitude: -180.01 },
    { name: '충전소', latitude: 37.5, longitude: 180.01 },
  ])('유효하지 않은 목적지 %j로 외부 링크를 만들지 않는다', destination => {
    expect(createNavigationLinks(destination, 'ios')).toBeNull()
  })

  it('Android 목적지 이름을 인텐트 쿼리 데이터로 인코딩한다', () => {
    const links = createNavigationLinks({ name: '충전소, A/B &?#=끝', latitude: 37.5, longitude: 127 }, 'android')
    expect(links?.directHref).toContain(encodeURIComponent('37.5,127(충전소, A/B &?#=끝)'))
  })
})
