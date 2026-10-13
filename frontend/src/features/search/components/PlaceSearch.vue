<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { usePlaceSearchStore } from '../stores/placeSearchStore'
import type { PlaceMatch } from '../api/placeApi'
import LoadingIndicator from '../../../shared/ui/LoadingIndicator.vue'
import { useErrorFocus } from '../../../shared/ui/useErrorFocus'
const emit = defineEmits<{ select: [place: PlaceMatch] }>()
const store = usePlaceSearchStore()
const query = ref('')
const errorNotice = ref<HTMLElement | null>(null)
useErrorFocus(() => store.status === 'error', errorNotice)
function select(place: PlaceMatch): void { emit('select', place); store.reset() }
onBeforeUnmount(store.reset)
</script>
<template>
  <section class="place-search" aria-label="주소·장소 검색" :aria-busy="store.status === 'loading'">
    <form class="place-search-form" @submit.prevent="store.search(query)">
      <label for="place-query">주소·장소 검색</label>
      <input id="place-query" v-model="query" type="search" maxlength="100" placeholder="지역명, 도로명 주소, 장소명" autocomplete="off" @input="store.reset()" />
      <button type="submit" class="secondary-button" :disabled="store.status === 'loading'">주소·장소 찾기</button>
    </form>
    <LoadingIndicator v-if="store.status === 'loading'" message="주소와 장소를 찾고 있습니다." />
    <p v-if="store.error" ref="errorNotice" tabindex="-1" role="alert">{{ store.error.message }}</p>
    <p v-if="store.status === 'success' && !store.places.length" role="status">찾는 장소가 없습니다. 검색어를 바꿔 주세요.</p>
    <ul v-if="store.places.length" class="place-results" aria-label="위치 후보">
      <li v-for="(place, index) in store.places" :key="index"><button type="button" class="secondary-button place-result" @click="select(place)"><strong>{{ place.name }}</strong><span>{{ place.address }}</span></button></li>
    </ul>
    <p v-if="store.status === 'success'">검색 제공: 카카오 · 최대 10개 위치 후보</p>
  </section>
</template>
