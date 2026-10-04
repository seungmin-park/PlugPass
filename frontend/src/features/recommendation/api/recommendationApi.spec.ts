import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter } from 'axios'
import recommendation from '../../../../tests/fixtures/alternative.json'
import { jsonResponse } from '../../../../tests/httpResponse'
import { httpClient } from '../../../shared/api/httpClient'
import { getRecommendations } from './recommendationApi'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error("Axios 기본 adapter가 필요합니다")
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
const criteria = { latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO' as const, limit: 20 }

describe('대체 후보 API', () => {
  it('제외 ID·조건·signal을 보내고 확인 필요 그룹을 보존한다', async () => {
    const signal = new AbortController().signal
    const adapter = vi.fn<AxiosAdapter>(async config => {
      expect(httpClient.getUri(config)).toBe('/api/v1/recommendations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20&excludeStationId=2')
      return jsonResponse(config, recommendation)
    })
    httpClient.defaults.adapter = adapter
    await expect(getRecommendations(criteria, 2, signal)).resolves.toEqual(recommendation)
    expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({
      method: 'get', baseURL: '/api/v1', url: '/recommendations', params: { ...criteria, excludeStationId: 2 }, signal, timeout: 10000,
    }))
  })
  it('제외 ID가 없으면 query에 넣지 않으며 세 빈 그룹을 허용한다', async () => {
    const empty = { preferred: [], requiresConfirmation: [], excluded: [] }
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, empty))
    httpClient.defaults.adapter = adapter
    await expect(getRecommendations(criteria, null)).resolves.toEqual(empty)
    expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ params: criteria }))
  })
  it('서버 그룹·이유 코드를 보존하고 좌표·준비 상태를 만들지 않는다', async () => {
    const response = { preferred: [{ id: 1, name: '후보', distanceMeters: 0, reasonCodes: ['FUTURE_REASON'] }], requiresConfirmation: [], excluded: [] }
    httpClient.defaults.adapter = async config => jsonResponse(config, response)
    await expect(getRecommendations(criteria, null)).resolves.toEqual(response)
  })
  it.each([0, -1, 1.5, NaN, Infinity, 9007199254740992])('제외 ID %j는 전송 전에 거부한다', async excludedId => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, recommendation))
    httpClient.defaults.adapter = adapter
    await expect(getRecommendations(criteria, excludedId)).rejects.toMatchObject({ kind: 'validation', fields: { excludeStationId: expect.any(String) } })
    expect(adapter).not.toHaveBeenCalled()
  })
  it.each([null, {}, { ...recommendation, preferred: null }, { ...recommendation, excluded: undefined },
    { ...recommendation, preferred: [{ id: 1, name: '', distanceMeters: 0, reasonCodes: [] }] },
    { ...recommendation, preferred: [{ id: 1, name: '후보', distanceMeters: 0, reasonCodes: [' '] }] },
    { ...recommendation, requiresConfirmation: [{ id: 9007199254740992, name: '후보', distanceMeters: 0, reasonCodes: [] }] },
    { ...recommendation, preferred: [{ id: 1, name: '후보', distanceMeters: -1, reasonCodes: [] }] },
    { ...recommendation, preferred: [{ id: 1, name: '후보', distanceMeters: 0, reasonCodes: [123] }] },
  ])('깨진 추천 응답 %j를 거부한다', async data => {
    httpClient.defaults.adapter = async config => jsonResponse(config, data)
    await expect(getRecommendations(criteria, null)).rejects.toMatchObject({ kind: 'invalid_response' })
  })
})
