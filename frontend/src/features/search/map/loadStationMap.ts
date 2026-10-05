import type { StationMapFactory } from './stationMap'

export async function loadStationMap(): Promise<StationMapFactory> {
  const sdk = await import('./leafletStationMap')
  return sdk.createLeafletStationMap
}
