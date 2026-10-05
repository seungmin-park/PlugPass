import { defineStore } from 'pinia'
import { onScopeDispose, ref } from 'vue'
import { getStations } from '../api/stationApi'
import type { SearchCriteria, StationSearchResponse } from '../types'
import { toApiError, type ApiError } from '../../../shared/api/apiError'

export const useStationSearchStore = defineStore('station-search', () => {
  const status = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
  const response = ref<StationSearchResponse | null>(null)
  const error = ref<ApiError | null>(null)
  const fetchedAt = ref<string | null>(null)
  let requestSequence = 0
  let activeRequest: AbortController | null = null

  function reset(): void {
    requestSequence += 1
    activeRequest?.abort()
    activeRequest = null
    status.value = 'idle'
    response.value = null
    error.value = null
    fetchedAt.value = null
  }

  async function search(criteria: SearchCriteria): Promise<void> {
    reset()
    status.value = 'loading'
    const sequence = requestSequence
    activeRequest = new AbortController()
    try {
      const result = await getStations({ ...criteria }, activeRequest.signal)
      if (sequence !== requestSequence) return
      response.value = result
      fetchedAt.value = new Date().toISOString()
      status.value = 'success'
    } catch (failure: unknown) {
      if (sequence !== requestSequence) return
      error.value = toApiError(failure)
      status.value = 'error'
    } finally {
      if (sequence === requestSequence) activeRequest = null
    }
  }

  onScopeDispose(reset)
  return { status, response, error, fetchedAt, search, reset }
})
