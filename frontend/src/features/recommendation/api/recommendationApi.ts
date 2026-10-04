import type { RecommendationResponse, RecommendedStation } from '../types'
import type { SearchCriteria } from '../../search/types'
import { ApiError } from '../../../shared/api/apiError'
import { requireSearchCriteria } from '../../search/criteria'
import { getValidatedJson } from '../../../shared/api/httpClient'
import { isStationId } from '../../../shared/api/stationId'
import { isRecord, isNonnegativeNumber, isNonblankString } from '../../../shared/api/responseValidation'

export async function getRecommendations(criteria: SearchCriteria, excludeStationId: number | null = null,
  signal?: AbortSignal): Promise<RecommendationResponse> {
  if (excludeStationId !== null && !isStationId(excludeStationId)) {
    throw new ApiError('validation', '제외 충전소 ID를 확인해 주세요', null, 'INVALID_STATION_ID',
      { excludeStationId: '제외 충전소 ID는 양의 안전 정수여야 합니다' })
  }
  const params = { ...requireSearchCriteria(criteria), ...(excludeStationId !== null ? { excludeStationId } : {}) }
  return getValidatedJson('/recommendations', isRecommendationResponse, { params, ...(signal ? { signal } : {}) })
}

function isRecommendationResponse(value: unknown): value is RecommendationResponse {
  return isRecord(value) && ['preferred', 'requiresConfirmation', 'excluded'].every(group => {
    const candidates = value[group]
    return Array.isArray(candidates) && candidates.every(isRecommendedStation)
  })
}

function isRecommendedStation(value: unknown): value is RecommendedStation {
  return isRecord(value) && isStationId(value.id) && isNonblankString(value.name)
    && isNonnegativeNumber(value.distanceMeters) && Array.isArray(value.reasonCodes)
    && value.reasonCodes.every(isNonblankString)
}
