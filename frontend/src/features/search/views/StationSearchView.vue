<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SearchForm from '../components/SearchForm.vue'
import StationCard from '../components/StationCard.vue'
import { parseSearchCriteria } from '../criteria'
import type { SearchCriteria } from '../types'
import type { Connector } from '../../charging-info/types'
import { formatTimestamp } from '../../charging-info/presentation'
import { useStationSearchStore } from '../stores/stationSearchStore'
import { useCurrentLocation, type LocationCoordinates } from '../../../shared/location/useCurrentLocation'
import { ApiError } from '../../../shared/api/apiError'
import RequestState from '../../../shared/ui/RequestState.vue'

const route = useRoute()
const router = useRouter()
const store = useStationSearchStore()
const { status: locationStatus, error: locationError, requestLocation, cancel: cancelLocation } = useCurrentLocation()
const selectedLocation = ref<LocationCoordinates | null>(null)
const locationLabel = ref('검색 위치')
const guidance = ref('검색할 위치와 조건을 선택해 주세요.')
const parsedCriteria = computed(() => parseSearchCriteria(route.query))
const confirmedCriteria = computed(() => parsedCriteria.value.kind === 'valid' ? parsedCriteria.value.criteria : null)
const inputError = computed(() => parsedCriteria.value.kind === 'invalid'
  ? new ApiError('validation', '검색 조건을 확인해 주세요', null, 'INVALID_SEARCH_CRITERIA', parsedCriteria.value.fields) : null)

watch(() => route.query, () => {
  cancelLocation()
  const criteria = confirmedCriteria.value
  selectedLocation.value = criteria ? { latitude: criteria.latitude, longitude: criteria.longitude } : null
  locationLabel.value = '검색 위치'
  guidance.value = '검색할 위치와 조건을 선택해 주세요.'
  if (criteria) void store.search(criteria)
  else store.reset()
}, { immediate: true })
onBeforeUnmount(() => store.reset())

async function selectCurrentLocation(): Promise<void> {
  selectedLocation.value = null
  const coordinates = await requestLocation()
  if (!coordinates || locationStatus.value !== 'success') return
  selectedLocation.value = coordinates
  locationLabel.value = '현재 위치'
  guidance.value = '현재 위치를 선택했습니다. 검색 버튼을 눌러 주세요.'
}
function selectExampleLocation(): void {
  cancelLocation()
  selectedLocation.value = { latitude: 37.5, longitude: 127 }
  locationLabel.value = '검증용 예시 위치 · 합성 데모 지역'
  guidance.value = '예시 위치를 선택했습니다. 실제 현재 위치가 아닙니다.'
}
async function submitSearch(conditions: { radiusMeters: number; connector: Connector }): Promise<void> {
  if (!selectedLocation.value) {
    guidance.value = '검색할 위치를 먼저 선택해 주세요.'
    return
  }
  const criteria: SearchCriteria = { ...selectedLocation.value, ...conditions, limit: confirmedCriteria.value?.limit ?? 20 }
  const current = confirmedCriteria.value
  const keys = ['latitude', 'longitude', 'radiusMeters', 'connector', 'limit'] as const
  if (current && keys.every(key => current[key] === criteria[key])) {
    await store.search(criteria)
    return
  }
  await router.push({ path: '/stations', query: {
    latitude: String(criteria.latitude), longitude: String(criteria.longitude), radiusMeters: String(criteria.radiusMeters),
    connector: criteria.connector, limit: String(criteria.limit),
  } })
}
function refreshSearch(): void {
  if (confirmedCriteria.value) void store.search(confirmedCriteria.value)
}
function detailHref(stationId: number): string {
  const criteria = confirmedCriteria.value
  if (!criteria) return `#/stations/${stationId}`
  const query = new URLSearchParams({ latitude: String(criteria.latitude), longitude: String(criteria.longitude),
    radiusMeters: String(criteria.radiusMeters), connector: criteria.connector, limit: String(criteria.limit) })
  return `#/stations/${stationId}?${query.toString()}`
}
function openDetail(stationId: number): void {
  void router.push({ path: `/stations/${stationId}`, query: route.query })
}
</script>

