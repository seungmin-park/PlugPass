import type { CollectionStatus, SearchCriteria, StationSearchResponse, StationSummary } from '../types'
import { requireSearchCriteria } from '../criteria'
import { isChargerInfo } from '../../charging-info/response'
import { getValidatedJson } from '../../../shared/api/httpClient'
import { isStationId } from '../../../shared/api/stationId'
import { hasNonblankStrings, isNullableString, isRecord, isNumberInRange, isNonnegativeInteger, isNonnegativeNumber } from '../../../shared/api/responseValidation'

export async function getStations(criteria: SearchCriteria, signal?: AbortSignal): Promise<StationSearchResponse> {
  const validated = requireSearchCriteria(criteria)
  const selection = validated.connectors ? { connectors: validated.connectors.join(',') } : { connector: validated.connector }
  const params = validated.area ? { ...validated.area, ...selection, limit: validated.limit }
    : { latitude: validated.latitude, longitude: validated.longitude, radiusMeters: validated.radiusMeters, ...selection, limit: validated.limit }
  return getValidatedJson(validated.area ? '/stations/area' : '/stations', isSearchResponse, { params, ...(signal ? { signal } : {}) })
}

function isSearchResponse(value: unknown): value is StationSearchResponse {
  return isRecord(value) && typeof value.dataReady === 'boolean' && isNullableString(value.lastSuccessfulRunAt)
    && (value.collection === undefined || value.collection === null || isCollectionStatus(value.collection))
    && Array.isArray(value.stations) && value.stations.every(isStationSummary)
}

function isStationSummary(value: unknown): value is StationSummary {
  return isRecord(value) && isStationId(value.id) && hasNonblankStrings(value, ['provider', 'providerStationId', 'name'])
    && isNumberInRange(value.latitude, -90, 90) && isNumberInRange(value.longitude, -180, 180)
    && isNonnegativeNumber(value.distanceMeters) && isNonnegativeInteger(value.compatibleChargerCount)
    && isNonnegativeInteger(value.reportedAvailableCount) && Array.isArray(value.chargers) && value.chargers.every(isChargerInfo)
}

export function isCollectionStatus(value: unknown): value is CollectionStatus {
  return isRecord(value) && ['NOT_READY', 'CURRENT', 'DELAYED'].includes(String(value.state))
    && isNullableString(value.lastCatalogSuccessAt) && isNullableString(value.lastStatusSuccessAt)
    && isNullableString(value.lastAttemptAt) && isNullableString(value.lastAttemptStatus)
    && isNonnegativeInteger(value.statusIntervalSeconds) && value.statusIntervalSeconds > 0
}
