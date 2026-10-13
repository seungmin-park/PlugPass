<script setup lang="ts">
import type { ChargerInfo } from '../../charging-info/types'
import { accessConditionLabel, formatSourceStatusChangedAt, formatTimestamp, observationSummary, reasonLabel } from '../../charging-info/presentation'
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
      <div class="charger-summary">
        <p>{{ reasonLabel(charger.reasonCode) }}</p>
        <p>운영 시간: {{ charger.useTime?.trim() || '확인 필요' }}</p>
        <p>이용 조건: {{ accessConditionLabel(charger.limitYn, charger.limitDetail) }}</p>
        <p>{{ observationSummary(charger.sourceObservedAt) }}</p>
        <p v-if="charger.note?.trim()">현장 안내: {{ charger.note.trim() }}</p>
        <p v-if="charger.delYn === 'Y'">공급자가 삭제한 충전기입니다. 이용 전 확인해 주세요.</p>
      </div>
      <details class="charger-source">
        <summary>정보 출처와 원본 데이터</summary>
        <p>현장 관측 시각: {{ formatTimestamp(charger.sourceObservedAt) }} · 현장에서 상태를 확인한 시각입니다.</p>
        <p>서버 수집 시각: {{ formatTimestamp(charger.collectedAt) }} · 서버가 정보를 받은 시각이며 현장 관측 시각과 다릅니다.</p>
        <p v-if="charger.sourceStatusChangedAtRaw?.trim()">공급자 상태 변경 시각: {{ formatSourceStatusChangedAt(charger.sourceStatusChangedAtRaw) }} · 현장 관측 아님</p>
        <p>원본 상태 코드: {{ charger.rawStatus ?? '확인 불가' }}</p>
        <p>원본 커넥터 코드: {{ charger.connectorCode ?? '확인 불가' }}</p>
        <p v-if="charger.delYn?.trim()">공급자 삭제 표시: {{ charger.delYn.trim() }}</p>
        <p v-if="charger.delDetail?.trim()">공급자 삭제 설명: {{ charger.delDetail.trim() }}</p>
      </details>
    </li>
  </ul>
</template>
