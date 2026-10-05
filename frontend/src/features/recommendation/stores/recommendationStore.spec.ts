import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getRecommendations } from '../api/recommendationApi'
import { getStations } from '../../search/api/stationApi'
import { useRecommendationStore } from './recommendationStore'
import { ApiError } from '../../../shared/api/apiError'
import type { RecommendationResponse } from '../types'
import type { StationSearchResponse } from '../../search/types'
import alternative from '../../../../tests/fixtures/alternative.json'
import search from '../../../../tests/fixtures/search.json'

vi.mock('../api/recommendationApi', () => ({ getRecommendations: vi.fn() }))
vi.mock('../../search/api/stationApi', () => ({ getStations: vi.fn() }))
beforeEach(() => { setActivePinia(createPinia()); vi.mocked(getRecommendations).mockReset(); vi.mocked(getStations).mockReset() })
const criteria = { latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO' as const, limit: 20 }
const emptyCandidates = { preferred: [], requiresConfirmation: [], excluded: [] }
describe('후보와 수집 메타의 독립 상태·순서', () => {
  it('로딩 상태를 구분하고 같은 조건과 제외 ID를 전달한다', async () => {
    let finishCandidates: (response: RecommendationResponse) => void = () => {}
    let finishMetadata: (response: StationSearchResponse) => void = () => {}
    vi.mocked(getRecommendations).mockImplementationOnce(() => new Promise(resolve => { finishCandidates = resolve }))
    vi.mocked(getStations).mockImplementationOnce(() => new Promise(resolve => { finishMetadata = resolve }))
    const store = useRecommendationStore()
    const pending = store.loadAlternatives(criteria, 2)
    expect(store.candidateStatus).toBe('loading')
    expect(store.metadataStatus).toBe('loading')
    finishCandidates(alternative)
    finishMetadata(search)
    await pending
    expect(store.candidates).toEqual(alternative)
    expect(store.metadata).toEqual(search)
    expect(store.candidatesFetchedAt).toEqual(expect.any(String))
    expect(store.metadataFetchedAt).toEqual(expect.any(String))
    expect(getRecommendations).toHaveBeenCalledWith(criteria, 2, expect.any(AbortSignal))
    expect(getStations).toHaveBeenCalledWith(criteria, expect.any(AbortSignal))
  })
  it('메타 실패는 후보를 유지하고 독립 재시도로 복구한다', async () => {
    vi.mocked(getRecommendations).mockResolvedValueOnce(alternative)
    vi.mocked(getStations).mockRejectedValueOnce(new ApiError('server', '메타 오류', 503)).mockResolvedValueOnce(search)
    const store = useRecommendationStore()
    await store.loadAlternatives(criteria, 2)
    expect(store.candidateStatus).toBe('success')
    expect(store.candidates).toEqual(alternative)
    expect(store.metadataStatus).toBe('error')
    await store.retryMetadata()
    expect(store.metadata).toEqual(search)
    expect(store.metadataError).toBeNull()
    expect(store.candidates).toEqual(alternative)
    expect(getRecommendations).toHaveBeenCalledTimes(1)
  })
  it('후보 실패는 메타를 유지하고 후보만 재시도한다', async () => {
    vi.mocked(getRecommendations).mockRejectedValueOnce(new ApiError('server', '후보 오류', 503)).mockResolvedValueOnce(alternative)
    vi.mocked(getStations).mockResolvedValueOnce(search)
    const store = useRecommendationStore()
    await store.loadAlternatives(criteria, 2)
    expect(store.candidateError?.message).toBe('후보 오류')
    expect(store.metadata).toEqual(search)
    await store.retryCandidates()
    expect(store.candidates).toEqual(alternative)
    expect(store.candidateError).toBeNull()
    expect(getStations).toHaveBeenCalledTimes(1)
  })
  it('취소를 무시한 이전 후보·메타의 성공도 새 조건을 덮지 않는다', async () => {
    let finishCandidates: (response: RecommendationResponse) => void = () => {}
    let finishMetadata: (response: StationSearchResponse) => void = () => {}
    vi.mocked(getRecommendations).mockImplementationOnce(() => new Promise(resolve => { finishCandidates = resolve })).mockResolvedValueOnce(emptyCandidates)
    vi.mocked(getStations).mockImplementationOnce(() => new Promise(resolve => { finishMetadata = resolve })).mockResolvedValueOnce({ dataReady: false, lastSuccessfulRunAt: null, stations: [] })
    const store = useRecommendationStore()
    const first = store.loadAlternatives(criteria, 2)
    await store.loadAlternatives({ ...criteria, radiusMeters: 3000 }, 3)
    finishCandidates(alternative)
    finishMetadata(search)
    await first
    expect(store.candidates).toEqual(emptyCandidates)
    expect(store.metadata?.dataReady).toBe(false)
    expect(vi.mocked(getRecommendations).mock.calls[0]?.[2]?.aborted).toBe(true)
    expect(vi.mocked(getStations).mock.calls[0]?.[1]?.aborted).toBe(true)
  })
  it('이전 후보·메타의 실패는 최신 성공을 변경하지 않는다', async () => {
    let failCandidates: (error: Error) => void = () => {}
    let failMetadata: (error: Error) => void = () => {}
    vi.mocked(getRecommendations).mockImplementationOnce(() => new Promise((_resolve, reject) => { failCandidates = reject })).mockResolvedValueOnce(emptyCandidates)
    vi.mocked(getStations).mockImplementationOnce(() => new Promise((_resolve, reject) => { failMetadata = reject })).mockResolvedValueOnce(search)
    const store = useRecommendationStore()
    const first = store.loadAlternatives(criteria, 2)
    await store.loadAlternatives(criteria, 3)
    failCandidates(new ApiError('server', '이전 후보 오류', 503))
    failMetadata(new ApiError('server', '이전 메타 오류', 503))
    await first
    expect(store.candidateStatus).toBe('success')
    expect(store.metadataStatus).toBe('success')
    expect(store.candidateError).toBeNull()
    expect(store.metadataError).toBeNull()
  })
  it('reset은 늦은 응답과 조건 없는 재시도를 무효화한다', async () => {
    let finish: (response: RecommendationResponse) => void = () => {}
    vi.mocked(getRecommendations).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
    vi.mocked(getStations).mockResolvedValueOnce(search)
    const store = useRecommendationStore()
    const pending = store.loadAlternatives(criteria, 2)
    store.reset()
    finish(alternative)
    await pending
    await store.retryCandidates()
    await store.retryMetadata()
    expect(store.candidates).toBeNull()
    expect(store.metadata).toBeNull()
    expect(store.candidateStatus).toBe('idle')
    expect(store.metadataStatus).toBe('idle')
    expect(getRecommendations).toHaveBeenCalledTimes(1)
    expect(getStations).toHaveBeenCalledTimes(1)
  })
})
