<script setup lang="ts">
import type { CollectionStatus } from '../../search/types'
import { formatTimestamp } from '../presentation'
defineProps<{ collection?: CollectionStatus | null | undefined }>()
</script>
<template>
  <div class="collection-summary">
    <p v-if="collection?.state === 'DELAYED'" role="status">정보 갱신 지연·출발 전 확인</p>
    <p v-else-if="collection?.state === 'NOT_READY'" role="status">전체 데이터 준비 전 · 일부 정보가 없을 수 있습니다.</p>
    <p v-else-if="collection?.state === 'CURRENT'">수집 정상</p>
    <p v-else>수집 상태 확인 필요</p>
    <p v-if="collection?.state !== 'NOT_READY'">최근 상태 수집: {{ formatTimestamp(collection?.lastStatusSuccessAt ?? collection?.lastCatalogSuccessAt ?? null) }}</p>
  </div>
</template>
