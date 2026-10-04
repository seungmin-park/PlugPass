<script setup lang="ts">
import { ref } from 'vue'
import type { Connector } from '../../charging-info/types'

const radiusMeters = ref(1000)
const connector = ref<Connector>('DC_COMBO')
const emit = defineEmits<{ submit: [conditions: { radiusMeters: number; connector: Connector }] }>()
const radiusOptions = [
  { value: 500, label: '500m' },
  { value: 1000, label: '1km' },
  { value: 3000, label: '3km' },
  { value: 5000, label: '5km' },
  { value: 10000, label: '10km' },
]
const connectorOptions: ReadonlyArray<{ value: Connector; label: string }> = [
  { value: 'DC_COMBO', label: 'DC 콤보' },
  { value: 'DC_CHADEMO', label: 'CHAdeMO' },
  { value: 'AC_SLOW', label: 'AC 완속' },
  { value: 'AC_THREE_PHASE', label: 'AC 3상' },
  { value: 'NACS', label: 'NACS' },
]
</script>

<template>
  <form class="search-form" @submit.prevent="emit('submit', { radiusMeters, connector })">
    <fieldset class="form-field">
      <legend>검색 반경</legend>
      <div class="radius-options">
        <label v-for="option in radiusOptions" :key="option.value" class="filter-option">
          <input v-model.number="radiusMeters" type="radio" name="radiusMeters" :value="option.value" />
          <span>{{ option.label }}</span>
        </label>
      </div>
    </fieldset>
    <fieldset class="form-field">
      <legend>차량 커넥터</legend>
      <div class="connector-options">
        <label v-for="option in connectorOptions" :key="option.value" class="filter-option">
          <input v-model="connector" type="radio" name="connector" :value="option.value" />
          <span>{{ option.label }}</span>
        </label>
      </div>
    </fieldset>
    <button type="submit" class="search-button">
      <svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5" /></svg>
      주변 충전소 검색
    </button>
  </form>
</template>
