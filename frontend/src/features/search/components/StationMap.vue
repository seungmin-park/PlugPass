<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadStationMap } from '../map/loadStationMap'
import type { SearchArea } from '../searchArea'
import type { MapCoordinates, MapStation, MapZoomState, StationMapSession } from '../map/stationMap'
import { useErrorFocus } from '../../../shared/ui/useErrorFocus'
import LoadingIndicator from '../../../shared/ui/LoadingIndicator.vue'

const props = defineProps<{ stations: readonly MapStation[]; center: MapCoordinates; area?: SearchArea | undefined; compact?: boolean; selectedStationId: number | null }>()
const emit = defineEmits<{ select: [stationId: number]; 'area-change': [area: SearchArea | null]; interact: [] }>()
const container = ref<HTMLElement | null>(null)
const canvasKey = ref(0)
const status = ref<'loading' | 'ready' | 'error'>('loading')
const backgroundLoading = ref(true)
const errorMessage = ref<string | null>(null)
const errorNotice = ref<HTMLElement | null>(null)
const reloadRequired = ref(false)
const zoomState = ref<MapZoomState>({ canZoomIn: false, canZoomOut: false, description: '' })
useErrorFocus(() => errorMessage.value !== null, errorNotice)
let session: StationMapSession | null = null
let generation = 0
let loadTimer: ReturnType<typeof setTimeout> | null = null
let commands: Promise<void> = Promise.resolve()
let cleanup: Promise<void> = Promise.resolve()

function clearLoadTimer(): void {
  if (loadTimer !== null) clearTimeout(loadTimer)
  loadTimer = null
}
function reloadPage(): void { window.location.reload() }
function zoomIn(): void { void runCommand(map => map.zoomIn()) }
function zoomOut(): void { void runCommand(map => map.zoomOut()) }

function retireSession(): void {
  const previous = session
  session = null
  if (!previous) return
  commands = Promise.resolve()
  cleanup = cleanup.then(() => previous.destroy())
  // Keep cleanup failure observable to the next attempt, without an unhandled rejection.
  void cleanup.catch(() => { reloadRequired.value = true })
}
function reportFailure(currentGeneration: number, reload: boolean): void {
  if (currentGeneration !== generation) return
  generation += 1
  clearLoadTimer()
  backgroundLoading.value = false
  status.value = 'error'
  reloadRequired.value = reload
  errorMessage.value = '지도를 불러오지 못했습니다. 충전소 목록은 계속 이용할 수 있습니다.'
  retireSession()
}
function reportBackgroundFailure(): void {
  clearLoadTimer()
  backgroundLoading.value = false
  errorMessage.value = '배경 지도를 불러오지 못했습니다. 충전소 목록은 계속 이용할 수 있습니다.'
}
function runCommand(command: (map: StationMapSession) => Promise<void>): Promise<void> {
  const currentSession = session
  const currentGeneration = generation
  if (!currentSession) return Promise.resolve()
  commands = commands.then(async () => {
    if (currentGeneration !== generation || currentSession !== session) return
    await command(currentSession)
  }).catch(() => reportFailure(currentGeneration, false))
  return commands
}

async function openMap(): Promise<void> {
  const currentGeneration = ++generation
  clearLoadTimer()
  retireSession()
  status.value = 'loading'
  backgroundLoading.value = true
  errorMessage.value = null
  reloadRequired.value = false
  zoomState.value = { canZoomIn: false, canZoomOut: false, description: '' }
  let creating = false
  loadTimer = setTimeout(() => {
    if (currentGeneration !== generation) return
    if (status.value === 'ready') { reportBackgroundFailure(); return }
    reportFailure(currentGeneration, !creating)
  }, 10_000)
  try {
    await cleanup
    if (currentGeneration !== generation) return
    const createMap = await loadStationMap()
    if (currentGeneration !== generation) return
    canvasKey.value += 1
    await nextTick()
    if (currentGeneration !== generation || !container.value) return
    creating = true
    const initialStations = props.stations
    const initialCenter = { ...props.center }
    const created = await createMap(container.value, initialStations, initialCenter, {
      interacted: () => { if (currentGeneration === generation) emit('interact') },
      areaChanged: area => { if (currentGeneration === generation) emit('area-change', area) },
      zoomChanged: state => { if (currentGeneration === generation) zoomState.value = state },
      selectStation: stationId => { if (currentGeneration === generation) emit('select', stationId) },
      ready: () => {
        if (currentGeneration !== generation) return
        backgroundLoading.value = false
        if (status.value === 'ready') clearLoadTimer()
      },
      failed: () => reportFailure(currentGeneration, false),
    })
    if (currentGeneration !== generation) { await created.destroy(); return }
    session = created
    if (initialStations !== props.stations) await runCommand(map => map.updateStations(props.stations))
    if (currentGeneration !== generation) return
    const initialArea = props.area
    if (initialArea) await runCommand(map => map.showArea(initialArea))
    else if (initialCenter.latitude !== props.center.latitude || initialCenter.longitude !== props.center.longitude)
      await runCommand(map => map.moveTo(props.center))
    if (currentGeneration !== generation) return
    await runCommand(map => map.selectStation(props.selectedStationId))
    if (currentGeneration !== generation) return
    status.value = 'ready'
    if (!backgroundLoading.value) clearLoadTimer()
  } catch {
    reportFailure(currentGeneration, !creating)
  }
}
onMounted(() => { void openMap() })
watch(() => props.selectedStationId, stationId => { void runCommand(map => map.selectStation(stationId)) })
watch(() => props.stations, stations => {
  void runCommand(map => map.updateStations(stations))
  void runCommand(map => map.selectStation(props.selectedStationId))
})
watch(() => props.area, area => { if (area) void runCommand(map => map.showArea(area)) })
watch(() => [props.center.latitude, props.center.longitude], () => {
  if (!props.area) void runCommand(map => map.moveTo(props.center))
})
onBeforeUnmount(() => {
  generation += 1
  clearLoadTimer()
  retireSession()
})
</script>

<template>
  <section id="station-map-panel" class="station-map-panel" :class="{ 'map-failed': !!errorMessage }" aria-label="검색 결과 지도" :aria-busy="status === 'loading' || backgroundLoading">
    <p v-if="!compact && stations.length">지도에서 충전소 마커를 누르면 이용 정보와 길찾기를 볼 수 있습니다.</p>
    <LoadingIndicator v-if="status === 'loading' || backgroundLoading" message="지도를 불러오고 있습니다." />
    <p v-if="errorMessage" ref="errorNotice" role="alert" tabindex="-1">{{ errorMessage }}</p>
    <button v-if="errorMessage && reloadRequired" id="reload-map" type="button" class="secondary-button" @click="reloadPage">화면 새로고침</button>
    <button v-else-if="errorMessage" id="retry-map" type="button" class="secondary-button" @click="openMap">지도 다시 시도</button>
    <div v-show="status !== 'error'" class="station-map-frame">
      <div :key="canvasKey" ref="container" class="station-map-canvas" tabindex="0" aria-label="충전소 위치 지도"></div>
      <div v-if="status === 'ready'" class="map-zoom-controls" aria-label="지도 확대와 축소">
        <button type="button" aria-label="지도 확대" :disabled="!zoomState.canZoomIn" @click="zoomIn">+</button>
        <button type="button" aria-label="지도 축소" :disabled="!zoomState.canZoomOut" @click="zoomOut">−</button>
      </div>
    </div>
    <span v-if="status === 'ready'" class="visually-hidden" role="status">{{ zoomState.description }}</span>
  </section>
</template>
