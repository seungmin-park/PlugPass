import type { Connector } from '../charging-info/types'
import { ApiError } from '../../shared/api/apiError'
import { isNumberInRange } from '../../shared/api/responseValidation'
import type { SearchCriteria } from './types'

export interface SearchArea { south: number; north: number; west: number; east: number }
export const areaFields = ['south', 'north', 'west', 'east'] as const

function cornerRadius(area: SearchArea): number {
  const latitude = (area.south + area.north) / 2
  const longitude = (area.west + area.east) / 2
  const distance = (cornerLatitude: number, cornerLongitude: number) => {
    const radians = Math.PI / 180
    const haversine = Math.sin((cornerLatitude - latitude) * radians / 2) ** 2
      + Math.cos(latitude * radians) * Math.cos(cornerLatitude * radians) * Math.sin((cornerLongitude - longitude) * radians / 2) ** 2
    return 2 * 6371008.8 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, haversine))))
  }
  return Math.max(distance(area.south, area.west), distance(area.north, area.east))
}
export function isSearchArea(area: SearchArea): boolean {
  return isNumberInRange(area.south, -90, 90) && isNumberInRange(area.north, -90, 90)
    && isNumberInRange(area.west, -180, 180) && isNumberInRange(area.east, -180, 180)
    && area.south < area.north && area.west < area.east
    && area.north - area.south <= 1 && area.east - area.west <= 1 && cornerRadius(area) <= 10000
}
export function searchCriteriaForArea(area: SearchArea, connector: Connector, limit = 20): SearchCriteria {
  if (!isSearchArea(area)) throw new ApiError('validation', '검색 영역을 확인하고 지도를 확대해 주세요', null, 'INVALID_SEARCH_AREA', { area: '지도를 확대해 주세요' })
  return { latitude: (area.south + area.north) / 2, longitude: (area.west + area.east) / 2,
    radiusMeters: Math.max(100, Math.ceil(cornerRadius(area))), connector, limit, area: { ...area } }
}
