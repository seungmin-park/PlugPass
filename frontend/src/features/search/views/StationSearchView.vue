<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SearchForm from '../components/SearchForm.vue'
import PlaceSearch from '../components/PlaceSearch.vue'
import type { PlaceMatch } from '../api/placeApi'
import CollectionSummary from '../../charging-info/components/CollectionSummary.vue'
import StationCard from '../components/StationCard.vue'
import StationMap from '../components/StationMap.vue'
import StationActionSheet from '../../station-detail/components/StationActionSheet.vue'
import { createNavigationLinks, navigationPlatform } from '../../station-detail/navigation/navigationLinks'
import type { StationSummary } from '../types'
import { parseSearchCriteria, toSearchQuery } from '../criteria'
import { isSearchArea, searchCriteriaForArea, type SearchArea } from '../searchArea'
import type { SearchConditions, SearchCriteria } from '../types'
import { clearVehicleProfile, readVehicleProfile, saveVehicleProfile } from '../../vehicle/vehicleProfile'
import { resolveVehicleCompatibility } from '../../vehicle/vehicleCompatibility'
import { vehicleCatalog, adapterCatalog } from '../../vehicle/vehicleCatalog'
import { connectorLabel } from '../../charging-info/presentation'
import { formatTimestamp } from '../../charging-info/presentation'
import { useStationSearchStore } from '../stores/stationSearchStore'
import { useCurrentLocation, type LocationCoordinates } from '../../../shared/location/useCurrentLocation'
import { ApiError } from '../../../shared/api/apiError'
import RequestState from '../../../shared/ui/RequestState.vue'
import LoadingIndicator from '../../../shared/ui/LoadingIndicator.vue'
import { useErrorFocus } from '../../../shared/ui/useErrorFocus'

const route = useRoute()
const router = useRouter()
const store = useStationSearchStore()
const platform = navigationPlatform(navigator.userAgent, navigator.maxTouchPoints)
const conditionsOpen = ref(false)
const resultsHeading = ref<HTMLElement | null>(null)
const revealResultsOnSuccess = ref(false)
const pendingArea = ref<SearchArea | null>(null)
const mapSelectionActive = ref(false)
const restoredArea = ref<SearchArea | undefined>(undefined)
const savedProfile = ref((() => { try { return readVehicleProfile(window.localStorage) } catch { return null } })())
const autoLocationAttempted = ref(false)
const savedConditions = computed<SearchConditions | null>(() => {
  const profile = savedProfile.value
  if (!profile) return null
  const compatibility = resolveVehicleCompatibility(profile)
  const connector = compatibility.connectors[0]
  if (compatibility.requiresConfirmation || !connector) return null
  return { connector, connectors: compatibility.connectors, vehicleId: profile.vehicleId, adapterIds: profile.adapterIds,
    ...(profile.vehicleSupportConfirmed ? { vehicleSupportConfirmed: true } : {}) }
})
const selectedConditions = ref<SearchConditions | null>(savedConditions.value)
const conditionSummary = computed(() => {
  const conditions = selectedConditions.value
  if (!conditions) return '차량과 장비를 선택하세요'
  const vehicle = vehicleCatalog.find(item => item.id === conditions.vehicleId)?.label
  const equipment = adapterCatalog.filter(item => conditions.adapterIds?.includes(item.id)).map(item => item.label)
  return [vehicle ?? (conditions.connectors ?? [conditions.connector]).map(connectorLabel).join(' · '), ...equipment].join(' · ')
})
const selectedStationId = ref<number | null>(null)
const selectedStation = computed(() => store.response?.stations.find(station => station.id === selectedStationId.value))
watch(() => store.status, status => {
  if (status === 'loading' || status === 'idle') { selectedStationId.value = null }
  if (status === 'error') revealResultsOnSuccess.value = false
  if (status === 'success' && revealResultsOnSuccess.value) {
    revealResultsOnSuccess.value = false
    void nextTick(() => resultsHeading.value?.scrollIntoView?.({ block: 'start' }))
  }
}, { flush: 'sync' })
const { status: locationStatus, error: locationError, requestLocation, cancel: cancelLocation } = useCurrentLocation()
const selectedLocation = ref<LocationCoordinates | null>(null)
const locationLabel = ref('검색 위치')
const guidance = ref('검색할 위치와 조건을 선택해 주세요.')
const needsVehicleSelection = ref(false)
const prerequisiteNotice = ref<HTMLElement | null>(null)
const parsedCriteria = computed(() => parseSearchCriteria(route.query))
const confirmedCriteria = computed(() => parsedCriteria.value.kind === 'valid' ? parsedCriteria.value.criteria : null)
const vehicleResultMessage = computed(() => {
  const criteria = confirmedCriteria.value
  if (!criteria?.vehicleId) return null
  const compatibility = resolveVehicleCompatibility({ vehicleId: criteria.vehicleId, adapterIds: [...(criteria.adapterIds ?? [])],
    vehicleSupportConfirmed: criteria.vehicleSupportConfirmed ?? false })
  if (compatibility.direct.length && compatibility.viaAdapter.length) return '직접 연결 또는 이번에 선택한 어댑터를 사용할 조건으로 찾았습니다. 충전기별 연결 규격을 확인해 주세요.'
  if (compatibility.viaAdapter.length) return '이번에 선택한 어댑터를 사용할 조건으로 찾았습니다. 충전기별 이용 가능 여부를 확인해 주세요.'
  return '차량 자료에서 확인된 직접 연결 규격으로 찾았습니다.'
})
const mapCenter = computed(() => selectedLocation.value ?? confirmedCriteria.value ?? { latitude: 37.5665, longitude: 126.978 })
const validMapArea = computed(() => pendingArea.value !== null && isSearchArea(pendingArea.value))
const inputError = computed(() => parsedCriteria.value.kind === 'invalid'
  ? new ApiError('validation', '검색 조건을 확인해 주세요', null, 'INVALID_SEARCH_CRITERIA', parsedCriteria.value.fields) : null)
