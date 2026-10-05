<script setup lang="ts">
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
        <h3>{{ candidate.name }}</h3><p>직선거리 {{ formatDistance(candidate.distanceMeters) }}</p>
        <ul class="candidate-reasons"><li v-for="(reason, index) in candidate.reasonCodes" :key="index">{{ reasonLabel(reason) }}</li></ul>
        <a :href="detailHref(candidate.id)" :id="`candidate-${candidate.id}`"
          @click.exact.left.prevent="$emit('openDetail', candidate.id)">{{ candidate.name }} 상세 보기</a>
      </li>
    </ul>
  </section>
</template>
