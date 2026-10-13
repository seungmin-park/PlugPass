import type { Page } from '@playwright/test'

const sdk = (failFirstTiles: boolean) => `
(() => {
  let mapCount = 0
  class LatLng {
    constructor(latitude, longitude) { this.latitude = latitude; this.longitude = longitude }
    getLat() { return this.latitude }
    getLng() { return this.longitude }
  }
  class LatLngBounds {
    constructor(southWest, northEast) { this.southWest = southWest; this.northEast = northEast }
    getSouthWest() { return this.southWest }
    getNorthEast() { return this.northEast }
  }
  class KakaoMap {
    constructor(container) {
      mapCount += 1
      this.container = container
      this.listeners = new globalThis.Map()
      this.level = 5
      this.bounds = new LatLngBounds(new LatLng(37.495, 126.995), new LatLng(37.505, 127.005))
      const surface = document.createElement('div')
      surface.className = 'kakao-test-map'
      container.append(surface)
      const zoomIn = document.createElement('button')
      zoomIn.className = 'kakao-test-zoom-in'
      zoomIn.addEventListener('click', () => this.zoom(0.5))
      const zoomOut = document.createElement('button')
      zoomOut.className = 'kakao-test-zoom-out'
      zoomOut.addEventListener('click', () => this.zoom(2))
      container.append(zoomIn, zoomOut)
      container.addEventListener('keydown', event => {
        if (!event.key.startsWith('Arrow')) return
        this.emit('dragstart')
        setTimeout(() => this.emit('idle'), 0)
      })
      if (!(${failFirstTiles} && mapCount === 1)) setTimeout(() => this.emit('tilesloaded'), 0)
    }
    emit(name) { for (const handler of this.listeners.get(name) ?? []) handler() }
    getBounds() { return this.bounds }
    setBounds(bounds) { this.bounds = bounds; this.emit('idle') }
    setCenter() { this.emit('idle') }
    getLevel() { return this.level }
    setLevel(level) { this.level = level; this.emit('zoom_start'); this.emit('zoom_changed'); this.emit('idle') }
    zoom(scale) {
      this.emit('zoom_start')
      const southWest = this.bounds.getSouthWest()
      const northEast = this.bounds.getNorthEast()
      const latitude = (southWest.getLat() + northEast.getLat()) / 2
      const longitude = (southWest.getLng() + northEast.getLng()) / 2
      const halfLatitude = (northEast.getLat() - southWest.getLat()) * scale / 2
      const halfLongitude = (northEast.getLng() - southWest.getLng()) * scale / 2
      this.bounds = new LatLngBounds(new LatLng(latitude - halfLatitude, longitude - halfLongitude),
        new LatLng(latitude + halfLatitude, longitude + halfLongitude))
      setTimeout(() => this.emit('idle'), 0)
    }
  }
  class CustomOverlay {
    constructor(options) { this.content = options.content; options.map.container.append(this.content) }
    setMap(target) { if (target === null) this.content.remove() }
  }
  class Geocoder {
    addressSearch(query, callback) {
      const results = query.includes('검증') ? [{ address_name: '검증용 주소 후보', x: '127', y: '37.5' }]
        : query.includes('서울') ? [{ address_name: '서울특별시 중구 세종대로 110',
          road_address: { address_name: '서울특별시 중구 세종대로 110' }, x: '126.9784', y: '37.5663' }] : []
      callback(results, results.length ? 'OK' : 'ZERO_RESULT')
    }
  }
  class Places {
    keywordSearch(_query, callback) { callback([], 'ZERO_RESULT') }
  }
  window.kakao = { maps: {
    load: ready => ready(), Map: KakaoMap, LatLng, LatLngBounds, CustomOverlay,
    services: { Status: { OK: 'OK', ZERO_RESULT: 'ZERO_RESULT', ERROR: 'ERROR' }, Geocoder, Places },
    event: {
      addListener(map, name, handler) {
        const handlers = map.listeners.get(name) ?? []
        handlers.push(handler)
        map.listeners.set(name, handlers)
      },
      removeListener(map, name, handler) {
        map.listeners.set(name, (map.listeners.get(name) ?? []).filter(candidate => candidate !== handler))
      },
    },
  } }
})()
`

export async function mockKakaoSdk(page: Page, options: { failFirstTiles?: boolean } = {}): Promise<void> {
  await page.route('https://dapi.kakao.com/v2/maps/sdk.js?**', route => route.fulfill({ contentType: 'application/javascript', body: sdk(Boolean(options.failFirstTiles)) }))
}
