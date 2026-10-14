<script setup lang="ts">
import { computed } from 'vue'
import { statusLabel } from '../presentation'
const props = defineProps<{ statuses: readonly string[]; totalCount: number; availableCount: number }>()
const distribution = computed(() => ['AVAILABLE', 'OCCUPIED', 'UNAVAILABLE', 'UNKNOWN'].map(status => ({
  status, count: props.statuses.filter(value => (['AVAILABLE', 'OCCUPIED', 'UNAVAILABLE'].includes(value) ? value : 'UNKNOWN') === status).length,
})).filter(item => item.count > 0))
</script>
<template>
  <p class="charger-counts">{{ totalCount }}대 중 <strong>{{ availableCount }}대 이용 가능</strong></p>
  <p class="status-distribution"><span v-for="item in distribution" :key="item.status" :class="`status-${item.status.toLowerCase()}`">{{ statusLabel(item.status) }} {{ item.count }}대</span></p>
</template>
