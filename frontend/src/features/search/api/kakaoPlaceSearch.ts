import type { PlaceMatch } from './placeApi'
import { ApiError } from '../../../shared/api/apiError'
import { isRecord } from '../../../shared/api/responseValidation'

export interface KakaoPlaceMaps {
  services: {
    Status: { OK: string; ZERO_RESULT: string; ERROR: string }
    Geocoder: new () => { addressSearch(query: string, callback: (results: unknown[], status: string) => void, options: { size: number }): void }
    Places: new () => { keywordSearch(query: string, callback: (results: unknown[], status: string) => void, options: { size: number }): void }
  }
}

export function isKakaoPlaceMaps(value: unknown): value is KakaoPlaceMaps {
  if (!isRecord(value) || !isRecord(value.services)) return false
  const services = value.services
  return isRecord(services.Status) && typeof services.Status.OK === 'string'
    && typeof services.Status.ZERO_RESULT === 'string' && typeof services.Status.ERROR === 'string'
    && typeof services.Geocoder === 'function' && typeof services.Places === 'function'
}

function readCoordinate(value: unknown, minimum: number, maximum: number): number {
  const number = typeof value === 'string' || typeof value === 'number' ? Number(value) : Number.NaN
  if (!Number.isFinite(number) || number < minimum || number > maximum) throw new ApiError('invalid_response', '주소 검색 결과의 위치를 읽을 수 없습니다')
  return number
}

function readAddress(value: unknown): PlaceMatch {
  if (!isRecord(value) || typeof value.address_name !== 'string' || !value.address_name.trim())
    throw new ApiError('invalid_response', '주소 검색 결과를 읽을 수 없습니다')
  const address = isRecord(value.road_address) && typeof value.road_address.address_name === 'string'
    ? value.road_address.address_name : value.address_name
  return { name: address, address, latitude: readCoordinate(value.y, -90, 90), longitude: readCoordinate(value.x, -180, 180) }
}

function readKeyword(value: unknown): PlaceMatch {
  if (!isRecord(value) || typeof value.place_name !== 'string' || !value.place_name.trim())
    throw new ApiError('invalid_response', '장소 검색 결과를 읽을 수 없습니다')
  const address = typeof value.road_address_name === 'string' && value.road_address_name.trim()
    ? value.road_address_name : value.address_name
  if (typeof address !== 'string') throw new ApiError('invalid_response', '장소 주소를 읽을 수 없습니다')
  return { name: value.place_name, address, latitude: readCoordinate(value.y, -90, 90), longitude: readCoordinate(value.x, -180, 180) }
}

function requestResults(
  start: (callback: (results: unknown[], status: string) => void) => void,
  statuses: KakaoPlaceMaps['services']['Status'],
  signal?: AbortSignal,
): Promise<unknown[]> {
  if (signal?.aborted) return Promise.reject(new ApiError('cancelled', '요청을 취소했습니다'))
  return new Promise((resolve, reject) => {
    let settled = false
    const finish = (result: unknown[] | ApiError) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      signal?.removeEventListener('abort', onAbort)
      if (result instanceof ApiError) reject(result)
      else resolve(result)
    }
    const onAbort = () => finish(new ApiError('cancelled', '요청을 취소했습니다'))
    const timer = setTimeout(() => finish(new ApiError('timeout', '주소 검색 응답 시간이 초과됐습니다')), 8_000)
    signal?.addEventListener('abort', onAbort, { once: true })
    try {
      start((results, status) => {
        if (status === statuses.ZERO_RESULT) { finish([]); return }
        if (status === statuses.OK && Array.isArray(results)) { finish(results); return }
        finish(new ApiError('server', '주소 검색 서비스에 연결할 수 없습니다. 다시 시도해 주세요.'))
      })
    } catch { finish(new ApiError('server', '주소 검색 서비스에 연결할 수 없습니다. 다시 시도해 주세요.')) }
  })
}

export async function searchKakaoPlaces(query: string, maps: KakaoPlaceMaps, signal?: AbortSignal): Promise<PlaceMatch[]> {
  const geocoder = new maps.services.Geocoder()
  const places = new maps.services.Places()
  const statuses = maps.services.Status
  const [addresses, keywords] = await Promise.all([
    requestResults(callback => geocoder.addressSearch(query, callback, { size: 5 }), statuses, signal),
    requestResults(callback => places.keywordSearch(query, callback, { size: 10 }), statuses, signal),
  ])
  const matches = [...addresses.map(readAddress), ...keywords.map(readKeyword)]
  const seen = new Set<string>()
  return matches.filter(match => {
    const coordinate = `${match.latitude}:${match.longitude}`
    if (seen.has(coordinate)) return false
    seen.add(coordinate)
    return true
  }).slice(0, 10)
}
