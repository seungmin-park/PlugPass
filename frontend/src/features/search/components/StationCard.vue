<script setup lang="ts">
import type { StationSummary } from '../types'
import { formatDistance } from '../../charging-info/presentation'
import StatusBadge from '../../charging-info/components/StatusBadge.vue'
import FreshnessBadge from '../../charging-info/components/FreshnessBadge.vue'
defineProps<{ station: StationSummary }>()
</script>
<template>
  <article class="station-card" :aria-labelledby="`station-${station.id}`">
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
  </article>
</template>
