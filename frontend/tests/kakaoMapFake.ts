import { vi } from 'vitest'

type Listener = () => void
let tilesEnabled = true

class FakeLatLng {
  constructor(private readonly latitude: number, private readonly longitude: number) {}
  getLat(): number { return this.latitude }
  getLng(): number { return this.longitude }
}

class FakeBounds {
  constructor(private readonly southWest: FakeLatLng, private readonly northEast: FakeLatLng) {}
  getSouthWest(): FakeLatLng { return this.southWest }
  getNorthEast(): FakeLatLng { return this.northEast }
}

class FakeMap {
  readonly listeners = new Map<string, Listener[]>()
  private bounds = new FakeBounds(new FakeLatLng(37.495, 126.995), new FakeLatLng(37.505, 127.005))

  constructor(readonly container: HTMLElement) {
    const surface = document.createElement('div')
    surface.className = 'kakao-test-map'
    const zoomIn = document.createElement('button')
    zoomIn.className = 'kakao-test-zoom-in'
    zoomIn.addEventListener('click', () => this.zoom(0.5))
    const zoomOut = document.createElement('button')
    zoomOut.className = 'kakao-test-zoom-out'
    zoomOut.addEventListener('click', () => this.zoom(2))
    container.append(surface, zoomIn, zoomOut)
    container.addEventListener('keydown', event => {
      if (!event.key.startsWith('Arrow')) return
      this.emit('dragstart')
      setTimeout(() => this.emit('idle'), 0)
    })
    if (tilesEnabled) setTimeout(() => this.emit('tilesloaded'), 0)
  }

  emit(name: string): void { for (const listener of this.listeners.get(name) ?? []) listener() }
  getBounds(): FakeBounds { return this.bounds }
  setBounds(bounds: FakeBounds): void { this.bounds = bounds; this.emit('idle') }
  setCenter(): void { this.emit('idle') }

  private zoom(scale: number): void {
    this.emit('zoom_start')
    const southWest = this.bounds.getSouthWest()
    const northEast = this.bounds.getNorthEast()
    const latitude = (southWest.getLat() + northEast.getLat()) / 2
    const longitude = (southWest.getLng() + northEast.getLng()) / 2
    const halfLatitude = (northEast.getLat() - southWest.getLat()) * scale / 2
    const halfLongitude = (northEast.getLng() - southWest.getLng()) * scale / 2
    this.bounds = new FakeBounds(new FakeLatLng(latitude - halfLatitude, longitude - halfLongitude),
      new FakeLatLng(latitude + halfLatitude, longitude + halfLongitude))
    setTimeout(() => this.emit('idle'), 0)
  }
}

class FakeOverlay {
  constructor(private readonly options: { content: HTMLElement; map: FakeMap }) { options.map.container.append(options.content) }
  setMap(map: FakeMap | null): void { if (map === null) this.options.content.remove() }
}

export function installKakaoMapFake(): void {
  tilesEnabled = true
  vi.stubEnv('VITE_KAKAO_JS_KEY', 'test-js-key')
  Object.defineProperty(window, 'kakao', { configurable: true, value: { maps: {
    load: (ready: () => void) => ready(), Map: FakeMap, LatLng: FakeLatLng, LatLngBounds: FakeBounds, CustomOverlay: FakeOverlay,
    event: {
      addListener(map: FakeMap, name: string, listener: Listener) { map.listeners.set(name, [...(map.listeners.get(name) ?? []), listener]) },
      removeListener(map: FakeMap, name: string, listener: Listener) { map.listeners.set(name, (map.listeners.get(name) ?? []).filter(candidate => candidate !== listener)) },
    },
  } } })
  const append = document.head.append.bind(document.head)
  vi.spyOn(document.head, 'append').mockImplementation((...nodes: (Node | string)[]) => {
    append(...nodes)
    for (const node of nodes) {
      if (node instanceof HTMLScriptElement && node.dataset.plugpassKakaoMap !== undefined)
        queueMicrotask(() => node.dispatchEvent(new Event('load')))
    }
  })
}

export function setKakaoMapTilesEnabled(enabled: boolean): void { tilesEnabled = enabled }
