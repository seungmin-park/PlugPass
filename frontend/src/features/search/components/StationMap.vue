<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadStationMap } from '../map/loadStationMap'
import type { MapCoordinates, MapStation, StationMapSession } from '../map/stationMap'
import { useErrorFocus } from '../../../shared/ui/useErrorFocus'

const props = defineProps<{ stations: readonly MapStation[]; center: MapCoordinates; selectedStationId: number | null }>()
const emit = defineEmits<{ select: [stationId: number] }>()
const container = ref<HTMLElement | null>(null)
const status = ref<'loading' | 'ready' | 'error'>('loading')
const backgroundLoading = ref(true)
const errorMessage = ref<string | null>(null)
const errorNotice = ref<HTMLElement | null>(null)
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
      selectStation: stationId => { if (currentGeneration === generation) emit('select', stationId) },
      tilesLoaded: () => {
        if (currentGeneration !== generation) return
        clearLoadTimer()
        backgroundLoading.value = false
      },
      tilesFailed: () => { if (currentGeneration === generation) reportTileFailure() },
    })
    session.selectStation(props.selectedStationId)
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
watch([() => props.stations, () => props.center], () => { void openMap() })
onBeforeUnmount(() => {
  generation += 1
  clearLoadTimer()
  session?.destroy()
  session = null
})
</script>

<template>
  <section id="station-map-panel" class="station-map-panel" aria-label="검색 결과 지도" :aria-busy="status === 'loading' || backgroundLoading">
    <p>마커 또는 카드의 ‘지도에서 선택’을 누르면 같은 충전소가 선택됩니다.</p>
    <p v-if="status === 'loading' || backgroundLoading" role="status">지도를 불러오고 있습니다.</p>
    <p v-if="errorMessage" ref="errorNotice" role="alert" tabindex="-1">{{ errorMessage }}</p>
    <button v-if="status === 'error'" id="reload-map" type="button" class="secondary-button" @click="reloadPage">화면 새로고침</button>
    <button v-else-if="errorMessage" id="retry-map" type="button" class="secondary-button" @click="openMap">지도 다시 시도</button>
    <div v-show="status !== 'error'" ref="container" class="station-map-canvas" aria-label="충전소 위치 지도"></div>
  </section>
</template>
