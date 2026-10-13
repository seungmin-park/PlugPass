import { afterEach, expect, it, vi } from 'vitest'
import type { AxiosAdapter } from 'axios'
import { getPlaces } from './placeApi'
import { httpClient } from '../../../shared/api/httpClient'
import { jsonResponse } from '../../../../tests/httpResponse'
const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios adapter가 필요합니다')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
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
