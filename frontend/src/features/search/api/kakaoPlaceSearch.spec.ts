import { expect, it } from 'vitest'
import { searchKakaoPlaces, type KakaoPlaceMaps } from './kakaoPlaceSearch'

const address = { address_name: '서울특별시 중구 세종대로 110', x: '126.9784', y: '37.5663', road_address: { address_name: '서울특별시 중구 세종대로 110' } }
const keyword = { place_name: '서울시청', road_address_name: '서울특별시 중구 세종대로 110', address_name: '서울 중구 태평로1가', x: '126.9784', y: '37.5663' }

function maps(addressStatus: string, addressResults: unknown[], keywordStatus: string, keywordResults: unknown[]): KakaoPlaceMaps {
  return { services: {
    Status: { OK: 'OK', ZERO_RESULT: 'ZERO_RESULT', ERROR: 'ERROR' },
    Geocoder: class { addressSearch(_query: string, callback: (results: unknown[], status: string) => void) { callback(addressResults, addressStatus) } },
    Places: class { keywordSearch(_query: string, callback: (results: unknown[], status: string) => void) { callback(keywordResults, keywordStatus) } },
  } }
}

it('주소와 장소를 함께 찾아 좌표가 같은 후보는 한 번만 보여 준다', async () => {
  const result = await searchKakaoPlaces('서울시청', maps('OK', [address], 'OK', [keyword]))
  expect(result).toEqual([{ name: '서울특별시 중구 세종대로 110', address: '서울특별시 중구 세종대로 110', latitude: 37.5663, longitude: 126.9784 }])
})

it('두 검색 모두 결과가 없으면 빈 목록을 반환한다', async () => {
  await expect(searchKakaoPlaces('없는 곳', maps('ZERO_RESULT', [], 'ZERO_RESULT', []))).resolves.toEqual([])
})

it('공급자 오류를 빈 결과로 숨기지 않는다', async () => {
  await expect(searchKakaoPlaces('서울시청', maps('ERROR', [], 'ZERO_RESULT', []))).rejects.toMatchObject({ kind: 'server' })
})

it('취소된 검색의 늦은 결과는 사용할 수 없다', async () => {
  const controller = new AbortController()
  controller.abort()
  await expect(searchKakaoPlaces('서울시청', maps('OK', [address], 'OK', [keyword]), controller.signal))
    .rejects.toMatchObject({ kind: 'cancelled' })
})
