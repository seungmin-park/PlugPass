import type { StationDetail } from '../types'
import { ApiError } from '../../../shared/api/apiError'
import { getValidatedJson } from '../../../shared/api/httpClient'
import { isStationId } from '../../../shared/api/stationId'
import { hasNonblankStrings, isRecord, isNumberInRange } from '../../../shared/api/responseValidation'
import { isChargerInfo } from '../../charging-info/response'

export async function getStationDetail(stationId: number, signal?: AbortSignal): Promise<StationDetail> {
  if (!isStationId(stationId)) throw new ApiError('validation', '충전소 ID를 확인해 주세요', null, 'INVALID_STATION_ID',
    { stationId: '충전소 ID는 양의 안전 정수여야 합니다' })
  return getValidatedJson(`/stations/${stationId}`, isStationDetail, signal ? { signal } : {})
}

function isStationDetail(value: unknown): value is StationDetail {
  return isRecord(value) && isStationId(value.id) && hasNonblankStrings(value, ['provider', 'providerStationId', 'name'])
    && isNumberInRange(value.latitude, -90, 90) && isNumberInRange(value.longitude, -180, 180)
    && Array.isArray(value.chargers) && value.chargers.every(isChargerInfo)
}
