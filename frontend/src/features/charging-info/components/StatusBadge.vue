<script setup lang="ts">
import { computed } from 'vue'
import { statusLabel } from '../presentation'
const props = defineProps<{ status: string }>()
const tone = computed(() => {
  switch (props.status) {
    case 'AVAILABLE': return 'status-available'
    case 'OCCUPIED': return 'status-occupied'
    case 'UNAVAILABLE': return 'status-unavailable'
    default: return 'status-unknown'
  }
})
</script>
<template><span class="info-badge status-badge" :class="tone">{{ statusLabel(status) }}</span></template>