const fieldErrors = computed(() => (inputError.value ?? store.error)?.fields ?? {})
const locationNotice = ref<HTMLElement | null>(null)
useErrorFocus(() => locationError.value !== null, locationNotice)

watch(() => route.query, () => {
  cancelLocation()
  const criteria = confirmedCriteria.value
  const sameLocation = criteria?.latitude === selectedLocation.value?.latitude && criteria?.longitude === selectedLocation.value?.longitude
  selectedLocation.value = criteria ? { latitude: criteria.latitude, longitude: criteria.longitude } : null
  restoredArea.value = criteria?.area
  mapSelectionActive.value = !!criteria?.area
  selectedConditions.value = criteria ? { connector: criteria.connector,
    ...(criteria.connectors ? { connectors: criteria.connectors } : {}),
    ...(criteria.vehicleId ? { vehicleId: criteria.vehicleId } : {}),
    ...(criteria.adapterIds ? { adapterIds: criteria.adapterIds } : {}),
    ...(criteria.vehicleSupportConfirmed ? { vehicleSupportConfirmed: true } : {}) } : savedConditions.value
  if (!sameLocation) locationLabel.value = '검색 위치'
  conditionsOpen.value = !criteria
  needsVehicleSelection.value = false
  guidance.value = '검색할 위치와 조건을 선택해 주세요.'
  if (criteria) void store.search(criteria)
  else store.reset()
}, { immediate: true })
onBeforeUnmount(() => store.reset())
onMounted(() => {
  if (parsedCriteria.value.kind !== 'empty' || !savedConditions.value) return
  autoLocationAttempted.value = true
  void selectCurrentLocation()
})

