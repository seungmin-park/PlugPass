<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadStationMap } from '../map/loadStationMap'
import type { SearchArea } from '../searchArea'
import type { MapCoordinates, MapStation, StationMapSession } from '../map/stationMap'
import { useErrorFocus } from '../../../shared/ui/useErrorFocus'
import LoadingIndicator from '../../../shared/ui/LoadingIndicator.vue'

const props = defineProps<{ stations: readonly MapStation[]; center: MapCoordinates; area?: SearchArea | undefined; compact?: boolean; selectedStationId: number | null }>()
const emit = defineEmits<{ select: [stationId: number]; 'area-change': [area: SearchArea | null] }>()
const container = ref<HTMLElement | null>(null)
const status = ref<'loading' | 'ready' | 'error'>('loading')
const backgroundLoading = ref(true)
const errorMessage = ref<string | null>(null)
const errorNotice = ref<HTMLElement | null>(null)
const zoomLevel = ref(5)
useErrorFocus(() => errorMessage.value !== null, errorNotice)
let session: StationMapSession | null = null
let generation = 0
let loadTimer: ReturnType<typeof setTimeout> | null = null

function clearLoadTimer(): void {
  if (loadTimer !== null) clearTimeout(loadTimer)
  loadTimer = null
}
function reloadPage(): void {
  window.location.reload()
}
function zoomIn(): void {
  if (session) zoomLevel.value = session.zoomIn()
}
function zoomOut(): void {
  if (session) zoomLevel.value = session.zoomOut()
}
function reportTileFailure(): void {
  clearLoadTimer()
  backgroundLoading.value = false
  errorMessage.value = '배경 지도를 불러오지 못했습니다. 충전소 목록은 계속 이용할 수 있습니다.'
}

async function openMap(): Promise<void> {
  const currentGeneration = ++generation
  clearLoadTimer()
  session?.destroy()
  session = null
  status.value = 'loading'
  backgroundLoading.value = true
  errorMessage.value = null
  loadTimer = setTimeout(() => {
    if (currentGeneration !== generation) return
    if (session) { reportTileFailure(); return }
    generation += 1
    loadTimer = null
    backgroundLoading.value = false
    status.value = 'error'
    errorMessage.value = '지도를 불러오지 못했습니다. 화면을 새로고침하거나 충전소 목록을 이용해 주세요.'
  }, 10_000)
  try {
    const createMap = await loadStationMap()
    if (currentGeneration !== generation || !container.value) return
    session = createMap(container.value, props.stations, props.center, {
      areaChanged: area => { if (currentGeneration === generation) emit('area-change', area) },
      zoomChanged: level => { if (currentGeneration === generation) zoomLevel.value = level },
      selectStation: stationId => { if (currentGeneration === generation) emit('select', stationId) },
      tilesLoaded: () => {
        if (currentGeneration !== generation) return
        clearLoadTimer()
        backgroundLoading.value = false
      },
      tilesFailed: () => { if (currentGeneration === generation) reportTileFailure() },
    })
    if (props.area) session.showArea(props.area)
    session.selectStation(props.selectedStationId)
    zoomLevel.value = session.getZoomLevel()
    status.value = 'ready'
  } catch {
    if (currentGeneration !== generation) return
    clearLoadTimer()
    backgroundLoading.value = false
    status.value = 'error'
    errorMessage.value = '지도를 불러오지 못했습니다. 화면을 새로고침하거나 충전소 목록을 이용해 주세요.'
  }
}
onMounted(() => { void openMap() })
watch(() => props.selectedStationId, stationId => session?.selectStation(stationId))
watch(() => props.stations, stations => {
  session?.updateStations(stations)
  session?.selectStation(props.selectedStationId)
})
watch(() => props.area, area => { if (area) session?.showArea(area) })
watch(() => [props.center.latitude, props.center.longitude], () => {
  if (props.area) return
  session?.moveTo(props.center)
})
onBeforeUnmount(() => {
  generation += 1
  clearLoadTimer()
  session?.destroy()
  session = null
})
</script>

<template>
  <section id="station-map-panel" class="station-map-panel" :class="{ 'map-failed': !!errorMessage }" aria-label="검색 결과 지도" :aria-busy="status === 'loading' || backgroundLoading">
    <p v-if="!compact && stations.length">지도에서 충전소 마커를 누르면 이용 정보와 길찾기를 볼 수 있습니다.</p>
    <LoadingIndicator v-if="status === 'loading' || backgroundLoading" message="지도를 불러오고 있습니다." />
    <p v-if="errorMessage" ref="errorNotice" role="alert" tabindex="-1">{{ errorMessage }}</p>
    <button v-if="status === 'error'" id="reload-map" type="button" class="secondary-button" @click="reloadPage">화면 새로고침</button>
    <button v-else-if="errorMessage" id="retry-map" type="button" class="secondary-button" @click="openMap">지도 다시 시도</button>
    <div v-show="status !== 'error'" class="station-map-frame">
      <div ref="container" class="station-map-canvas" tabindex="0" aria-label="충전소 위치 지도"></div>
      <div v-if="status === 'ready'" class="map-zoom-controls" aria-label="지도 확대와 축소">
        <button type="button" aria-label="지도 확대" :disabled="zoomLevel <= 1" @click="zoomIn">+</button>
        <button type="button" aria-label="지도 축소" :disabled="zoomLevel >= 14" @click="zoomOut">−</button>
      </div>
    </div>
    <span v-if="status === 'ready'" class="visually-hidden" role="status">지도 확대 수준 {{ zoomLevel }}단계</span>
  </section>
</template>
