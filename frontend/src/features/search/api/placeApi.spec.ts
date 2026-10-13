import { afterEach, expect, it, vi } from 'vitest'
import type { AxiosAdapter } from 'axios'
import { getPlaces } from './placeApi'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios adapter가 필요합니다')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
afterEach(() => { vi.unstubAllEnvs(); vi.restoreAllMocks() })
it.each(['', '   ', 'a'.repeat(101)])('잘못된 검색어는 HTTP 전에 거부한다: %s', async query => {
  const adapter = vi.fn<AxiosAdapter>()
  httpClient.defaults.adapter = adapter
  await expect(getPlaces(query)).rejects.toMatchObject({ kind: 'validation' })
  expect(adapter).not.toHaveBeenCalled()
})
it.each([{ places: null }, { places: [{ name: '서울', address: '', latitude: 91, longitude: 127 }] }, { places: [{ name: '', address: '', latitude: 37, longitude: 127 }] }])('잘못된 응답을 빈 결과로 숨기지 않는다: %s', async body => {
  httpClient.defaults.adapter = async config => jsonResponse(config, body)
  await expect(getPlaces('서울')).rejects.toMatchObject({ kind: 'invalid_response' })
})
it('공백을 정리하고 특수문자를 한 번 인코딩한다', async () => {
  httpClient.defaults.adapter = async config => {
    expect(httpClient.getUri(config)).toBe('/api/v1/places?query=%EC%84%9C%EC%9A%B8+%26+%EC%8B%9C%EC%B2%AD')
    return jsonResponse(config, { places: [] })
  }
  await expect(getPlaces(' 서울 & 시청 ')).resolves.toEqual([])
})

it('카카오 지도 키가 있으면 서버 REST 설정에 의존하지 않고 SDK 주소 검색을 사용한다', async () => {
  vi.stubEnv('VITE_KAKAO_JS_KEY', 'test-js-key')
  const adapter = vi.fn<AxiosAdapter>()
  httpClient.defaults.adapter = adapter
  const pending = getPlaces(' 서울시청 ')
  await vi.waitFor(() => expect(document.head.querySelector('script[data-plugpass-kakao-map]')).not.toBeNull())
  const result = { address_name: '서울특별시 중구 세종대로 110', x: '126.9784', y: '37.5663' }
  Object.defineProperty(window, 'kakao', { configurable: true, value: { maps: {
    load: (ready: () => void) => ready(),
    services: {
      Status: { OK: 'OK', ZERO_RESULT: 'ZERO_RESULT', ERROR: 'ERROR' },
      Geocoder: class { addressSearch(_query: string, callback: (results: unknown[], status: string) => void) { callback([result], 'OK') } },
      Places: class { keywordSearch(_query: string, callback: (results: unknown[], status: string) => void) { callback([], 'ZERO_RESULT') } },
    },
  } } })
  document.head.querySelector('script[data-plugpass-kakao-map]')?.dispatchEvent(new Event('load'))
  await expect(pending).resolves.toEqual([{ name: '서울특별시 중구 세종대로 110', address: '서울특별시 중구 세종대로 110', latitude: 37.5663, longitude: 126.9784 }])
  expect(adapter).not.toHaveBeenCalled()
})
