interface KakaoWindow extends Window {
  kakao?: { maps: KakaoMaps & KakaoMapApi }
}

export interface KakaoMaps {
  load(ready: () => void): void
}

interface KakaoLatLng {
  getLat(): number
  getLng(): number
}

interface KakaoBounds {
  getSouthWest(): KakaoLatLng
  getNorthEast(): KakaoLatLng
}

interface KakaoMap {
  getLevel(): number
  setLevel(level: number): void
  setCenter(position: KakaoLatLng): void
  setBounds(bounds: KakaoBounds): void
  getBounds(): KakaoBounds
}

interface KakaoOverlay {
  setMap(map: KakaoMap | null): void
}

interface KakaoMapApi {
  Map: new (container: HTMLElement, options: { center: KakaoLatLng; level: number; scrollwheel: boolean; keyboardShortcuts: boolean }) => KakaoMap
  LatLng: new (latitude: number, longitude: number) => KakaoLatLng
  LatLngBounds: new (southWest: KakaoLatLng, northEast: KakaoLatLng) => KakaoBounds
  CustomOverlay: new (options: { map: KakaoMap; position: KakaoLatLng; content: HTMLElement; clickable: boolean; yAnchor: number }) => KakaoOverlay
  event: {
    addListener(target: KakaoMap, name: string, handler: () => void): void
    removeListener(target: KakaoMap, name: string, handler: () => void): void
  }
}

import type { SearchArea } from '../searchArea'
import type { MapCoordinates, MapStation, StationMapCallbacks, StationMapSession } from './stationMap'

let pendingSdk: Promise<KakaoMaps> | null = null

export function loadKakaoMaps(key: string): Promise<KakaoMaps> {
  if (pendingSdk) return pendingSdk
  pendingSdk = new Promise<KakaoMaps>((resolve, reject) => {
    const script = document.createElement('script')
    let timer: ReturnType<typeof setTimeout> | null = null
    const fail = (message: string) => {
      if (timer !== null) clearTimeout(timer)
      script.remove()
      pendingSdk = null
      reject(new Error(message))
    }
    script.dataset.plugpassKakaoMap = ''
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(key)}&autoload=false&libraries=services`
    script.async = true
    script.onerror = () => fail('카카오 지도 SDK를 불러오지 못했습니다.')
    script.onload = () => {
      const maps = (window as KakaoWindow).kakao?.maps
      if (!maps) {
        fail('카카오 지도 SDK가 준비되지 않았습니다.')
        return
      }
      try {
        maps.load(() => {
          if (timer !== null) clearTimeout(timer)
          resolve(maps)
        })
      } catch {
        fail('카카오 지도 SDK가 준비되지 않았습니다.')
      }
    }
    timer = setTimeout(() => fail('카카오 지도 SDK 응답 시간이 초과됐습니다.'), 9_000)
    document.head.append(script)
  })
  return pendingSdk
}

export function createKakaoStationMap(
  container: HTMLElement,
  stations: readonly MapStation[],
  center: MapCoordinates,
  callbacks: StationMapCallbacks,
): StationMapSession {
  const maps = (window as KakaoWindow).kakao?.maps
  if (!maps) throw new Error('카카오 지도 SDK가 준비되지 않았습니다.')
  const stationMap = new maps.Map(container, { center: new maps.LatLng(center.latitude, center.longitude), level: 5, scrollwheel: true, keyboardShortcuts: true })
  let overlays = new Map<number, { overlay: KakaoOverlay; button: HTMLButtonElement; coordinates: MapCoordinates }>()

  const toArea = (): SearchArea => {
    const bounds = stationMap.getBounds()
    return { south: bounds.getSouthWest().getLat(), north: bounds.getNorthEast().getLat(),
      west: bounds.getSouthWest().getLng(), east: bounds.getNorthEast().getLng() }
  }
  const reportArea = () => callbacks.areaChanged?.(toArea())
  const reportZoom = () => callbacks.zoomChanged?.(stationMap.getLevel())
  const clearArea = () => callbacks.areaChanged?.(null)
  const reportTiles = () => callbacks.tilesLoaded()
  maps.event.addListener(stationMap, 'dragstart', clearArea)
  maps.event.addListener(stationMap, 'zoom_start', clearArea)
  maps.event.addListener(stationMap, 'idle', reportArea)
  maps.event.addListener(stationMap, 'zoom_changed', reportZoom)
  maps.event.addListener(stationMap, 'tilesloaded', reportTiles)
  reportArea()

  function replaceStations(nextStations: readonly MapStation[]): void {
    for (const entry of overlays.values()) entry.overlay.setMap(null)
    overlays = new Map()
    for (const station of nextStations) {
      const button = document.createElement('button')
      button.type = 'button'
      button.className = 'station-map-marker kakao-station-marker'
      button.textContent = '●'
      button.setAttribute('aria-label', `${station.name} 지도 마커`)
      button.setAttribute('aria-pressed', 'false')
      button.addEventListener('click', () => callbacks.selectStation(station.id))
      const overlay = new maps!.CustomOverlay({ map: stationMap, position: new maps!.LatLng(station.latitude, station.longitude),
        content: button, clickable: true, yAnchor: 1 })
      overlays.set(station.id, { overlay, button, coordinates: station })
    }
  }
  replaceStations(stations)

  return {
    zoomIn() {
      const level = Math.max(1, stationMap.getLevel() - 1)
      stationMap.setLevel(level)
      return level
    },
    zoomOut() {
      const level = Math.min(14, stationMap.getLevel() + 1)
      stationMap.setLevel(level)
      return level
    },
    getZoomLevel() { return stationMap.getLevel() },
    selectStation(stationId) {
      for (const [id, entry] of overlays) {
        const selected = id === stationId
        entry.button.setAttribute('aria-pressed', String(selected))
        entry.button.classList.toggle('station-map-marker-selected', selected)
      }
      const selected = stationId === null ? undefined : overlays.get(stationId)
      if (selected) stationMap.setCenter(new maps.LatLng(selected.coordinates.latitude, selected.coordinates.longitude))
    },
    updateStations: replaceStations,
    moveTo(nextCenter) { stationMap.setCenter(new maps.LatLng(nextCenter.latitude, nextCenter.longitude)) },
    showArea(area) {
      stationMap.setBounds(new maps.LatLngBounds(new maps.LatLng(area.south, area.west), new maps.LatLng(area.north, area.east)))
    },
    destroy() {
      for (const entry of overlays.values()) entry.overlay.setMap(null)
      overlays.clear()
      maps.event.removeListener(stationMap, 'dragstart', clearArea)
      maps.event.removeListener(stationMap, 'zoom_start', clearArea)
      maps.event.removeListener(stationMap, 'idle', reportArea)
      maps.event.removeListener(stationMap, 'zoom_changed', reportZoom)
      maps.event.removeListener(stationMap, 'tilesloaded', reportTiles)
      container.replaceChildren()
    },
  }
}