async function selectCurrentLocation(): Promise<void> {
  mapSelectionActive.value = false
  selectedLocation.value = null
  const coordinates = await requestLocation()
  if (!coordinates || locationStatus.value !== 'success') return
  restoredArea.value = undefined
  selectedLocation.value = coordinates
  locationLabel.value = '현재 위치'
  needsVehicleSelection.value = false
  if (selectedConditions.value) {
    await confirmSearch({ ...coordinates, ...selectedConditions.value, radiusMeters: 1000, limit: 20 }, false)
    return
  }
  guidance.value = '현재 위치를 선택했습니다. 차량을 선택해 주세요.'
}
async function selectPlace(place: PlaceMatch): Promise<void> {
  mapSelectionActive.value = false
  cancelLocation()
  restoredArea.value = undefined
  selectedLocation.value = { latitude: place.latitude, longitude: place.longitude }
  locationLabel.value = place.name
  if (!selectedConditions.value) { promptForVehicle(); return }
  await confirmSearch({ latitude: place.latitude, longitude: place.longitude, ...selectedConditions.value,
    radiusMeters: confirmedCriteria.value?.radiusMeters ?? 1000, limit: confirmedCriteria.value?.limit ?? 20 })
}
function focusPlaceSearch(): void {
  cancelLocation()
  document.getElementById('place-query')?.focus()
}
function promptForVehicle(): void {
  needsVehicleSelection.value = true
  conditionsOpen.value = true
  guidance.value = '차량을 먼저 선택한 뒤 검색해 주세요.'
  void nextTick(() => prerequisiteNotice.value?.focus())
}
function focusVehicleChoice(): void {
  conditionsOpen.value = true
  void nextTick(() => document.getElementById('vehicle-choice')?.focus())
}
async function submitSearch(conditions: SearchConditions): Promise<void> {
  selectedConditions.value = conditions
  if (selectedLocation.value && !mapSelectionActive.value) {
    await confirmSearch({ ...selectedLocation.value, ...conditions, radiusMeters: confirmedCriteria.value?.radiusMeters ?? 1000, limit: confirmedCriteria.value?.limit ?? 20 })
    return
  }
  if (mapSelectionActive.value || pendingArea.value) {
    if (!pendingArea.value || !validMapArea.value) {
      guidance.value = '지도가 준비되고 이동이 끝난 후 검색해 주세요. 지도를 사용할 수 없다면 주소나 현재 위치로 검색해 주세요.'
      return
    }
    await confirmSearch({ ...searchCriteriaForArea(pendingArea.value, conditions.connector, confirmedCriteria.value?.limit ?? 20), ...conditions })
    return
  }
  await selectCurrentLocation()
}
function receiveConditions(conditions: SearchConditions | null): void {
  selectedConditions.value = conditions
  if (!conditions?.vehicleId || mapSelectionActive.value || selectedLocation.value || parsedCriteria.value.kind !== 'empty'
    || autoLocationAttempted.value || locationStatus.value !== 'idle') return
  autoLocationAttempted.value = true
  void selectCurrentLocation()
}
async function confirmSearch(criteria: SearchCriteria, revealResults = true): Promise<void> {
  conditionsOpen.value = false
  revealResultsOnSuccess.value = revealResults
  const current = confirmedCriteria.value
  if (current && JSON.stringify(toSearchQuery(current)) === JSON.stringify(toSearchQuery(criteria))) {
    void store.search(criteria)
  } else {
    await router.push({ path: '/stations', query: toSearchQuery(criteria) })
  }
  try {
    if (criteria.vehicleId) {
      const profile = { vehicleId: criteria.vehicleId, adapterIds: [...(criteria.adapterIds ?? [])],
        vehicleSupportConfirmed: criteria.vehicleSupportConfirmed ?? false }
      if (saveVehicleProfile(window.localStorage, profile)) savedProfile.value = profile
    } else if (clearVehicleProfile(window.localStorage)) savedProfile.value = null
  } catch { /* 저장소가 차단돼도 현재 검색은 계속한다. */ }
  await nextTick()
  if (revealResults && !store.error) resultsHeading.value?.focus()
}
function refreshSearch(): void {
  if (confirmedCriteria.value) void store.search(confirmedCriteria.value)
}
function receiveMapArea(area: SearchArea | null): void { pendingArea.value = area }
function selectMapArea(event?: KeyboardEvent): void {
  if (event && !['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', '+', '-'].includes(event.key)) return
  mapSelectionActive.value = true
  cancelLocation()
}
function searchMapArea(): void {
  selectMapArea()
  const conditions = selectedConditions.value
  if (!conditions) { promptForVehicle(); return }
  if (!pendingArea.value || !validMapArea.value) return
  void confirmSearch({ ...searchCriteriaForArea(pendingArea.value, conditions.connector, confirmedCriteria.value?.limit ?? 20), ...conditions })
}
function selectStation(stationId: number): void {
  if (store.response?.stations.some(station => station.id === stationId)) selectedStationId.value = stationId
}
function closeStationSheet(): void {
  selectedStationId.value = null
}
function detailHref(stationId: number): string {
  const criteria = confirmedCriteria.value
  if (!criteria) return `#/stations/${stationId}`
  const query = new URLSearchParams(toSearchQuery(criteria))
  return `#/stations/${stationId}?${query.toString()}`
}
function openDetail(stationId: number): void {
  void router.push({ path: `/stations/${stationId}`, query: route.query })
}
function directionsHref(station: StationSummary): string | undefined {
  return platform === 'web' ? undefined : createNavigationLinks(station, platform)?.directHref
}
function directionsInstallHref(station: StationSummary): string | undefined {
  return platform === 'ios' ? createNavigationLinks(station, platform)?.installHref : undefined
}
</script>

