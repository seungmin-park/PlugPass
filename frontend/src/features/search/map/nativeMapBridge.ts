import { registerPlugin, type PluginListenerHandle } from '@capacitor/core'
import type { MapCoordinates, MapStation } from './stationMap'
import type { SearchArea } from '../searchArea'

export interface MapRect { x: number; y: number; width: number; height: number }
export interface NativeMapLayout { frame: MapRect; clip: MapRect; visible: boolean; viewportHeight: number }
interface SessionRequest { sessionId: string }
export interface NativeMapPlugin {
  addListener(event: 'mapEvent', listener: (event: unknown) => void): Promise<PluginListenerHandle>
  create(options: SessionRequest & { center: MapCoordinates; stations: readonly MapStation[]; layout: NativeMapLayout }): Promise<void>
  updateLayout(options: SessionRequest & { layout: NativeMapLayout }): Promise<void>
  updateStations(options: SessionRequest & { stations: readonly MapStation[] }): Promise<void>
  selectStation(options: SessionRequest & { stationId: number | null }): Promise<void>
  moveTo(options: SessionRequest & { center: MapCoordinates }): Promise<void>
  showArea(options: SessionRequest & { area: SearchArea }): Promise<void>
  zoom(options: SessionRequest & { direction: 'in' | 'out' }): Promise<void>
  destroy(options: SessionRequest): Promise<void>
}

export const nativeMap = registerPlugin<NativeMapPlugin>('PlugPassMap')
