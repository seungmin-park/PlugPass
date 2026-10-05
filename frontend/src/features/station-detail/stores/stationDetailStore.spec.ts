import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getStationDetail } from '../api/stationDetailApi'
import { useStationDetailStore } from './stationDetailStore'
import { ApiError } from '../../../shared/api/apiError'
import type { StationDetail } from '../types'
import detail from '../../../../tests/fixtures/detail.json'

vi.mock('../api/stationDetailApi', () => ({ getStationDetail: vi.fn() }))
beforeEach(() => { setActivePinia(createPinia()); vi.mocked(getStationDetail).mockReset() })
describe('상세 요청 상태와 순서', () => {
  it('성공한 상세를 제공하고 재조회 중에는 이전 상세를 지운다', async () => {
    vi.mocked(getStationDetail).mockResolvedValueOnce(detail)
    const store = useStationDetailStore()
    expect(store.status).toBe('idle')
    await store.loadStation(2)
    expect(store.station).toEqual(detail)
    expect(store.fetchedAt).toEqual(expect.any(String))
    let finish: (station: StationDetail) => void = () => {}
    vi.mocked(getStationDetail).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
    const pending = store.loadStation(3)
    expect(store.status).toBe('loading')
    expect(store.station).toBeNull()
    expect(store.fetchedAt).toBeNull()
    finish({ ...detail, id: 3 })
    await pending
    expect(store.station?.id).toBe(3)
    expect(store.status).toBe('success')
  })
  it.each([new ApiError('not_found', '없는 충전소', 404), new ApiError('server', '서버 오류', 503)])('오류 %s와 재시도 결과를 제공한다', async failure => {
    vi.mocked(getStationDetail).mockRejectedValueOnce(failure).mockResolvedValueOnce(detail)
    const store = useStationDetailStore()
    await store.loadStation(2)
    expect(store.status).toBe('error')
    expect(store.error).toEqual(failure)
    expect(store.station).toBeNull()
    await store.loadStation(2)
    expect(store.status).toBe('success')
    expect(store.error).toBeNull()
    expect(store.station).toEqual(detail)
  })
  it('취소를 무시한 이전 성공도 새 상세를 덮지 않는다', async () => {
    let finish: (station: StationDetail) => void = () => {}
    vi.mocked(getStationDetail).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
    vi.mocked(getStationDetail).mockResolvedValueOnce({ ...detail, id: 3 })
    const store = useStationDetailStore()
    const first = store.loadStation(2)
    await store.loadStation(3)
    finish(detail)
    await first
    expect(store.station?.id).toBe(3)
    expect(vi.mocked(getStationDetail).mock.calls[0]?.[1]?.aborted).toBe(true)
  })
  it('늦은 이전 오류도 최신 성공을 오류로 바꾸지 않는다', async () => {
    let fail: (error: Error) => void = () => {}
    vi.mocked(getStationDetail).mockImplementationOnce(() => new Promise((_resolve, reject) => { fail = reject }))
    vi.mocked(getStationDetail).mockResolvedValueOnce({ ...detail, id: 3 })
    const store = useStationDetailStore()
    const first = store.loadStation(2)
    await store.loadStation(3)
    fail(new ApiError('server', '이전 오류', 503))
    await first
    expect(store.status).toBe('success')
    expect(store.error).toBeNull()
    expect(store.station?.id).toBe(3)
  })
  it('화면 이탈 후 늦은 응답은 비어 있는 상태를 유지한다', async () => {
    let finish: (station: StationDetail) => void = () => {}
    vi.mocked(getStationDetail).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
    const store = useStationDetailStore()
    const pending = store.loadStation(2)
    store.reset()
    finish(detail)
    await pending
    expect(store.status).toBe('idle')
    expect(store.station).toBeNull()
    expect(store.error).toBeNull()
    expect(store.fetchedAt).toBeNull()
  })
})
