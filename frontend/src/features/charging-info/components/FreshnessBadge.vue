<script setup lang="ts">
import { freshnessLabel } from '../presentation'
defineProps<{ freshness: string }>()
</script>
<template><span class="info-badge" :class="{ 'freshness-recent': freshness === 'RECENT', 'freshness-uncertain': freshness !== 'RECENT' }">{{ freshnessLabel(freshness) }}</span></template>
