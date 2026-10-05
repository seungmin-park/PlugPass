<script setup lang="ts">
import type { ChargerInfo } from '../../charging-info/types'
import { formatTimestamp, reasonLabel } from '../../charging-info/presentation'
import StatusBadge from '../../charging-info/components/StatusBadge.vue'
import FreshnessBadge from '../../charging-info/components/FreshnessBadge.vue'
defineProps<{ chargers: ChargerInfo[] }>()
</script>
<template>
  <p v-if="!chargers.length" role="status">충전기 정보가 없습니다. 이용 전 확인해 주세요.</p>
  <ul v-else class="charger-details">
    <li v-for="charger in chargers" :key="charger.chargerId" class="charger-detail">
      <h3>충전기 {{ charger.chargerId }}</h3>
      <div class="charger-badges"><StatusBadge :status="charger.status" /><FreshnessBadge :freshness="charger.freshness" /></div>
      <p>{{ reasonLabel(charger.reasonCode) }}</p>
      <p>관측 시각: {{ formatTimestamp(charger.sourceObservedAt) }}</p>
      <p>수집 시각: {{ formatTimestamp(charger.collectedAt) }}</p>
      <p>원본 상태 코드: {{ charger.rawStatus ?? '확인 불가' }}</p>
      <p>원본 커넥터 코드: {{ charger.connectorCode ?? '확인 불가' }}</p>
      <p>운영 시간: {{ charger.useTime?.trim() || '이용 조건 확인 필요' }}</p>
      <p>이용 제한: {{ charger.limitYn === 'Y' ? '제한 있음 (Y)' : charger.limitYn === 'N' ? '제한 없음 보고 (N)' : '이용 조건 확인 필요' }}</p>
      <p>이용 조건: {{ charger.limitDetail?.trim() || '이용 조건 확인 필요' }}</p>
      <p>비고: {{ charger.note ?? '확인 불가' }}</p>
      <p>공급자 삭제 표시: {{ charger.delYn ?? '확인 불가' }} · {{ charger.delDetail ?? '확인 불가' }}</p>
      <p>원본 상태 변경 시각 (시간대 미확인): {{ charger.sourceStatusChangedAtRaw ?? '확인 불가' }}</p>
    </li>
  </ul>
</template>
