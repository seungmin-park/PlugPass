import { describe, expect, it } from 'vitest'
import { createNavigationLinks } from './navigationLinks'

describe('길찾기 목적지 경계', () => {
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
    expect(createNavigationLinks(destination, 'ios', 'https://plugpass.example/app/index.html')).toBeNull()
  })

  it.each([[31.43, 122.37], [44.35, 132]])('네이버 지원 좌표 경계 %s/%s를 전달한다', (latitude, longitude) => {
    const links = createNavigationLinks({ name: '경계', latitude, longitude }, 'ios', 'https://plugpass.example/app/index.html')
    expect(links?.apps.map(app => app.id)).toEqual(['kakao', 'naver'])
  })

  it.each([[31.429, 127], [44.351, 127], [37.5, 122.369], [37.5, 132.001], [-90, -180], [90, 180], [0, 0]])(
    '네이버 지원 범위 밖 %s/%s는 해당 앱 링크를 제공하지 않는다', (latitude, longitude) => {
      const links = createNavigationLinks({ name: '범위 밖', latitude, longitude }, 'ios', 'https://plugpass.example/app/index.html')
      expect(links?.apps.map(app => app.id)).toEqual(['kakao'])
      expect(links?.webHref).toContain(`,${latitude},${longitude}`)
    })

  it('네이버 호출자에는 페이지 경로만 전달하고 검색 조건·fragment는 보내지 않는다', () => {
    const links = createNavigationLinks({ name: '서울', latitude: 37.5, longitude: 127 }, 'ios',
      'https://plugpass.example/app/index.html?token=private#/stations/2?latitude=36')
    const naver = links?.apps.find(app => app.id === 'naver')
    expect(new URL(naver?.href ?? '').searchParams.get('appname')).toBe('https://plugpass.example/app/index.html')
  })
})
