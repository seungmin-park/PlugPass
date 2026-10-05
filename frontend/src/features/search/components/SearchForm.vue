<script setup lang="ts">
import { computed, ref, watchPostEffect } from 'vue'
import type { Connector } from '../../charging-info/types'

const props = withDefaults(defineProps<{ initialRadiusMeters?: number; initialConnector?: Connector; fieldErrors?: Readonly<Record<string, string>> }>(), {
  initialRadiusMeters: 1000, initialConnector: 'DC_COMBO', fieldErrors: () => ({}),
})
const radiusGroup = ref<HTMLElement | null>(null)
const connectorGroup = ref<HTMLElement | null>(null)
watchPostEffect(() => {
  if (props.fieldErrors.radiusMeters) { radiusGroup.value?.querySelector<HTMLInputElement>('input:checked')?.focus(); return }
  if (props.fieldErrors.connector) connectorGroup.value?.querySelector<HTMLInputElement>('input:checked')?.focus()
})
const radiusMeters = ref(props.initialRadiusMeters)
const connector = ref<Connector>(props.initialConnector)
const emit = defineEmits<{ submit: [conditions: { radiusMeters: number; connector: Connector }] }>()
const standardRadiusOptions = [
  { value: 500, label: '500m' },
  { value: 1000, label: '1km' },
  { value: 3000, label: '3km' },
  { value: 5000, label: '5km' },
  { value: 10000, label: '10km' },
]
const radiusOptions = computed(() => standardRadiusOptions.some(option => option.value === props.initialRadiusMeters)
  ? standardRadiusOptions : [...standardRadiusOptions, { value: props.initialRadiusMeters, label: `${props.initialRadiusMeters}m` }]
    .sort((first, second) => first.value - second.value))
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
    <fieldset ref="radiusGroup" class="form-field">
      <legend>검색 반경</legend>
      <div class="radius-options">
        <label v-for="option in radiusOptions" :key="option.value" class="filter-option">
          <input v-model.number="radiusMeters" type="radio" name="radiusMeters" :value="option.value"
            :aria-invalid="!!fieldErrors.radiusMeters" :aria-describedby="fieldErrors.radiusMeters ? 'radius-error' : undefined" />
          <span>{{ option.label }}</span>
        </label>
      </div>
      <p v-if="fieldErrors.radiusMeters" id="radius-error" class="field-error">{{ fieldErrors.radiusMeters }}</p>
    </fieldset>
    <fieldset ref="connectorGroup" class="form-field">
      <legend>차량 커넥터</legend>
      <div class="connector-options">
        <label v-for="option in connectorOptions" :key="option.value" class="filter-option">
          <input v-model="connector" type="radio" name="connector" :value="option.value"
            :aria-invalid="!!fieldErrors.connector" :aria-describedby="fieldErrors.connector ? 'connector-error' : undefined" />
          <span>{{ option.label }}</span>
        </label>
      </div>
      <p v-if="fieldErrors.connector" id="connector-error" class="field-error">{{ fieldErrors.connector }}</p>
    </fieldset>
    <button type="submit" class="search-button">
      <svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5" /></svg>
      주변 충전소 검색
    </button>
  </form>
</template>
