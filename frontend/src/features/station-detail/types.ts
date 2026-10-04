import type { ChargerInfo } from '../charging-info/types'
export interface StationDetail {
  id: number
  provider: string
  providerStationId: string
  name: string
  latitude: number
  longitude: number
  chargers: ChargerInfo[]
}
