import { beforeEach, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getPlaces, type PlaceMatch } from '../api/placeApi'
import { ApiError } from '../../../shared/api/apiError'
import { usePlaceSearchStore } from './placeSearchStore'
vi.mock('../api/placeApi', () => ({ getPlaces: vi.fn() }))
beforeEach(() => { setActivePinia(createPinia()); vi.mocked(getPlaces).mockReset() })
it('조회 중 표시 후 빈 결과로 완료하고 오류 후 다시 검색할 수 있다', async () => {
  let finish: (places: PlaceMatch[]) => void = () => {}
  vi.mocked(getPlaces).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
  const store = usePlaceSearchStore()
  const pending = store.search('서울')
  expect(store.status).toBe('loading')
  finish([])
  await pending
  expect(store.status).toBe('success')
  expect(store.places).toEqual([])
  vi.mocked(getPlaces).mockRejectedValueOnce(new ApiError('server', '주소 검색 준비 중'))
  await store.search('부산')
  expect(store.status).toBe('error')
  expect(store.error?.message).toBe('주소 검색 준비 중')
  vi.mocked(getPlaces).mockResolvedValueOnce([{ name: '부산', address: '', latitude: 35, longitude: 129 }])
  await store.search('부산')
  expect(store.error).toBeNull()
  expect(store.places).toEqual([{ name: '부산', address: '', latitude: 35, longitude: 129 }])
})
it('늦은 이전 성공이 새 검색 결과를 덮지 않는다', async () => {
  let finish: (places: PlaceMatch[]) => void = () => {}
  vi.mocked(getPlaces).mockImplementationOnce(() => new Promise(resolve => { finish = resolve })).mockResolvedValueOnce([])
  const store = usePlaceSearchStore()
  const pending = store.search('서울')
  await store.search('부산')
  finish([{ name: '서울', address: '', latitude: 37.5, longitude: 127 }])
  await pending
  expect(store.status).toBe('success')
  expect(store.places).toEqual([])
  expect(vi.mocked(getPlaces).mock.calls[0]?.[1]?.aborted).toBe(true)
})
it('입력 변경으로 취소한 요청의 늦은 오류는 화면에 남지 않는다', async () => {
  let fail: (error: Error) => void = () => {}
  vi.mocked(getPlaces).mockImplementationOnce(() => new Promise((_resolve, reject) => { fail = reject }))
  const store = usePlaceSearchStore()
  const pending = store.search('서울')
  store.reset()
  fail(new Error('late'))
  await pending
  expect(store.status).toBe('idle')
  expect(store.error).toBeNull()
  expect(store.places).toEqual([])
})