<template>
  <section aria-labelledby="search-heading" class="search-view">
    <div class="intro">
      <h1 id="search-heading">주변 충전소 찾기</h1>
      <p>차량에 맞는 충전소와 정보의 최신성을 함께 확인하세요.</p>
    </div>
    <div class="search-layout">
      <div class="search-panel">
        <div class="panel-heading">
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h16M8 3v6m8 0v6M8 15v6" /></svg>
          <h2>검색 조건</h2>
        </div>
        <p class="panel-description">내 차량에 맞는 커넥터와 검색 범위를 선택하세요.</p>
        <div class="location-picker" aria-label="검색 위치 선택">
          <button type="button" class="secondary-button" id="current-location" @click="selectCurrentLocation">현재 위치 사용</button>
          <button type="button" class="secondary-button" id="example-location" @click="selectExampleLocation">검증용 예시 위치 사용</button>
          <p>예시 위치 37.5 / 127은 합성 데모 지역입니다. 해당 지역의 데이터가 없으면 결과가 없을 수 있습니다.</p>
          <p v-if="selectedLocation" class="selected-location">{{ locationLabel }}: {{ selectedLocation.latitude }} / {{ selectedLocation.longitude }}</p>
          <p v-if="locationStatus === 'loading'" role="status">현재 위치를 확인하고 있습니다. 최대 10초 기다려 주세요.</p>
          <p v-if="locationError" role="alert">{{ locationError }}</p>
        </div>
        <SearchForm :key="route.fullPath" :initial-radius-meters="confirmedCriteria?.radiusMeters ?? 1000"
          :initial-connector="confirmedCriteria?.connector ?? 'DC_COMBO'" @submit="submitSearch" />
        <div class="location-guidance">
          <svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="7" /><circle cx="12" cy="12" r="2.5" /><path d="M12 2v3m0 14v3M2 12h3m14 0h3" /></svg>
          <div><h3>위치 선택 안내</h3><p class="initial-guidance" role="status">{{ guidance }}</p></div>
        </div>
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
            <dd>현장에서 상태를 확인한 시각입니다. 정보가 언제 관측됐는지 확인하세요.</dd>
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
          <h2 id="results-heading">주변 충전소</h2>
          <button v-if="confirmedCriteria" type="button" class="secondary-button" id="refresh" :disabled="store.status === 'loading'" @click="refreshSearch">새로고침</button>
        </div>
        <RequestState :status="inputError ? 'error' : store.status" :error="inputError ?? store.error" :retryable="!!confirmedCriteria" @retry="refreshSearch" />
        <template v-if="store.response && confirmedCriteria">
          <p v-if="!store.response.dataReady" class="request-notice" role="status">
            {{ store.response.stations.length ? '전체 수집 완료 전 정보입니다. 일부 충전소만 표시될 수 있습니다.' : '아직 전체 수집이 완료되지 않았습니다. 수집 후 다시 조회해 주세요.' }}
          </p>
          <p v-else-if="!store.response.stations.length" class="request-notice" role="status">조건에 맞는 충전소가 없습니다. 반경이나 커넥터를 변경해 주세요.</p>
          <p class="results-count">{{ store.response.stations.length }}개 표시 · 최대 {{ confirmedCriteria.limit }}개 표시</p>
          <p class="results-caution">직선거리 기준입니다. 이용 가능 보고는 도착 시점의 빈자리를 보장하지 않습니다.</p>
          <p class="result-time">조회 시각: {{ formatTimestamp(store.fetchedAt) }}</p>
          <p class="result-time">전체 수집 성공 시각: {{ formatTimestamp(store.response.lastSuccessfulRunAt) }}</p>
          <div class="station-list"><StationCard v-for="station in store.response.stations" :key="station.id" :station="station"
            :detail-href="detailHref(station.id)" @open-detail="openDetail(station.id)" /></div>
        </template>
      </div>
    </div>
  </section>
</template>
