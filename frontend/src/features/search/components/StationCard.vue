<script setup lang="ts">
import type { StationSummary } from '../types'
import { formatDistance } from '../../charging-info/presentation'
import StatusBadge from '../../charging-info/components/StatusBadge.vue'
import FreshnessBadge from '../../charging-info/components/FreshnessBadge.vue'
import { scheduleIosInstallFallback } from '../../station-detail/navigation/navigationHandoff'
const props = defineProps<{ station: StationSummary; detailHref?: string; directionsHref?: string | undefined; directionsInstallHref?: string | undefined; selected?: boolean }>()
defineEmits<{ openDetail: []; directions: [] }>()
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
    <p class="charger-counts">호환 충전기 {{ station.compatibleChargerCount }}대 · 이용 가능 보고 {{ station.reportedAvailableCount }}대</p>
    <ul v-if="station.chargers.length" class="charger-badges">
      <li v-for="charger in station.chargers" :key="charger.chargerId">
        <span class="charger-label">충전기 {{ charger.chargerId }}</span>
        <StatusBadge :status="charger.status" />
        <FreshnessBadge :freshness="charger.freshness" />
      </li>
    </ul>
    <p v-else>충전기 정보 확인 필요</p>
    <div class="station-actions">
      <a v-if="directionsHref" :id="`station-directions-${station.id}`" :href="directionsHref" :aria-label="`${station.name} 길찾기`" @click="prepareDirections">길찾기</a>
      <button v-else :id="`station-directions-${station.id}`" type="button" class="search-button" :aria-label="`${station.name} 길찾기`" @click="$emit('directions')">길찾기</button>
    <a v-if="detailHref" :href="detailHref" @click.exact.left.prevent="$emit('openDetail')">{{ station.name }} 상세 보기</a>
    </div>
  </article>
</template>
