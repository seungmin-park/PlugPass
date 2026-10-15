import { nativeMap } from './nativeMapBridge'
import { observeNativeMapLayout, readNativeMapLayout } from './nativeMapLayout'
import type { MapCoordinates, MapZoomState, StationMapFactory } from './stationMap'
import { isSearchArea, type SearchArea } from '../searchArea'

function record(value: unknown): value is Record<string, unknown> { return typeof value === 'object' && value !== null }
function coordinates(value: MapCoordinates): boolean {
  return Number.isFinite(value.latitude) && Math.abs(value.latitude) <= 90 && Number.isFinite(value.longitude) && Math.abs(value.longitude) <= 180
}
function area(value: unknown): SearchArea | null {
  if (!record(value) || typeof value.south !== 'number' || typeof value.north !== 'number'
    || typeof value.west !== 'number' || typeof value.east !== 'number') return null
  const candidate = { south: value.south, north: value.north, west: value.west, east: value.east }
  return isSearchArea(candidate) ? candidate : null
}
function zoomState(value: unknown): MapZoomState | null {
  if (!record(value) || typeof value.canZoomIn !== 'boolean' || typeof value.canZoomOut !== 'boolean'
    || typeof value.description !== 'string' || value.description.length > 100) return null
  return { canZoomIn: value.canZoomIn, canZoomOut: value.canZoomOut, description: value.description }
}

export const createNativeStationMap: StationMapFactory = async (container, stations, center, callbacks) => {
  if (!coordinates(center)) throw new Error('지도 좌표를 확인해 주세요.')
  const sessionId = crypto.randomUUID()
  let alive = true
  let stationIds = new Set(stations.map(station => station.id))
  let stopLayout = () => {}
  let destruction: Promise<void> | null = null
  const listener = await nativeMap.addListener('mapEvent', event => {
    if (!alive || !record(event) || event.sessionId !== sessionId) return
    switch (event.type) {
      case 'ready': callbacks.ready(); break
      case 'failed': callbacks.failed(); break
      case 'moving':
        if (event.userInitiated === true) callbacks.interacted?.()
        callbacks.areaChanged?.(null)
        break
      case 'settled': {
        const zoom = zoomState(event.zoom)
        if (!zoom) { callbacks.failed(); return }
        callbacks.zoomChanged?.(zoom)
        callbacks.areaChanged?.(area(event.area))
        break
      }
      case 'selected':
        if (typeof event.stationId === 'number' && Number.isSafeInteger(event.stationId) && stationIds.has(event.stationId))
          callbacks.selectStation(event.stationId)
        break
    }
  })
  const destroy = (): Promise<void> => {
    if (destruction) return destruction
    alive = false
    stopLayout()
    destruction = Promise.all([listener.remove(), nativeMap.destroy({ sessionId })]).then(() => {})
    return destruction
  }
  try {
    await nativeMap.create({ sessionId, stations, center, layout: readNativeMapLayout(container) })
    stopLayout = observeNativeMapLayout(container, layout => {
      void nativeMap.updateLayout({ sessionId, layout }).catch(() => { if (alive) callbacks.failed() })
    })
  } catch (error) {
    await destroy()
    throw error
  }
  return {
    zoomIn: () => alive ? nativeMap.zoom({ sessionId, direction: 'in' }) : Promise.resolve(),
    zoomOut: () => alive ? nativeMap.zoom({ sessionId, direction: 'out' }) : Promise.resolve(),
    selectStation: stationId => alive ? nativeMap.selectStation({ sessionId, stationId }) : Promise.resolve(),
    updateStations: async updated => {
      if (!alive) return
      await nativeMap.updateStations({ sessionId, stations: updated })
      if (alive) stationIds = new Set(updated.map(station => station.id))
    },
    moveTo: next => alive ? nativeMap.moveTo({ sessionId, center: next }) : Promise.resolve(),
    showArea: next => alive ? nativeMap.showArea({ sessionId, area: next }) : Promise.resolve(),
    destroy,
  }
}
