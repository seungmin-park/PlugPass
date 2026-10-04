import type { SearchCriteria, StationSearchResponse, StationSummary } from '../types'
import { requireSearchCriteria } from '../criteria'
import { isChargerInfo } from '../../charging-info/response'
import { getValidatedJson } from '../../../shared/api/httpClient'
import { isStationId } from '../../../shared/api/stationId'
import { hasNonblankStrings, isNullableString, isRecord, isNumberInRange, isNonnegativeInteger, isNonnegativeNumber } from '../../../shared/api/responseValidation'

export async function getStations(criteria: SearchCriteria, signal?: AbortSignal): Promise<StationSearchResponse> {
  return getValidatedJson('/stations', isSearchResponse, { params: requireSearchCriteria(criteria), ...(signal ? { signal } : {}) })
}

function isSearchResponse(value: unknown): value is StationSearchResponse {
  return isRecord(value) && typeof value.dataReady === 'boolean' && isNullableString(value.lastSuccessfulRunAt)
    && Array.isArray(value.stations) && value.stations.every(isStationSummary)
}

function isStationSummary(value: unknown): value is StationSummary {
  return isRecord(value) && isStationId(value.id) && hasNonblankStrings(value, ['provider', 'providerStationId', 'name'])
    && isNumberInRange(value.latitude, -90, 90) && isNumberInRange(value.longitude, -180, 180)
    && isNonnegativeNumber(value.distanceMeters) && isNonnegativeInteger(value.compatibleChargerCount)
    && isNonnegativeInteger(value.reportedAvailableCount) && Array.isArray(value.chargers) && value.chargers.every(isChargerInfo)
}
