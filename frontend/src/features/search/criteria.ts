import { areaFields, isSearchArea, type SearchArea } from './searchArea'
import { connectors, type Connector } from '../charging-info/types'
import type { SearchCriteria, SearchCriteriaResult } from './types'
import { ApiError } from '../../shared/api/apiError'
import { resolveVehicleCompatibility } from '../vehicle/vehicleCompatibility'
import { vehicleCatalog } from '../vehicle/vehicleCatalog'

const queryFields = ['latitude', 'longitude', 'radiusMeters', 'connector', 'connectors', 'limit', 'vehicleId', 'adapterIds', 'vehicleSupportConfirmed'] as const
const decimal = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i
const integer = /^[+-]?\d+$/
const numericLabels = { latitude: '위도', longitude: '경도', radiusMeters: '반경', limit: '검색 개수' } as const

export function toSearchQuery(criteria: SearchCriteria): Record<string, string> {
  return { latitude: String(criteria.latitude), longitude: String(criteria.longitude),
    radiusMeters: String(criteria.radiusMeters),
    ...(criteria.connectors ? { connectors: criteria.connectors.join(',') } : { connector: criteria.connector }),
    limit: String(criteria.limit),
    ...(criteria.vehicleId ? { vehicleId: criteria.vehicleId } : {}),
    ...(criteria.adapterIds ? { adapterIds: criteria.adapterIds.join(',') } : {}),
    ...(criteria.vehicleSupportConfirmed ? { vehicleSupportConfirmed: '1' } : {}),
    ...(criteria.area ? Object.fromEntries(areaFields.map(field => [field, String(criteria.area?.[field])])) : {}) }
}

export function parseSearchCriteria(query: Record<string, unknown>): SearchCriteriaResult {
  if (![...queryFields, ...areaFields].some(field => Object.prototype.hasOwnProperty.call(query, field))) return { kind: 'empty' }
  const fields: Record<string, string> = {}
  const latitude = parseNumericQueryField(query.latitude, 'latitude', -90, 90, decimal, fields)
  const longitude = parseNumericQueryField(query.longitude, 'longitude', -180, 180, decimal, fields)
  const radiusMeters = parseNumericQueryField(query.radiusMeters, 'radiusMeters', 100, 10000, integer, fields)
  const limit = query.limit === undefined ? 20 : parseNumericQueryField(query.limit, 'limit', 1, 50, integer, fields)
  let area: SearchArea | undefined
  if (areaFields.some(field => Object.prototype.hasOwnProperty.call(query, field))) {
    const values = areaFields.map(field => typeof query[field] === 'string' && decimal.test(query[field].trim()) ? Number(query[field]) : NaN)
    area = { south: values[0] ?? NaN, north: values[1] ?? NaN, west: values[2] ?? NaN, east: values[3] ?? NaN }
    if (!isSearchArea(area)) fields.area = '검색 영역을 확인하고 지도를 확대해 주세요'
  }
  const parsedConnectors = parseConnectors(query, fields)
  const connector = parsedConnectors[0]
  const profile = parseVehicleQuery(query, parsedConnectors, fields)
  if (Object.keys(fields).length > 0 || !connector) return { kind: 'invalid', fields }
  return { kind: 'valid', criteria: { latitude, longitude, radiusMeters, connector, limit,
    ...(query.connectors !== undefined ? { connectors: parsedConnectors } : {}), ...profile, ...(area ? { area } : {}) } }
}

function parseConnectors(query: Record<string, unknown>, fields: Record<string, string>): Connector[] {
  if (query.connectors !== undefined) {
    if (query.connector !== undefined) fields.connectors = '단일 규격과 여러 규격을 함께 사용할 수 없습니다'
    const values = typeof query.connectors === 'string' ? query.connectors.split(',') : []
    if (!values.length || values.some(value => !isConnector(value)) || new Set(values).size !== values.length) {
      fields.connectors = '사용할 충전 규격을 다시 확인해 주세요'
      return []
    }
    return values as Connector[]
  }
  if (!isConnector(query.connector)) {
    fields.connector = '지원하는 차량 커넥터를 한 번 선택해 주세요'
    return []
  }
  return [query.connector]
}

function parseVehicleQuery(query: Record<string, unknown>, selected: readonly Connector[], fields: Record<string, string>):
  Pick<SearchCriteria, 'vehicleId' | 'adapterIds' | 'vehicleSupportConfirmed'> {
  if (query.vehicleId === undefined && query.adapterIds === undefined && query.vehicleSupportConfirmed === undefined) return {}
  if (typeof query.vehicleId !== 'string' || !vehicleCatalog.some(vehicle => vehicle.id === query.vehicleId)) {
    fields.vehicleId = '차량 정보를 다시 선택해 주세요'
    return {}
  }
  const adapterIds = query.adapterIds === undefined || query.adapterIds === '' ? []
    : typeof query.adapterIds === 'string' ? query.adapterIds.split(',') : null
  if (!adapterIds || new Set(adapterIds).size !== adapterIds.length || ![undefined, '1'].includes(query.vehicleSupportConfirmed as string | undefined)) {
    fields.vehicleId = '차량과 어댑터 선택을 다시 확인해 주세요'
    return {}
  }
  const vehicleSupportConfirmed = query.vehicleSupportConfirmed === '1'
  const compatibility = resolveVehicleCompatibility({ vehicleId: query.vehicleId, adapterIds, vehicleSupportConfirmed })
  if (compatibility.requiresConfirmation || selected.length !== compatibility.connectors.length
    || selected.some(connector => !compatibility.connectors.includes(connector))) {
    fields.vehicleId = '차량과 이번에 사용할 어댑터를 확인해 주세요'
    return {}
  }
  return { vehicleId: query.vehicleId, adapterIds, ...(vehicleSupportConfirmed ? { vehicleSupportConfirmed } : {}) }
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
  const result = parseSearchCriteria(toSearchQuery(criteria))
  if (result.kind !== 'valid') {
    throw new ApiError('validation', '검색 조건을 확인해 주세요', null, 'INVALID_SEARCH_CRITERIA',
      result.kind === 'invalid' ? result.fields : {})
  }
  return result.criteria
}
