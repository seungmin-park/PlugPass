<script setup lang="ts">
import { ref, watch, watchPostEffect } from 'vue'
import type { Connector } from '../../charging-info/types'

const props = withDefaults(defineProps<{ initialConnector?: Connector; fieldErrors?: Readonly<Record<string, string>> }>(), {
  initialConnector: 'DC_COMBO', fieldErrors: () => ({}),
})
const connectorGroup = ref<HTMLElement | null>(null)
watchPostEffect(() => {
  if (props.fieldErrors.connector) connectorGroup.value?.querySelector<HTMLInputElement>('input:checked')?.focus()
})
const connector = ref<Connector>(props.initialConnector)
const emit = defineEmits<{ submit: [conditions: { connector: Connector }]; change: [connector: Connector] }>()
watch(connector, value => emit('change', value))
const connectorOptions: ReadonlyArray<{ value: Connector; label: string }> = [
  { value: 'DC_COMBO', label: 'DC 콤보' },
  { value: 'DC_CHADEMO', label: 'CHAdeMO' },
  { value: 'AC_SLOW', label: 'AC 완속' },
  { value: 'AC_THREE_PHASE', label: 'AC 3상' },
  { value: 'NACS', label: 'NACS' },
]
</script>

<template>
  <form class="search-form" @submit.prevent="emit('submit', { connector })">
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
