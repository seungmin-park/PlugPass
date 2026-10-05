import { connectors, type Connector } from '../charging-info/types'
import type { SearchCriteria, SearchCriteriaResult } from './types'
import { ApiError } from '../../shared/api/apiError'

const queryFields = ['latitude', 'longitude', 'radiusMeters', 'connector', 'limit'] as const
const decimal = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i
const integer = /^[+-]?\d+$/
const numericLabels = { latitude: '위도', longitude: '경도', radiusMeters: '반경', limit: '검색 개수' } as const

export function toSearchQuery(criteria: SearchCriteria): Record<string, string> {
  return { latitude: String(criteria.latitude), longitude: String(criteria.longitude),
    radiusMeters: String(criteria.radiusMeters), connector: criteria.connector, limit: String(criteria.limit) }
}

export function parseSearchCriteria(query: Record<string, unknown>): SearchCriteriaResult {
  if (!queryFields.some(field => Object.prototype.hasOwnProperty.call(query, field))) return { kind: 'empty' }
  const fields: Record<string, string> = {}
  const latitude = parseNumericQueryField(query.latitude, 'latitude', -90, 90, decimal, fields)
  const longitude = parseNumericQueryField(query.longitude, 'longitude', -180, 180, decimal, fields)
  const radiusMeters = parseNumericQueryField(query.radiusMeters, 'radiusMeters', 100, 10000, integer, fields)
  const limit = query.limit === undefined ? 20 : parseNumericQueryField(query.limit, 'limit', 1, 50, integer, fields)
  const connector = query.connector
  if (!isConnector(connector)) fields.connector = '지원하는 차량 커넥터를 한 번 선택해 주세요'
  if (Object.keys(fields).length > 0 || !isConnector(connector)) return { kind: 'invalid', fields }
  return { kind: 'valid', criteria: { latitude, longitude, radiusMeters, connector, limit } }
}

function parseNumericQueryField(value: unknown, field: keyof typeof numericLabels, minimum: number, maximum: number,
  format: RegExp, fields: Record<string, string>): number {
  if (typeof value !== 'string' || !format.test(value.trim())) {
    fields[field] = `${numericLabels[field]} 조건을 올바른 숫자로 한 번 입력해 주세요`
    return NaN
  }
  const parsedValue = Number(value)
  if (!Number.isFinite(parsedValue) || parsedValue < minimum || parsedValue > maximum) {
    fields[field] = `${numericLabels[field]} 값은 ${minimum} 이상 ${maximum} 이하여야 합니다`
  }
  return parsedValue
}

function isConnector(value: unknown): value is Connector {
  return connectors.some(connector => connector === value)
}

export function requireSearchCriteria(criteria: SearchCriteria): SearchCriteria {
  const result = parseSearchCriteria({
    latitude: String(criteria.latitude), longitude: String(criteria.longitude),
    radiusMeters: String(criteria.radiusMeters), connector: criteria.connector, limit: String(criteria.limit),
  })
  if (result.kind !== 'valid') {
    throw new ApiError('validation', '검색 조건을 확인해 주세요', null, 'INVALID_SEARCH_CRITERIA',
      result.kind === 'invalid' ? result.fields : {})
  }
  return result.criteria
}
