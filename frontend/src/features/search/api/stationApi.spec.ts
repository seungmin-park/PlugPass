import { afterEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, CanceledError, type AxiosAdapter } from 'axios'
import search from '../../../../tests/fixtures/search.json'
import { jsonResponse } from '../../../../tests/httpResponse'
import { httpClient } from '../../../shared/api/httpClient'
import { getStations } from './stationApi'

const originalAdapter = httpClient.defaults.adapter
if (originalAdapter === undefined) throw new Error("Axios 기본 adapter가 필요합니다")
afterEach(() => { httpClient.defaults.adapter = originalAdapter })
const criteria = { latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO' as const, limit: 20 }

describe('주변 검색 API', () => {
  it('정확한 경로·조건·10초 timeout·취소 signal을 보내고 nullable 시각을 보존한다', async () => {
    const signal = new AbortController().signal
    const adapter = vi.fn<AxiosAdapter>(async config => {
      expect(httpClient.getUri(config)).toBe('/api/v1/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20')
      return jsonResponse(config, search)
    })
    httpClient.defaults.adapter = adapter
    await expect(getStations(criteria, signal)).resolves.toEqual(search)
    expect(adapter).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({
      method: 'get', baseURL: '/api/v1', url: '/stations', params: criteria, timeout: 10000, signal, responseType: 'json',
    }))
  })
  it('아직 수집되지 않은 빈 결과와 null 시각을 보존한다', async () => {
    const empty = { dataReady: false, lastSuccessfulRunAt: null, stations: [] }
    httpClient.defaults.adapter = async config => jsonResponse(config, empty)
    await expect(getStations(criteria)).resolves.toEqual(empty)
  })
  it('새 상태·최신성 코드를 임의의 이용 가능 상태로 바꾸지 않는다', async () => {
    const future = { ...search, stations: search.stations.map(station => ({ ...station,
      chargers: station.chargers.map(charger => ({ ...charger, status: 'FUTURE_STATUS', freshness: 'FUTURE_FRESHNESS', reasonCode: 'FUTURE_REASON', sourceObservedAt: 'unparseable-time' })),
    })) }
    httpClient.defaults.adapter = async config => jsonResponse(config, future)
    await expect(getStations(criteria)).resolves.toEqual(future)
  })
  it.each([
    null, [], {}, { ...search, dataReady: 'true' }, { ...search, lastSuccessfulRunAt: 123 },
    { ...search, stations: {} }, { ...search, stations: search.stations.map(station => ({ ...station, id: 9007199254740992 })) },
    { ...search, stations: search.stations.map(station => ({ ...station, provider: ' ' })) },
    { ...search, stations: search.stations.map(station => ({ ...station, name: '' })) },
    { ...search, stations: search.stations.map(station => ({ ...station, chargers: station.chargers.map(charger => ({ ...charger, chargerId: '' })) })) },
    { ...search, stations: search.stations.map(station => ({ ...station, latitude: 91 })) },
    { ...search, stations: search.stations.map(station => ({ ...station, distanceMeters: -1 })) },
    { ...search, stations: search.stations.map(station => ({ ...station, compatibleChargerCount: 1.5 })) },
    { ...search, stations: search.stations.map(station => ({ ...station, reportedAvailableCount: -1 })) },
    { ...search, stations: search.stations.map(station => ({ ...station, chargers: station.chargers.map(charger => ({ ...charger, sourceObservedAt: undefined })) })) },
    { ...search, stations: search.stations.map(station => ({ ...station, chargers: station.chargers.map(charger => ({ ...charger, collectedAt: null })) })) },
    { ...search, stations: search.stations.map(station => ({ ...station, chargers: station.chargers.map(charger => ({ ...charger, note: 3 })) })) },
  ])('깨진 검색 응답 구조 %j를 거부한다', async data => {
    httpClient.defaults.adapter = async config => jsonResponse(config, data)
    await expect(getStations(criteria)).rejects.toMatchObject({ kind: 'invalid_response' })
  })
  it('잘못된 JSON을 조용히 문자열로 반환하지 않는다', async () => {
    httpClient.defaults.adapter = async config => ({ ...jsonResponse(config, {}), data: '{broken' })
    await expect(getStations(criteria)).rejects.toMatchObject({ kind: 'invalid_response' })
  })
  it.each([[400, 'validation'], [404, 'not_found'], [429, 'rate_limit'], [500, 'server'], [503, 'server']])('HTTP %s는 %s 오류이며 재시도하지 않는다', async (status, kind) => {
    const body = { code: 'INVALID_REQUEST', message: '요청 오류', fields: { radiusMeters: '반경 오류' } }
    const adapter = vi.fn<AxiosAdapter>(async config => {
      throw new AxiosError('failed', status >= 500 ? 'ERR_BAD_RESPONSE' : 'ERR_BAD_REQUEST', config, undefined, jsonResponse(config, body, status))
    })
    httpClient.defaults.adapter = adapter
    await expect(getStations(criteria)).rejects.toMatchObject({ kind, status, fields: body.fields })
    expect(adapter).toHaveBeenCalledTimes(1)
  })
  it.each([['ERR_NETWORK', 'network'], ['ECONNABORTED', 'timeout']])('%s도 재시도하지 않는다', async (code, kind) => {
    const adapter = vi.fn<AxiosAdapter>(async () => { throw new AxiosError('failure', code) })
    httpClient.defaults.adapter = adapter
    await expect(getStations(criteria)).rejects.toMatchObject({ kind })
    expect(adapter).toHaveBeenCalledTimes(1)
  })
  it('이미 취소된 요청은 전송하지 않는다', async () => {
    const controller = new AbortController()
    controller.abort()
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, search))
    httpClient.defaults.adapter = adapter
    await expect(getStations(criteria, controller.signal)).rejects.toMatchObject({ kind: 'cancelled' })
    expect(adapter).not.toHaveBeenCalled()
  })
  it('진행 중 요청을 취소하면 취소 오류로 끝낸다', async () => {
    const controller = new AbortController()
    const adapter = vi.fn<AxiosAdapter>(config => new Promise((_resolve, reject) => {
      config.signal?.addEventListener?.('abort', () => reject(new CanceledError('cancelled', config)), { once: true })
    }))
    httpClient.defaults.adapter = adapter
    const pending = getStations(criteria, controller.signal)
    const assertion = expect(pending).rejects.toMatchObject({ kind: 'cancelled' })
    controller.abort()
    await assertion
    expect(adapter).toHaveBeenCalledTimes(1)
  })
  it('잘못된 직접 입력은 네트워크 전에 거부한다', async () => {
    const adapter = vi.fn<AxiosAdapter>(async config => jsonResponse(config, search))
    httpClient.defaults.adapter = adapter
    await expect(getStations({ ...criteria, radiusMeters: 99 })).rejects.toMatchObject({ kind: 'validation', fields: { radiusMeters: expect.any(String) } })
    expect(adapter).not.toHaveBeenCalled()
  })
})
