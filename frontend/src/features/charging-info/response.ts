import type { ChargerInfo } from './types'
import { hasNullableStrings, hasNonblankStrings, isRecord } from '../../shared/api/responseValidation'

export function isChargerInfo(value: unknown): value is ChargerInfo {
  return isRecord(value)
    && hasNonblankStrings(value, ['chargerId', 'status', 'collectedAt', 'freshness', 'reasonCode'])
    && (value.connectors === undefined || (Array.isArray(value.connectors) && value.connectors.every(connector => typeof connector === 'string' && connector.trim().length > 0)))
    && hasNullableStrings(value, ['rawStatus', 'sourceObservedAt', 'connectorCode', 'useTime', 'limitYn',
      'limitDetail', 'note', 'sourceStatusChangedAtRaw', 'lastChargingStartedAtRaw', 'lastChargingEndedAtRaw',
      'chargingStartedAtRaw', 'delYn', 'delDetail'])
}
