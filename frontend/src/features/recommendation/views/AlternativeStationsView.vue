<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { parseSearchCriteria, toSearchQuery } from '../../search/criteria'
import { parseStationId } from '../../../shared/api/stationId'
import { useRecommendationStore } from '../stores/recommendationStore'
import CandidateGroup from '../components/CandidateGroup.vue'
import { formatTimestamp } from '../../charging-info/presentation'

const route = useRoute()
const router = useRouter()
const store = useRecommendationStore()
const parsedCriteria = computed(() => parseSearchCriteria(route.query))
const excludeStationId = computed(() => parseStationId(route.query.excludeStationId))
const validInput = computed(() => parsedCriteria.value.kind === 'valid' && excludeStationId.value !== null)
const searchQuery = computed(() => parsedCriteria.value.kind === 'valid' ? toSearchQuery(parsedCriteria.value.criteria) : {})
watch(() => route.fullPath, () => {
  const parsed = parsedCriteria.value
  if (parsed.kind === 'valid' && excludeStationId.value !== null) void store.loadAlternatives(parsed.criteria, excludeStationId.value)
  else store.reset()
}, { immediate: true })
onBeforeUnmount(() => store.reset())
function openDetail(stationId: number): void { void router.push({ path: `/stations/${stationId}`, query: searchQuery.value }) }
</script>
<template>
  <section aria-labelledby="alternative-heading" class="alternative-view">
    <div class="intro"><h1 id="alternative-heading">대체 충전소 후보</h1><p>같은 조건으로 찾은 후보와 확인·제외 이유입니다.</p></div>
    <nav class="journey-links" aria-label="충전소 탐색"><RouterLink :to="{ path: '/stations', query: searchQuery }">검색 목록으로 돌아가기</RouterLink></nav>
    <p v-if="!validInput" class="request-notice request-error" role="alert">검색 조건과 제외 충전소 ID를 확인해 주세요.</p>
    <template v-else>
      <p>제외 충전소 ID: {{ excludeStationId }} · 최대 {{ parsedCriteria.kind === 'valid' ? parsedCriteria.criteria.limit : '' }}개씩 표시</p>
      <p class="results-caution">직선거리 기준입니다. 후보는 도착 시점의 빈자리를 보장하지 않습니다.</p>
      <div class="metadata-state" :aria-busy="store.metadataStatus === 'loading'">
        <p v-if="store.metadataStatus === 'loading'" role="status">수집 상태를 조회하고 있습니다.</p>
        <div v-if="store.metadataError" class="request-notice request-error" role="alert">
          <p>수집 상태 확인 실패: {{ store.metadataError.message }}</p>
          <button id="retry-metadata" type="button" class="secondary-button" @click="store.retryMetadata">수집 상태 다시 조회</button>
        </div>
        <template v-if="store.metadata">
          <p v-if="!store.metadata.dataReady" class="request-notice" role="status">전체 수집 완료 전 정보입니다. 일부 후보만 표시될 수 있습니다.</p>
          <p>수집 상태 조회 시각: {{ formatTimestamp(store.metadataFetchedAt) }}</p>
          <p>전체 수집 성공 시각: {{ formatTimestamp(store.metadata.lastSuccessfulRunAt) }} (후보 관측 시각이 아닙니다)</p>
        </template>
      </div>
      <div :aria-busy="store.candidateStatus === 'loading'">
        <p v-if="store.candidateStatus === 'loading'" role="status">대체 후보를 조회하고 있습니다.</p>
        <div v-if="store.candidateError" class="request-notice request-error" role="alert">
          <p>{{ store.candidateError.message }}</p><button id="retry-candidates" type="button" class="secondary-button" @click="store.retryCandidates">후보 다시 조회</button>
        </div>
        <template v-if="store.candidates">
          <p>후보 조회 시각: {{ formatTimestamp(store.candidatesFetchedAt) }}</p>
          <button class="secondary-button" type="button" @click="store.retryCandidates">후보 새로고침</button>
          <CandidateGroup title="우선 후보" :candidates="store.candidates.preferred" :search-query="searchQuery" @open-detail="openDetail" />
          <CandidateGroup title="이용 전 확인 필요" :candidates="store.candidates.requiresConfirmation" :search-query="searchQuery" @open-detail="openDetail" />
          <CandidateGroup title="추천에서 제외된 이유" :candidates="store.candidates.excluded" :search-query="searchQuery" @open-detail="openDetail" />
        </template>
      </div>
    </template>
  </section>
</template>
