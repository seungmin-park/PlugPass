import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter } from 'axios'
import detail from '../../../../tests/fixtures/detail.json'
import { jsonResponse } from '../../../../tests/httpResponse'
import { httpClient } from '../../../shared/api/httpClient'
import { getStationDetail } from './stationDetailApi'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error("Axios 기본 adapter가 필요합니다")
afterEach(() => { httpClient.defaults.adapter = originalAdapter })

describe('충전소 상세 API', () => {
  it('ID 경로·signal·timeout을 전달하고 서버 응답을 보존한다', async () => {
    const signal = new AbortController().signal
    const adapter = vi.fn<AxiosAdapter>(async config => {
      expect(httpClient.getUri(config)).toBe('/api/v1/stations/2')
      return jsonResponse(config, detail)
    })
    httpClient.defaults.adapter = adapter
    await expect(getStationDetail(2, signal)).resolves.toEqual(detail)
    expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({
      method: 'get', baseURL: '/api/v1', url: '/stations/2', signal, timeout: 10000,
    }))
  })
  it.each([0, -1, 1.5, NaN, Infinity, 9007199254740992])('ID %j는 전송 전에 거부한다', async stationId => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, detail))
    httpClient.defaults.adapter = adapter
    await expect(getStationDetail(stationId)).rejects.toMatchObject({ kind: 'validation', fields: { stationId: expect.any(String) } })
    expect(adapter).not.toHaveBeenCalled()
  })
  it.each([null, {}, { ...detail, id: -1 }, { ...detail, longitude: 181 }, { ...detail, chargers: null },
    { ...detail, name: ' ' },
    { ...detail, chargers: detail.chargers.map(charger => ({ ...charger, freshness: null })) },
  ])('깨진 상세 응답 %j를 거부한다', async data => {
    httpClient.defaults.adapter = async config => jsonResponse(config, data)
    await expect(getStationDetail(2)).rejects.toMatchObject({ kind: 'invalid_response' })
  })
})
