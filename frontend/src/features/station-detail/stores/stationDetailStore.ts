import { defineStore } from 'pinia'
import { onScopeDispose, ref } from 'vue'
import { getStationDetail } from '../api/stationDetailApi'
import type { StationDetail } from '../types'
import { toApiError, type ApiError } from '../../../shared/api/apiError'

export const useStationDetailStore = defineStore('station-detail', () => {
  const status = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
  const station = ref<StationDetail | null>(null)
  const error = ref<ApiError | null>(null)
  const fetchedAt = ref<string | null>(null)
  let requestSequence = 0
  let activeRequest: AbortController | null = null

  function reset(): void {
    requestSequence += 1
    activeRequest?.abort()
    activeRequest = null
    status.value = 'idle'
    station.value = null
    error.value = null
    fetchedAt.value = null
  }
  async function loadStation(stationId: number): Promise<void> {
    reset()
    status.value = 'loading'
    const sequence = requestSequence
    activeRequest = new AbortController()
    try {
      const result = await getStationDetail(stationId, activeRequest.signal)
      if (sequence !== requestSequence) return
      station.value = result
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
  return { status, station, error, fetchedAt, loadStation, reset }
})
