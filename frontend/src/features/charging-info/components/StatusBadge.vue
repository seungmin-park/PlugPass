<script setup lang="ts">
import { statusLabel } from '../presentation'
defineProps<{ status: string }>()
</script>
<template><span class="info-badge status-badge">{{ statusLabel(status) }}</span></template>
