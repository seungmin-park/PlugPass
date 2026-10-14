import type { CollectionStatus } from '../search/types'
import type { ChargerInfo } from '../charging-info/types'
export interface StationDetail {
  collection?: CollectionStatus | null
  id: number
  provider: string
  providerStationId: string
  name: string
  latitude: number
  longitude: number
  chargers: ChargerInfo[]
}
