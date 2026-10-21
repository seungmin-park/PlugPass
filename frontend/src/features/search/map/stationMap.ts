import type { SearchArea } from '../searchArea'
export interface MapCoordinates {
  latitude: number
  longitude: number
}
export interface MapStation extends MapCoordinates {
  id: number
  name: string
}
export interface MapZoomState {
  canZoomIn: boolean
  canZoomOut: boolean
  description: string
}
export interface StationMapSession {
  zoomIn(): Promise<void>
  zoomOut(): Promise<void>
  selectStation(stationId: number | null): Promise<void>
  updateStations(stations: readonly MapStation[]): Promise<void>
  moveTo(center: MapCoordinates): Promise<void>
  showArea(area: SearchArea): Promise<void>
  /** Invalidate this session immediately, including pending work; repeated calls are safe. */
  destroy(): Promise<void>
}
export interface StationMapCallbacks {
  selectStation(stationId: number): void
  /** Native gestures do not bubble DOM pointer events to the search screen. */
  interacted?(): void
  /** null means movement started; settled movement supplies the actual viewport. */
  areaChanged?(area: SearchArea | null): void
  zoomChanged?(state: MapZoomState): void
  ready(): void
  failed(): void
}
export type StationMapFactory = (
  container: HTMLElement,
  stations: readonly MapStation[],
  center: MapCoordinates,
  callbacks: StationMapCallbacks,
) => Promise<StationMapSession>
