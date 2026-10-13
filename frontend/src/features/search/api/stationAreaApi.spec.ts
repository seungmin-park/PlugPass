import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter } from 'axios'
import { httpClient } from '../../../shared/api/httpClient'
import { getStations } from './stationApi'
import { parseSearchCriteria, toSearchQuery } from '../criteria'
import { jsonResponse } from '../../../../tests/httpResponse'
import searchResponse from '../../../../tests/fixtures/search.json'
const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error('Axios adapter가 필요합니다')
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
const area = { south: 37.499, north: 37.501, west: 126.999, east: 127.001 }
const criteria = { latitude: 37.5, longitude: 127, radiusMeters: 143, connector: 'DC_COMBO' as const, limit: 20, area }

describe('화면 영역 검색의 HTTP·URL 계약', () => {
  it('영역이 있으면 서버 영역 endpoint에 경계와 커넥터만 전달한다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    await expect(getStations(criteria)).resolves.toEqual(searchResponse)
    expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ url: '/stations/area', params: { ...area, connector: 'DC_COMBO', limit: 20 } }))
  })
  it('확정한 영역은 URL 왕복 후에도 보존된다', () => {
    expect(parseSearchCriteria(toSearchQuery(criteria))).toEqual({ kind: 'valid', criteria })
  })
  it('부분 경계 URL을 반경 검색으로 조용히 처리하지 않는다', () => {
    expect(parseSearchCriteria({ ...toSearchQuery(criteria), north: undefined }).kind).toBe('invalid')
  })
  it('너무 넓은 영역은 HTTP 요청 전에 거부한다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, searchResponse))
    httpClient.defaults.adapter = adapter
    await expect(getStations({ ...criteria, area: { ...area, north: 38.5 } })).rejects.toMatchObject({ kind: 'validation' })
    expect(adapter).not.toHaveBeenCalled()
  })
})
