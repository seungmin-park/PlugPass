<script setup lang="ts">
import ChargerStatusSummary from '../../charging-info/components/ChargerStatusSummary.vue'
import type { RecommendedStation } from '../types'
import { formatDistance, reasonLabel } from '../../charging-info/presentation'
const props = defineProps<{ title: string; candidates: RecommendedStation[]; searchQuery: Record<string, string> }>()
defineEmits<{ openDetail: [stationId: number] }>()
function detailHref(stationId: number): string {
  return `#/stations/${stationId}?${new URLSearchParams(props.searchQuery).toString()}`
}
</script>
<template>
  <section class="candidate-group" :aria-label="title">
    <h2>{{ title }}</h2>
    <p v-if="!candidates.length" role="status">해당 그룹의 후보가 없습니다.</p>
    <ul v-else class="candidate-list">
      <li v-for="candidate in candidates" :key="candidate.id" class="station-card">
        <div class="station-heading"><h3>{{ candidate.name }}</h3><p>직선거리 {{ formatDistance(candidate.distanceMeters) }}</p></div>
        <ChargerStatusSummary v-if="candidate.chargerStatuses" :statuses="candidate.chargerStatuses" :total-count="candidate.chargerStatuses.length" :available-count="candidate.chargerStatuses.filter(status => status === 'AVAILABLE').length" />
        <p v-else>충전기 상태는 상세에서 확인해 주세요.</p>
        <ul class="candidate-reasons"><li v-for="(reason, index) in candidate.reasonCodes" :key="index">{{ reasonLabel(reason) }}</li></ul>
        <a :href="detailHref(candidate.id)" :id="`candidate-${candidate.id}`"
          @click.exact.left.prevent="$emit('openDetail', candidate.id)">{{ candidate.name }} 상세 보기</a>
      </li>
    </ul>
  </section>
</template>
