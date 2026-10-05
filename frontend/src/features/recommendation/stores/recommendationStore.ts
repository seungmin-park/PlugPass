import { defineStore } from 'pinia'
import { onScopeDispose, ref } from 'vue'
import { getRecommendations } from '../api/recommendationApi'
import { getStations } from '../../search/api/stationApi'
import type { SearchCriteria, StationSearchResponse } from '../../search/types'
import type { RecommendationResponse } from '../types'
import { toApiError, type ApiError } from '../../../shared/api/apiError'

export const useRecommendationStore = defineStore('recommendation', () => {
  const candidateStatus = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
  const candidates = ref<RecommendationResponse | null>(null)
  const candidateError = ref<ApiError | null>(null)
  const candidatesFetchedAt = ref<string | null>(null)
  const metadataStatus = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
  const metadata = ref<StationSearchResponse | null>(null)
  const metadataError = ref<ApiError | null>(null)
  const metadataFetchedAt = ref<string | null>(null)
  let selectedCriteria: SearchCriteria | null = null
  let excludedStationId: number | null = null
  let candidateSequence = 0
  let metadataSequence = 0
  let candidateRequest: AbortController | null = null
  let metadataRequest: AbortController | null = null

  function clearCandidates(): void {
    candidateSequence += 1
    candidateRequest?.abort()
    candidateRequest = null
    candidateStatus.value = 'idle'
    candidates.value = null
    candidateError.value = null
    candidatesFetchedAt.value = null
  }
  function clearMetadata(): void {
    metadataSequence += 1
    metadataRequest?.abort()
    metadataRequest = null
    metadataStatus.value = 'idle'
    metadata.value = null
    metadataError.value = null
    metadataFetchedAt.value = null
  }
  function reset(): void {
    clearCandidates()
    clearMetadata()
    selectedCriteria = null
    excludedStationId = null
  }
  async function retryCandidates(): Promise<void> {
    if (!selectedCriteria || excludedStationId === null) return
    clearCandidates()
    candidateStatus.value = 'loading'
    const sequence = candidateSequence
    candidateRequest = new AbortController()
    try {
      const result = await getRecommendations({ ...selectedCriteria }, excludedStationId, candidateRequest.signal)
      if (sequence !== candidateSequence) return
      candidates.value = result
      candidatesFetchedAt.value = new Date().toISOString()
      candidateStatus.value = 'success'
    } catch (failure: unknown) {
      if (sequence !== candidateSequence) return
      candidateError.value = toApiError(failure)
      candidateStatus.value = 'error'
    } finally {
      if (sequence === candidateSequence) candidateRequest = null
    }
  }
  async function retryMetadata(): Promise<void> {
    if (!selectedCriteria) return
    clearMetadata()
    metadataStatus.value = 'loading'
    const sequence = metadataSequence
    metadataRequest = new AbortController()
    try {
      const result = await getStations({ ...selectedCriteria }, metadataRequest.signal)
      if (sequence !== metadataSequence) return
      metadata.value = result
      metadataFetchedAt.value = new Date().toISOString()
      metadataStatus.value = 'success'
    } catch (failure: unknown) {
      if (sequence !== metadataSequence) return
      metadataError.value = toApiError(failure)
      metadataStatus.value = 'error'
    } finally {
      if (sequence === metadataSequence) metadataRequest = null
    }
  }
  async function loadAlternatives(criteria: SearchCriteria, excludeStationId: number): Promise<void> {
    reset()
    selectedCriteria = { ...criteria }
    excludedStationId = excludeStationId
    await Promise.all([retryCandidates(), retryMetadata()])
  }
  onScopeDispose(reset)
  return { candidateStatus, candidates, candidateError, candidatesFetchedAt, metadataStatus, metadata, metadataError,
    metadataFetchedAt, loadAlternatives, retryCandidates, retryMetadata, reset }
})
