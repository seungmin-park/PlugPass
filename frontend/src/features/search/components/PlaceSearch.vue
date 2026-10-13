<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { usePlaceSearchStore } from '../stores/placeSearchStore'
import type { PlaceMatch } from '../api/placeApi'
import LoadingIndicator from '../../../shared/ui/LoadingIndicator.vue'
import { useErrorFocus } from '../../../shared/ui/useErrorFocus'
const emit = defineEmits<{ select: [place: PlaceMatch] }>()
const store = usePlaceSearchStore()
const query = ref('')
const suggestAsYouType = Boolean(import.meta.env.VITE_KAKAO_JS_KEY)
let suggestionTimer: ReturnType<typeof setTimeout> | null = null
const errorNotice = ref<HTMLElement | null>(null)
useErrorFocus(() => store.status === 'error', errorNotice)
function clearSuggestionTimer(): void {
  if (suggestionTimer !== null) clearTimeout(suggestionTimer)
  suggestionTimer = null
}
function onInput(): void {
  clearSuggestionTimer()
  store.reset()
  if (!suggestAsYouType || query.value.trim().length < 2) return
  suggestionTimer = setTimeout(() => { void store.search(query.value) }, 300)
}
function submit(): void { clearSuggestionTimer(); void store.search(query.value) }
function select(place: PlaceMatch): void { clearSuggestionTimer(); query.value = place.name; emit('select', place); store.reset() }
onBeforeUnmount(() => { clearSuggestionTimer(); store.reset() })
</script>
<template>
  <section class="place-search" aria-label="주소·장소 검색" :aria-busy="store.status === 'loading'">
    <form class="place-search-form" @submit.prevent="submit">
      <label for="place-query">주소·장소 검색</label>
      <input id="place-query" v-model="query" type="search" maxlength="100" placeholder="지역명, 도로명 주소, 장소명" autocomplete="off" @input="onInput" />
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
