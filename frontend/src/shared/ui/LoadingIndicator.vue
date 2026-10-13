<script setup lang="ts">
defineProps<{ message: string }>()
</script>

<template>
  <p class="loading-indicator" role="status" aria-live="polite" aria-atomic="true">
    <span class="loading-spinner" aria-hidden="true"></span>
    <span>{{ message }}</span>
  </p>
</template>

<style scoped>
.loading-indicator { display: flex; align-items: center; gap: 12px; min-height: 44px; padding: 12px 16px; margin-block: 12px; border-radius: 10px; background: var(--color-notice); overflow-wrap: anywhere; }
.loading-spinner { display: inline-block; flex: 0 0 24px; width: 24px; height: 24px; border: 3px solid var(--color-line); border-top-color: var(--color-primary); border-right-color: var(--color-primary); border-radius: 50%; animation: loading-spin .8s linear infinite; }
@keyframes loading-spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .loading-spinner { animation: none; } }
</style>
