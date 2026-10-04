<script setup lang="ts">
import { ref } from 'vue'
import type { Connector } from '../../charging-info/types'

const radiusMeters = ref(1000)
const connector = ref<Connector>('DC_COMBO')
const emit = defineEmits<{ submit: [conditions: { radiusMeters: number; connector: Connector }] }>()
</script>

<template>
  <form class="search-form" @submit.prevent="emit('submit', { radiusMeters, connector })">
    <div class="form-field">
      <label for="radius">검색 반경</label>
      <select id="radius" v-model.number="radiusMeters" name="radiusMeters">
        <option :value="500">500m</option>
        <option :value="1000">1km</option>
        <option :value="3000">3km</option>
        <option :value="5000">5km</option>
        <option :value="10000">10km</option>
      </select>
    </div>
    <div class="form-field">
      <label for="connector">차량 커넥터</label>
      <select id="connector" v-model="connector" name="connector">
        <option value="DC_CHADEMO">CHAdeMO</option>
        <option value="AC_SLOW">AC 완속</option>
        <option value="AC_THREE_PHASE">AC 3상</option>
        <option value="DC_COMBO">DC 콤보</option>
        <option value="NACS">NACS</option>
      </select>
    </div>
    <button type="submit" class="search-button">주변 충전소 검색</button>
  </form>
</template>
