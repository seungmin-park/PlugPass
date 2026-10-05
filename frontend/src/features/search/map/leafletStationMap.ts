import { divIcon, latLngBounds, map, marker, tileLayer, type Map as LeafletMap, type Marker } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { MapCoordinates, MapStation, StationMapCallbacks, StationMapFactory } from './stationMap'

function addBackgroundTiles(stationMap: LeafletMap, callbacks: StationMapCallbacks): void {
  let tilesFailed = false
  tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    referrerPolicy: 'strict-origin-when-cross-origin',
    updateWhenIdle: true,
    keepBuffer: 0,
  }).on('tileerror', () => { tilesFailed = true; callbacks.tilesFailed() })
    .on('load', () => { if (!tilesFailed) callbacks.tilesLoaded() }).addTo(stationMap)
}

function addStationMarkers(stationMap: LeafletMap, stations: readonly MapStation[], callbacks: StationMapCallbacks): Map<number, Marker> {
  const stationMarkers = new Map<number, Marker>()
  for (const station of stations) {
    const label = document.createElement('span')
    label.textContent = '●'
    const stationMarker = marker([station.latitude, station.longitude], {
      title: `${station.name} 지도 마커`,
      keyboard: true,
      icon: divIcon({ html: label, className: 'station-map-marker', iconSize: [36, 36], iconAnchor: [18, 36] }),
    }).addTo(stationMap)
    const icon = stationMarker.getElement()
    icon?.setAttribute('aria-label', `${station.name} 지도 마커`)
    icon?.setAttribute('aria-pressed', 'false')
    stationMarker.on('click', () => callbacks.selectStation(station.id))
    stationMarker.on('keydown', event => {
      if (!('originalEvent' in event) || !(event.originalEvent instanceof KeyboardEvent)) return
      const keyboardEvent = event.originalEvent
      if (keyboardEvent.key !== 'Enter' && keyboardEvent.key !== ' ') return
      keyboardEvent.preventDefault()
      keyboardEvent.stopPropagation()
      callbacks.selectStation(station.id)
    })
    stationMarkers.set(station.id, stationMarker)
  }
  return stationMarkers
}

function frameStations(stationMap: LeafletMap, stations: readonly MapStation[], center: MapCoordinates): void {
  if (!stations.length) return
  const bounds = latLngBounds([[center.latitude, center.longitude]])
  for (const station of stations) bounds.extend([station.latitude, station.longitude])
  stationMap.fitBounds(bounds, { padding: [28, 28], maxZoom: 16, animate: false })
}

export const createLeafletStationMap: StationMapFactory = (container, stations, center, callbacks) => {
  const stationMap = map(container, { scrollWheelZoom: false, zoomAnimation: false, fadeAnimation: false, markerZoomAnimation: false })
  try {
    stationMap.setView([center.latitude, center.longitude], 14)
    addBackgroundTiles(stationMap, callbacks)
    const stationMarkers = addStationMarkers(stationMap, stations, callbacks)
    frameStations(stationMap, stations, center)
    return {
      selectStation(stationId) {
        for (const [markerId, stationMarker] of stationMarkers) {
          const selected = markerId === stationId
          const icon = stationMarker.getElement()
          icon?.setAttribute('aria-pressed', String(selected))
          icon?.classList.toggle('station-map-marker-selected', selected)
          stationMarker.setZIndexOffset(selected ? 1000 : 0)
        }
        const selectedMarker = stationId === null ? undefined : stationMarkers.get(stationId)
        if (selectedMarker) stationMap.panTo(selectedMarker.getLatLng(), { animate: false })
      },
      destroy() { stationMap.remove() },
    }
  } catch (failure: unknown) {
    stationMap.remove()
    throw failure
  }
}
