<script setup lang="ts">
import { computed, ref } from 'vue'
import type { StationSummary } from '../types'
import { formatDistance } from '../../charging-info/presentation'
import StatusBadge from '../../charging-info/components/StatusBadge.vue'
import { statusLabel } from '../../charging-info/presentation'
import { scheduleIosInstallFallback } from '../../station-detail/navigation/navigationHandoff'
const props = defineProps<{ station: StationSummary; detailHref?: string; directionsHref?: string | undefined; directionsInstallHref?: string | undefined; selected?: boolean }>()
defineEmits<{ openDetail: []; directions: [] }>()
const expanded = ref(false)
const distribution = computed(() => ['AVAILABLE', 'OCCUPIED', 'UNAVAILABLE', 'UNKNOWN'].map(status => ({
  status, count: props.station.chargers.filter(charger => (['AVAILABLE', 'OCCUPIED', 'UNAVAILABLE'].includes(charger.status) ? charger.status : 'UNKNOWN') === status).length,
})).filter(item => item.count > 0))
function prepareDirections(event: MouseEvent): void {
  if (event.defaultPrevented || !props.directionsInstallHref) return
  scheduleIosInstallFallback(document, props.directionsInstallHref, href => window.location.assign(href))
}
</script>
<template>
  <article class="station-card" :class="{ 'station-card-selected': selected }" :aria-labelledby="`station-${station.id}`">
    <div class="station-heading">
      <h3 :id="`station-${station.id}`">{{ station.name }}</h3>
      <p>직선거리 {{ formatDistance(station.distanceMeters) }}</p>
    </div>
    <p class="charger-counts">{{ station.compatibleChargerCount }}대 중 <strong>{{ station.reportedAvailableCount }}대 이용 가능</strong></p>
    <p v-if="station.chargers.length" class="status-distribution"><span v-for="item in distribution" :key="item.status">{{ statusLabel(item.status).replace(' 보고', '') }} {{ item.count }}대</span></p>
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
