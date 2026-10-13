import type { Page } from '@playwright/test'

const sdk = `
(() => {
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
      this.container = container
      this.listeners = new globalThis.Map()
      this.bounds = new LatLngBounds(new LatLng(37.495, 126.995), new LatLng(37.505, 127.005))
      const surface = document.createElement('div')
      surface.className = 'kakao-test-map'
      container.append(surface)
      setTimeout(() => this.emit('tilesloaded'), 0)
    }
    emit(name) { for (const handler of this.listeners.get(name) ?? []) handler() }
    getBounds() { return this.bounds }
    setBounds(bounds) { this.bounds = bounds; this.emit('idle') }
    setCenter() { this.emit('idle') }
  }
  class CustomOverlay {
    constructor(options) { this.content = options.content; options.map.container.append(this.content) }
    setMap(target) { if (target === null) this.content.remove() }
  }
  window.kakao = { maps: {
    load: ready => ready(), Map: KakaoMap, LatLng, LatLngBounds, CustomOverlay,
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

export async function mockKakaoSdk(page: Page): Promise<void> {
  await page.route('https://dapi.kakao.com/v2/maps/sdk.js?**', route => route.fulfill({ contentType: 'application/javascript', body: sdk }))
}
