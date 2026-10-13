import { defineStore } from 'pinia'
import { onScopeDispose, ref } from 'vue'
import { getPlaces, type PlaceMatch } from '../api/placeApi'
import { toApiError, type ApiError } from '../../../shared/api/apiError'
export const usePlaceSearchStore = defineStore('place-search', () => {
  const status = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
  const places = ref<PlaceMatch[]>([])
  const error = ref<ApiError | null>(null)
  let sequence = 0
  let active: AbortController | null = null
  function reset(): void {
    sequence += 1
    active?.abort()
    active = null
    status.value = 'idle'
    places.value = []
    error.value = null
  }
  async function search(query: string): Promise<void> {
    reset()
    const current = sequence
    active = new AbortController()
    status.value = 'loading'
    try {
      const response = await getPlaces(query, active.signal)
      if (sequence !== current) return
      places.value = response
      status.value = 'success'
    } catch (failure: unknown) {
      if (sequence !== current) return
      error.value = toApiError(failure)
      status.value = 'error'
    } finally { if (sequence === current) active = null }
  }
  onScopeDispose(reset)
  return { status, places, error, search, reset }
})
