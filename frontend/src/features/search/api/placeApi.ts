import { getValidatedJson } from '../../../shared/api/httpClient'
import { ApiError } from '../../../shared/api/apiError'
import { isRecord, isNumberInRange } from '../../../shared/api/responseValidation'
export interface PlaceMatch { name: string; address: string; latitude: number; longitude: number }
interface PlaceResponse { places: PlaceMatch[] }
export async function getPlaces(query: string, signal?: AbortSignal): Promise<PlaceMatch[]> {
  const normalized = query.trim()
  if (!normalized || normalized.length > 100) throw new ApiError('validation', '주소나 장소를 1~100자로 입력해 주세요')
  const kakaoKey = import.meta.env.VITE_KAKAO_JS_KEY
  if (kakaoKey) {
    const [{ loadKakaoMaps }, { isKakaoPlaceMaps, searchKakaoPlaces }] = await Promise.all([
      import('../map/kakaoStationMap'), import('./kakaoPlaceSearch'),
    ])
    const maps = await loadKakaoMaps(kakaoKey)
    if (!isKakaoPlaceMaps(maps)) throw new ApiError('server', '주소 검색 서비스를 불러오지 못했습니다. 다시 시도해 주세요.')
    return searchKakaoPlaces(normalized, maps, signal)
  }
  const response = await getValidatedJson('/places', isPlaceResponse, { params: { query: normalized }, ...(signal ? { signal } : {}) })
  return response.places
}
function isPlaceResponse(value: unknown): value is PlaceResponse {
  return isRecord(value) && Array.isArray(value.places) && value.places.length <= 10 && value.places.every(place =>
    isRecord(place) && typeof place.name === 'string' && place.name.trim().length > 0 && typeof place.address === 'string'
    && isNumberInRange(place.latitude, -90, 90) && isNumberInRange(place.longitude, -180, 180))
}