<template>
  <section aria-labelledby="search-heading" class="search-view" :class="{ 'has-results': confirmedCriteria }">
    <div class="intro">
      <h1 id="search-heading">주변 충전소 찾기</h1>
      <p v-if="!confirmedCriteria">차량에 맞는 충전소와 이용 조건을 확인하세요.</p>
    </div>
    <PlaceSearch @select="selectPlace" />
    <div v-if="locationError && autoLocationAttempted" ref="locationNotice" class="search-location-alert" tabindex="-1" role="alert">
      <strong>현재 위치를 확인하지 못했어요</strong>
      <p>{{ locationError }}</p>
      <div class="location-error-actions">
        <button type="button" class="secondary-button" id="retry-location" @click="selectCurrentLocation">현재 위치 다시 시도</button>
        <button type="button" class="secondary-button" id="location-address" @click="focusPlaceSearch">주소로 검색하기</button>
      </div>
    </div>
    <div v-if="needsVehicleSelection && !selectedConditions" ref="prerequisiteNotice" class="search-prerequisite-notice" role="alert" tabindex="-1">
      <div>
        <strong>{{ selectedLocation ? `${locationLabel} 위치를 선택했어요.` : '지도에서 검색할 지역을 골랐어요.' }}</strong>
        <p>충전소 결과를 보려면 내 차량을 선택해 주세요. 목록에 없다면 충전구를 직접 확인할 수 있어요.</p>
      </div>
      <button type="button" class="secondary-button" @click="focusVehicleChoice">내 차량 선택하기</button>
    </div>
    <div class="search-layout" :class="{ 'results-layout': confirmedCriteria }">
      <details id="search-conditions" class="search-panel" :open="conditionsOpen || !confirmedCriteria || !!fieldErrors.connector || !!fieldErrors.connectors || !!fieldErrors.vehicleId" @toggle="conditionsOpen = ($event.target as HTMLDetailsElement).open">
        <summary><span>{{ confirmedCriteria ? '검색 조건 변경' : '검색 위치와 조건' }}</span><span v-if="confirmedCriteria" class="criteria-summary">{{ conditionSummary }}</span></summary>
        <div class="panel-heading">
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h16M8 3v6m8 0v6M8 15v6" /></svg>
          <h2>검색 조건</h2>
        </div>
        <p class="panel-description">차량과 이번에 사용할 장비를 선택하세요. 지도로 지역을 고르거나 선택 위치 주변을 검색할 수 있습니다.</p>
        <div class="location-picker" aria-label="검색 위치 선택">
          <button type="button" class="secondary-button" id="current-location" @click="selectCurrentLocation">현재 위치 사용</button>
          <p v-if="selectedLocation" class="selected-location">{{ locationLabel }} 선택됨</p>
          <LoadingIndicator v-if="locationStatus === 'loading'" message="현재 위치를 확인하고 있습니다. 최대 10초 기다려 주세요." />
          <div v-if="locationError && !autoLocationAttempted" ref="locationNotice" class="location-error" tabindex="-1" role="alert">
            <p>{{ locationError }}</p>
            <div class="location-error-actions">
              <button type="button" class="secondary-button" id="retry-location" @click="selectCurrentLocation">현재 위치 다시 시도</button>
              <button type="button" class="secondary-button" id="location-address" @click="focusPlaceSearch">주소로 검색하기</button>
            </div>
          </div>
        </div>
        <SearchForm :key="route.fullPath"
          :initial-connector="confirmedCriteria?.connector ?? 'DC_COMBO'"
          :initial-mode="confirmedCriteria && !confirmedCriteria.vehicleId ? 'manual' : 'choose'"
          :initial-vehicle-id="confirmedCriteria?.vehicleId ?? (!confirmedCriteria ? savedProfile?.vehicleId : undefined)"
          :initial-adapter-ids="confirmedCriteria?.adapterIds ?? (!confirmedCriteria ? savedProfile?.adapterIds : undefined)"
          :initial-vehicle-support-confirmed="confirmedCriteria?.vehicleSupportConfirmed ?? (!confirmedCriteria ? savedProfile?.vehicleSupportConfirmed : undefined)"
          :field-errors="fieldErrors" @change="receiveConditions" @submit="submitSearch" />
        <div class="location-guidance">
          <svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="7" /><circle cx="12" cy="12" r="2.5" /><path d="M12 2v3m0 14v3M2 12h3m14 0h3" /></svg>
          <div><h3>위치 선택 안내</h3><p class="initial-guidance" role="status">{{ guidance }}</p></div>
        </div>
      </details>
    <div class="search-map-explorer" @pointerdown.capture="selectMapArea()" @wheel.capture.passive="selectMapArea()" @click.capture="selectMapArea()" @keydown.capture="selectMapArea">
        <p v-if="!confirmedCriteria && !selectedLocation">지도를 움직여 찾을 지역을 고른 뒤 ‘이 지역 재검색’을 눌러 주세요. 위치를 선택하지 않았다면 서울 시청 주변에서 시작합니다.</p>
        <StationMap :stations="store.response?.stations ?? []" :center="mapCenter" :area="restoredArea"
          :selected-station-id="selectedStationId" :compact="!!confirmedCriteria" @select="selectStation" @area-change="receiveMapArea" @interact="selectMapArea()" />
        <p v-if="pendingArea && !validMapArea" role="status">검색 영역이 너무 넓거나 지원 범위를 벗어났습니다. 지도를 확대해 주세요.</p>
        <button id="search-map-area" type="button" class="search-button" :disabled="!validMapArea || store.status === 'loading'" @click="searchMapArea">이 지역 재검색</button>
    </div>
      <aside v-if="parsedCriteria.kind === 'empty'" class="search-explainer" aria-labelledby="information-heading">
        <div class="explainer-heading">
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6zM8 12l3 3 5-6" /></svg>
          <h2 id="information-heading">헛걸음을 줄이는 정보 확인</h2>
        </div>
        <p class="explainer-intro">이용 가능 보고만으로 도착 시점의 빈자리를 보장할 수는 없습니다.</p>
        <dl class="information-guide">
          <div>
            <dt><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>관측 시각</dt>
            <dd>현장에서 상태를 확인한 시각입니다. 공급자가 현장 관측 시각을 제공하지 않으면 확인 불가로 표시합니다.</dd>
          </div>
          <div>
            <dt><svg aria-hidden="true" viewBox="0 0 24 24"><path d="M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5M4 16a8 8 0 0 0 14 3l3-3m0 5v-5h-5" /></svg>수집 시각</dt>
            <dd>서버가 정보를 받은 시각입니다. 최근에 수집됐어도 관측 정보는 오래됐을 수 있습니다.</dd>
          </div>
          <div>
            <dt><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="M12 10v7m0-10v.1" /></svg>이용 전 확인</dt>
            <dd>차량 커넥터와 함께 운영 시간·출입 조건을 확인하세요.</dd>
          </div>
        </dl>
      </aside>
      <div v-else class="search-results" aria-labelledby="results-heading" :aria-busy="store.status === 'loading'">
        <div class="results-heading">
          <h2 id="results-heading" ref="resultsHeading" tabindex="-1">주변 충전소</h2>
          <button v-if="confirmedCriteria" type="button" class="secondary-button" id="refresh" :disabled="store.status === 'loading'" @click="refreshSearch">새로고침</button>
        </div>
        <RequestState :status="inputError ? 'error' : store.status" :error="inputError ?? store.error" :retryable="!!confirmedCriteria"
          :focus-error="!fieldErrors.connector" @retry="refreshSearch" />
        <template v-if="store.response && confirmedCriteria">
          <p v-if="!store.response.dataReady" class="request-notice" role="status">
            {{ store.response.stations.length ? '전체 수집 완료 전 정보입니다. 일부 충전소만 표시될 수 있습니다.' : '아직 전체 수집이 완료되지 않았습니다. 수집 후 다시 조회해 주세요.' }}
          </p>
          <p v-else-if="!store.response.stations.length" class="request-notice" role="status">조건에 맞는 충전소가 없습니다. 지도 위치나 차량 조건을 변경해 주세요.</p>
          <CollectionSummary :collection="store.response.collection" />
          <p class="results-count">{{ store.response.stations.length }}개 표시 · 최대 {{ confirmedCriteria.limit }}개 표시</p>
          <p v-if="selectedStation" role="status">선택한 충전소: {{ selectedStation.name }}</p>
          <div class="station-list"><StationCard v-for="station in store.response.stations" :key="station.id" :station="station"
            :detail-href="detailHref(station.id)" :directions-href="directionsHref(station)" :directions-install-href="directionsInstallHref(station)" :selected="selectedStationId === station.id"
            @select="selectStation(station.id)" @directions="selectStation(station.id)" @open-detail="openDetail(station.id)" /></div>
          <p v-if="vehicleResultMessage" class="vehicle-result-note">{{ vehicleResultMessage }}</p>
          <p class="results-caution">{{ confirmedCriteria.area ? '지도 중심에서의 직선거리 기준입니다.' : '선택 위치에서의 직선거리 기준입니다.' }} 이용 가능 표시는 도착 시점의 빈자리를 보장하지 않습니다.</p>
          <details class="collection-details"><summary>정보 갱신 안내</summary>
          <p class="result-time">조회 시각: {{ formatTimestamp(store.fetchedAt) }}</p>
          <p class="result-time">전체 수집 성공 시각: {{ formatTimestamp(store.response.lastSuccessfulRunAt) }}</p>
          <div v-if="store.response.collection">
            <p v-if="store.response.collection.state === 'DELAYED'">수집 상태: 갱신 지연</p>
            <p v-else-if="store.response.collection.state === 'NOT_READY'">수집 상태: 준비 전</p>
            <p v-else>수집 정상 · 상태 변경분은 {{ store.response.collection.statusIntervalSeconds / 60 }}분마다 갱신합니다.</p>
            <p class="result-time">상태 변경분 수집 성공 시각: {{ formatTimestamp(store.response.collection.lastStatusSuccessAt) }}</p>
            <p class="result-time">수집 성공은 현장 관측 시각이나 도착 시 빈자리를 보장하지 않습니다.</p>
          </div>
          </details>
        </template>
      </div>
    </div>
    <StationActionSheet v-if="selectedStation" :station="selectedStation" :detail-href="detailHref(selectedStation.id)" @close="closeStationSheet" @open-detail="openDetail(selectedStation.id)" />
  </section>
</template>
