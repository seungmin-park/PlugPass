<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, useId } from 'vue'
import StationDirections from './StationDirections.vue'
import type { StationSummary } from '../../search/types'
import { accessConditionLabel } from '../../charging-info/presentation'
const props = defineProps<{ station: StationSummary; detailHref: string }>()
const emit = defineEmits<{ close: []; 'open-detail': [] }>()
const sheet = ref<HTMLElement | null>(null)
const titleId = useId()
const accessSummary = computed(() => {
  const conditions = props.station.chargers.map(charger => accessConditionLabel(charger.limitYn, charger.limitDetail))
  return [...new Set(conditions)].join(' · ') || '운영 시간과 출입 조건을 확인해 주세요'
})
let previousFocus: HTMLElement | null = null
onMounted(async () => {
  previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  sheet.value?.focus({ preventScroll: true })
})
onUnmounted(() => {
  void nextTick(() => {
    const target = previousFocus?.isConnected ? previousFocus : document.getElementById(previousFocus?.id ?? '')
    target?.focus({ preventScroll: true })
  })
})
</script>
<template>
  <section ref="sheet" class="station-action-sheet" role="dialog" :aria-labelledby="titleId" tabindex="-1" @keydown.esc.stop.prevent="emit('close')">
    <button type="button" class="secondary-button sheet-close" @click="emit('close')">닫기</button>
    <h2 :id="titleId">{{ props.station.name }}</h2>
    <p>{{ station.compatibleChargerCount }}대 중 {{ station.reportedAvailableCount }}대 이용 가능</p>
    <p class="access-summary">{{ accessSummary }}</p>
    <StationDirections :destination="station" expanded />
    <a :href="detailHref" @click.exact.left.prevent="emit('open-detail')">이용 조건·상세 보기</a>
  </section>
</template>
<style scoped>
.station-action-sheet { position: fixed; z-index: 1050; left: 50%; bottom: 0; transform: translateX(-50%); width: min(100%, 560px); max-height: 55dvh; overflow-y: auto; padding: 20px; background: var(--color-panel); border: 1px solid var(--color-line); border-radius: 20px 20px 0 0; box-shadow: 0 -8px 32px #0b1c3020; }
h2 { font-size: 20px; padding-right: 64px; }
p { font-size: 13px; margin-top: 8px; }
.sheet-close { float: right; }
</style>
