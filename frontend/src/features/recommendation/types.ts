export interface RecommendedStation {
  id: number
  name: string
  distanceMeters: number
  reasonCodes: string[]
  chargerStatuses?: readonly string[]
}
export interface RecommendationResponse {
  preferred: RecommendedStation[]
  requiresConfirmation: RecommendedStation[]
  excluded: RecommendedStation[]
}
