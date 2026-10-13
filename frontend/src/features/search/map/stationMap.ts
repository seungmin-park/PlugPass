import type { SearchArea } from '../searchArea'
export interface MapCoordinates {
  latitude: number
  longitude: number
}
export interface MapStation extends MapCoordinates {
  id: number
  name: string
}
export interface StationMapSession {
  zoomIn(): number
  zoomOut(): number
  getZoomLevel(): number
  selectStation(stationId: number | null): void
  updateStations(stations: readonly MapStation[]): void
  moveTo(center: MapCoordinates): void
  showArea(area: SearchArea): void
  destroy(): void
}
export interface StationMapCallbacks {
  selectStation(stationId: number): void
  areaChanged?(area: SearchArea | null): void
  zoomChanged?(level: number): void
  tilesLoaded(): void
  tilesFailed(): void
}
export type StationMapFactory = (
  container: HTMLElement,
  stations: readonly MapStation[],
  center: MapCoordinates,
  callbacks: StationMapCallbacks,
) => StationMapSession
