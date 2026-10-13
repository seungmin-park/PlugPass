import type { ChargerInfo, Connector } from '../charging-info/types'
import type { SearchArea } from './searchArea'
export interface SearchCriteria {
  area?: SearchArea
  latitude: number
  longitude: number
  radiusMeters: number
  connector: Connector
  connectors?: readonly Connector[]
  vehicleId?: string
  adapterIds?: readonly string[]
  vehicleSupportConfirmed?: boolean
  limit: number
}
export type SearchConditions = Pick<SearchCriteria, 'connector' | 'connectors' | 'vehicleId' | 'adapterIds' | 'vehicleSupportConfirmed'>
export type SearchCriteriaResult =
  | { kind: 'empty' }
  | { kind: 'valid'; criteria: SearchCriteria }
  | { kind: 'invalid'; fields: Record<string, string> }
export interface StationSummary {
  id: number
  provider: string
  providerStationId: string
  name: string
  latitude: number
  longitude: number
  distanceMeters: number
  compatibleChargerCount: number
  reportedAvailableCount: number
  chargers: ChargerInfo[]
}
export interface CollectionStatus {
  state: 'NOT_READY' | 'CURRENT' | 'DELAYED'
  lastCatalogSuccessAt: string | null
  lastStatusSuccessAt: string | null
  lastAttemptAt: string | null
  lastAttemptStatus: string | null
  statusIntervalSeconds: number
}
export interface StationSearchResponse {
  collection?: CollectionStatus | null
  dataReady: boolean
  lastSuccessfulRunAt: string | null
  stations: StationSummary[]
}
