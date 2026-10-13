<script setup lang="ts">
import { computed, ref, watch, watchPostEffect } from 'vue'
import type { Connector } from '../../charging-info/types'
import type { SearchConditions } from '../types'
import { adapterCatalog, vehicleCatalog } from '../../vehicle/vehicleCatalog'
import { resolveVehicleCompatibility } from '../../vehicle/vehicleCompatibility'

const props = withDefaults(defineProps<{
  initialConnector?: Connector
  initialMode?: 'choose' | 'manual'
  initialVehicleId?: string | undefined
  initialAdapterIds?: readonly string[] | undefined
  initialVehicleSupportConfirmed?: boolean | undefined
  fieldErrors?: Readonly<Record<string, string>>
}>(), {
  initialConnector: 'DC_COMBO', initialMode: 'choose', initialAdapterIds: () => [], fieldErrors: () => ({}),
})
const emit = defineEmits<{ submit: [conditions: SearchConditions]; change: [conditions: SearchConditions | null] }>()
const connectorGroup = ref<HTMLElement | null>(null)
const vehicleChoice = ref<HTMLSelectElement | null>(null)
const mode = ref(props.initialVehicleId ?? (props.initialMode === 'manual' ? 'manual' : ''))
const connector = ref<Connector>(props.initialConnector)
const adapterIds = ref<string[]>([...props.initialAdapterIds])
const vehicleSupportConfirmed = ref(props.initialVehicleSupportConfirmed ?? false)
const localError = ref('')
const selectedVehicle = computed(() => vehicleCatalog.find(vehicle => vehicle.id === mode.value))
const availableAdapters = computed(() => adapterCatalog.filter(adapter => adapter.vehicleId === mode.value))
const selectedCompatibility = computed(() => resolveVehicleCompatibility({ vehicleId: mode.value, adapterIds: adapterIds.value,
  vehicleSupportConfirmed: vehicleSupportConfirmed.value }))
const conditions = computed<SearchConditions | null>(() => {
  if (mode.value === 'manual') return { connector: connector.value }
  if (!selectedVehicle.value || selectedCompatibility.value.requiresConfirmation) return null
  const first = selectedCompatibility.value.connectors[0]
  if (!first) return null
  return { connector: first, connectors: selectedCompatibility.value.connectors, vehicleId: selectedVehicle.value.id,
    adapterIds: [...adapterIds.value], ...(vehicleSupportConfirmed.value ? { vehicleSupportConfirmed: true } : {}) }
})
watch(mode, () => {
  localError.value = ''
  if (mode.value !== 'tesla-kr') { adapterIds.value = []; vehicleSupportConfirmed.value = false }
})
watch(conditions, value => { localError.value = ''; emit('change', value) })
watchPostEffect(() => {
  if (!props.fieldErrors.connector && !props.fieldErrors.connectors && !props.fieldErrors.vehicleId) return
  if (mode.value === 'manual') connectorGroup.value?.querySelector<HTMLInputElement>('input:checked')?.focus()
  else vehicleChoice.value?.focus()
})
const connectorOptions: ReadonlyArray<{ value: Connector; label: string }> = [
  { value: 'DC_COMBO', label: 'DC 콤보' },
  { value: 'DC_CHADEMO', label: 'CHAdeMO' },
  { value: 'AC_SLOW', label: 'AC 완속' },
  { value: 'AC_THREE_PHASE', label: 'AC 3상' },
  { value: 'NACS', label: 'NACS' },
]
function submit(): void {
  if (!conditions.value) {
    localError.value = mode.value ? '이번에 사용할 어댑터와 차량 지원 여부를 확인해 주세요' : '차량을 먼저 선택해 주세요'
    vehicleChoice.value?.focus()
    return
  }
  emit('submit', conditions.value)
}
</script>

<template>
  <form class="search-form" @submit.prevent="submit">
    <div class="form-field vehicle-choice">
      <label for="vehicle-choice">내 차량</label>
      <select id="vehicle-choice" ref="vehicleChoice" v-model="mode" :aria-invalid="!!fieldErrors.vehicleId || !!localError" :aria-describedby="localError ? 'vehicle-error' : undefined">
        <option value="">차량을 선택해 주세요</option>
        <option v-for="vehicle in vehicleCatalog" :key="vehicle.id" :value="vehicle.id">{{ vehicle.label }}</option>
        <option value="manual">목록에 없는 차량 · 직접 확인</option>
      </select>
      <p v-if="localError" id="vehicle-error" class="field-error" role="alert">{{ localError }}</p>
      <p v-if="selectedVehicle" class="vehicle-source">{{ selectedVehicle.modelYear ? `${selectedVehicle.modelYear}년 국내형 기준` : '차량 충전 사양 확인 필요' }} · <a :href="selectedVehicle.sourceUrl" target="_blank" rel="noopener noreferrer">제조사 자료 확인</a></p>
    </div>
    <fieldset v-if="availableAdapters.length" class="form-field adapter-options">
      <legend>이번에 가지고 가서 사용할 어댑터</legend>
      <label v-for="adapter in availableAdapters" :key="adapter.id" class="adapter-option">
        <input v-model="adapterIds" type="checkbox" :value="adapter.id" />
        <span>{{ adapter.label }}</span>
        <small>{{ adapter.caution }}</small>
      </label>
      <label class="adapter-confirmation"><input id="vehicle-support-confirmed" v-model="vehicleSupportConfirmed" type="checkbox" /> 내 차량에서 선택한 어댑터 사용 가능 여부를 확인했습니다</label>
      <p>별도 구매·보유만으로 사용 가능하다고 보지 않습니다. 충전기 조건도 출발 전에 확인해 주세요.</p>
    </fieldset>
    <fieldset v-if="mode === 'manual'" ref="connectorGroup" class="form-field">
      <legend>내 차량 충전구 직접 확인</legend>
      <p>목록에 없는 차량은 차량 설명서의 충전 규격을 확인해 주세요.</p>
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
