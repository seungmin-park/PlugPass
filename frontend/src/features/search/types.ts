import type { ChargerInfo, Connector } from '../charging-info/types'
export interface SearchCriteria {
  latitude: number
  longitude: number
  radiusMeters: number
  connector: Connector
  limit: number
}
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
export interface StationSearchResponse {
  dataReady: boolean
  lastSuccessfulRunAt: string | null
  stations: StationSummary[]
}
