import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getStations } from '../api/stationApi'
import { ApiError } from '../../../shared/api/apiError'
import type { StationSearchResponse } from '../types'
import { useStationSearchStore } from './stationSearchStore'
import searchResponse from '../../../../tests/fixtures/search.json'

vi.mock('../api/stationApi', () => ({ getStations: vi.fn() }))
beforeEach(() => { setActivePinia(createPinia()); vi.mocked(getStations).mockReset() })
const criteria = { latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO' as const, limit: 20 }

describe('주변 검색 상태와 응답 순서', () => {
  it('초기 화면은 미검색이며 요청 중 이전 결과를 지운다', async () => {
    const store = useStationSearchStore()
    expect(store.status).toBe('idle')
    vi.mocked(getStations).mockResolvedValueOnce(searchResponse)
    await store.search(criteria)
    let finish: (value: StationSearchResponse) => void = () => {}
    vi.mocked(getStations).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
    const pending = store.search({ ...criteria, radiusMeters: 3000 })
    expect(store.status).toBe('loading')
    expect(store.response).toBeNull()
    expect(store.fetchedAt).toBeNull()
    finish({ dataReady: true, lastSuccessfulRunAt: null, stations: [] })
    await pending
    expect(store.status).toBe('success')
    expect(store.response?.stations).toEqual([])
  })
  it('검색 조건을 전달하고 API 결과와 조회 시각을 제공한다', async () => {
    vi.mocked(getStations).mockResolvedValueOnce(searchResponse)
    const store = useStationSearchStore()
    await store.search(criteria)
    expect(store.status).toBe('success')
    expect(store.response).toEqual(searchResponse)
    expect(store.fetchedAt).toEqual(expect.any(String))
    expect(store.error).toBeNull()
    expect(getStations).toHaveBeenCalledWith(criteria, expect.any(AbortSignal))
  })
  it.each([new ApiError('validation', '조건 오류', 400, 'INVALID_REQUEST', { radiusMeters: '반경 오류' }), new ApiError('server', '서버 오류', 503)])('오류 %s를 보존하고 재시도 성공 시 지운다', async failure => {
    vi.mocked(getStations).mockRejectedValueOnce(failure).mockResolvedValueOnce(searchResponse)
    const store = useStationSearchStore()
    await store.search(criteria)
    expect(store.status).toBe('error')
    expect(store.error).toEqual(failure)
    expect(store.response).toBeNull()
    await store.search(criteria)
    expect(store.status).toBe('success')
    expect(store.error).toBeNull()
    expect(store.response).toEqual(searchResponse)
  })
  it('이전 성공이 나중에 도착해도 최신 결과를 유지한다', async () => {
    let finishFirst: (value: StationSearchResponse) => void = () => {}
    vi.mocked(getStations).mockImplementationOnce(() => new Promise(resolve => { finishFirst = resolve }))
    vi.mocked(getStations).mockResolvedValueOnce({ dataReady: true, lastSuccessfulRunAt: null, stations: [] })
    const store = useStationSearchStore()
    const first = store.search(criteria)
    await store.search({ ...criteria, radiusMeters: 3000 })
    finishFirst(searchResponse)
    await first
    expect(store.status).toBe('success')
    expect(store.response?.stations).toEqual([])
    expect(vi.mocked(getStations).mock.calls[0]?.[1]?.aborted).toBe(true)
  })
  it('이전 실패가 나중에 도착해도 최신 성공을 오류로 바꾸지 않는다', async () => {
    let failFirst: (error: Error) => void = () => {}
    vi.mocked(getStations).mockImplementationOnce(() => new Promise((_resolve, reject) => { failFirst = reject }))
    vi.mocked(getStations).mockResolvedValueOnce(searchResponse)
    const store = useStationSearchStore()
    const first = store.search(criteria)
    await store.search({ ...criteria, connector: 'NACS' })
    failFirst(new ApiError('server', '늦은 오류', 503))
    await first
    expect(store.status).toBe('success')
    expect(store.error).toBeNull()
    expect(store.response).toEqual(searchResponse)
  })
  it('reset 후 도착한 응답은 초기 화면을 변경하지 않는다', async () => {
    let finish: (value: StationSearchResponse) => void = () => {}
    vi.mocked(getStations).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
    const store = useStationSearchStore()
    const pending = store.search(criteria)
    store.reset()
    finish(searchResponse)
    await pending
    expect(store.status).toBe('idle')
    expect(store.response).toBeNull()
    expect(store.error).toBeNull()
    expect(store.fetchedAt).toBeNull()
  })
})
