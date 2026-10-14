<script setup lang="ts">
import { ref } from 'vue'
import type { StationSummary } from '../types'
import { formatDistance } from '../../charging-info/presentation'
import StatusBadge from '../../charging-info/components/StatusBadge.vue'
import ChargerStatusSummary from '../../charging-info/components/ChargerStatusSummary.vue'
import { scheduleIosInstallFallback } from '../../station-detail/navigation/navigationHandoff'
const props = defineProps<{ station: StationSummary; detailHref?: string; directionsHref?: string | undefined; directionsInstallHref?: string | undefined; selected?: boolean }>()
defineEmits<{ openDetail: []; directions: []; select: [] }>()
const expanded = ref(false)
function prepareDirections(event: MouseEvent): void {
  if (event.defaultPrevented || !props.directionsInstallHref) return
  scheduleIosInstallFallback(document, props.directionsInstallHref, href => window.location.assign(href))
}
</script>
<template>
  <article class="station-card" :class="{ 'station-card-selected': selected }" :aria-labelledby="`station-${station.id}`">
    <div class="station-heading">
      <h3 :id="`station-${station.id}`"><button v-if="selected !== undefined" type="button" class="station-select" :aria-pressed="selected" @click="$emit('select')">{{ station.name }}</button><template v-else>{{ station.name }}</template></h3>
      <p>직선거리 {{ formatDistance(station.distanceMeters) }}</p>
    </div>
    <ChargerStatusSummary :statuses="station.chargers.map(charger => charger.status)" :total-count="station.compatibleChargerCount" :available-count="station.reportedAvailableCount" />
    <button v-if="station.chargers.length" class="charger-toggle" type="button" :aria-expanded="expanded" :aria-controls="`chargers-${station.id}`" @click="expanded = !expanded">충전기별 보기 <span aria-hidden="true">{{ expanded ? '−' : '+' }}</span></button>
    <ul v-if="expanded && station.chargers.length" :id="`chargers-${station.id}`" class="charger-badges">
      <li v-for="charger in station.chargers" :key="charger.chargerId">
        <span class="charger-label">충전기 {{ charger.chargerId }}</span>
        <StatusBadge :status="charger.status" />
      </li>
    </ul>
    <p v-if="!station.chargers.length">충전기 정보 확인 필요</p>
    <div class="station-actions">
      <a v-if="directionsHref" :id="`station-directions-${station.id}`" :href="directionsHref" :aria-label="`${station.name} 길찾기`" @click="prepareDirections">길찾기</a>
      <button v-else :id="`station-directions-${station.id}`" type="button" class="search-button" :aria-label="`${station.name} 길찾기`" @click="$emit('directions')">길찾기</button>
    <a v-if="detailHref" :href="detailHref" @click.exact.left.prevent="$emit('openDetail')">{{ station.name }} 상세 보기</a>
    </div>
  </article>
</template>
