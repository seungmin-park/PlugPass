<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useErrorFocus } from '../../../shared/ui/useErrorFocus'
import { useRoute } from 'vue-router'
import { parseStationId } from '../../../shared/api/stationId'
import { ApiError } from '../../../shared/api/apiError'
import { parseSearchCriteria, toSearchQuery } from '../../search/criteria'
import { formatTimestamp } from '../../charging-info/presentation'
import { useStationDetailStore } from '../stores/stationDetailStore'
import ChargerList from '../components/ChargerList.vue'

const route = useRoute()
const store = useStationDetailStore()
const stationId = computed(() => parseStationId(route.params.stationId))
const parsedCriteria = computed(() => parseSearchCriteria(route.query))
const searchQuery = computed(() => parsedCriteria.value.kind === 'valid' ? toSearchQuery(parsedCriteria.value.criteria) : {})
const inputError = computed(() => stationId.value === null
  ? new ApiError('validation', '충전소 ID를 확인해 주세요', null, 'INVALID_STATION_ID') : null)
const errorNotice = ref<HTMLElement | null>(null)
useErrorFocus(() => inputError.value !== null || store.error !== null, errorNotice)
watch(stationId, id => { if (id !== null) void store.loadStation(id); else store.reset() }, { immediate: true })
onBeforeUnmount(() => store.reset())
function refresh(): void { if (stationId.value !== null) void store.loadStation(stationId.value) }
</script>
<template>
  <section aria-labelledby="detail-heading" class="detail-view" :aria-busy="store.status === 'loading'">
    <div class="intro"><h1 id="detail-heading">충전소 상세</h1><p>충전기 상태와 정보의 근거를 함께 확인하세요.</p></div>
    <nav class="journey-links" aria-label="충전소 탐색">
      <RouterLink :to="{ path: '/stations', query: searchQuery }">검색 목록으로 돌아가기</RouterLink>
      <RouterLink v-if="parsedCriteria.kind === 'valid' && store.station" :to="{ path: '/alternatives', query: { ...searchQuery, excludeStationId: String(store.station.id) } }">다른 충전소 찾기</RouterLink>
      <RouterLink v-else to="/stations">검색 조건 선택</RouterLink>
    </nav>
    <p v-if="parsedCriteria.kind !== 'valid'" role="status">대체 후보를 찾으려면 검색 조건을 먼저 선택해 주세요.</p>
    <p v-if="store.status === 'loading'" role="status">충전소 상세를 조회하고 있습니다.</p>
    <div v-else-if="inputError || store.error" ref="errorNotice" tabindex="-1" role="alert" class="request-notice request-error">
      <p>{{ (inputError ?? store.error)?.message }}</p>
      <button v-if="!inputError" id="retry" type="button" class="secondary-button" @click="refresh">다시 조회</button>
    </div>
    <template v-if="store.station">
      <div class="results-heading"><h2>{{ store.station.name }}</h2><button class="secondary-button" type="button" @click="refresh">새로고침</button></div>
      <p>충전소 ID: {{ store.station.id }} · 공급자: {{ store.station.provider }} · 공급자 충전소 ID: {{ store.station.providerStationId }}</p>
      <p>위치: {{ store.station.latitude }} / {{ store.station.longitude }}</p>
      <p>조회 시각: {{ formatTimestamp(store.fetchedAt) }}</p>
      <p class="results-caution">이용 가능 보고는 도착 시점의 빈자리를 보장하지 않습니다. 운영 시간과 출입 조건을 확인하세요.</p>
      <ChargerList :chargers="store.station.chargers" />
    </template>
  </section>
</template>
