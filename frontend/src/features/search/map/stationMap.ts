export interface MapCoordinates {
  latitude: number
  longitude: number
}
export interface MapStation extends MapCoordinates {
  id: number
  name: string
}
export interface StationMapSession {
  selectStation(stationId: number | null): void
  destroy(): void
}
export interface StationMapCallbacks {
  selectStation(stationId: number): void
  tilesLoaded(): void
  tilesFailed(): void
}
export type StationMapFactory = (
  container: HTMLElement,
  stations: readonly MapStation[],
  center: MapCoordinates,
  callbacks: StationMapCallbacks,
) => StationMapSession
