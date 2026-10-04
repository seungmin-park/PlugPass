export const connectors = ['DC_CHADEMO', 'AC_SLOW', 'AC_THREE_PHASE', 'DC_COMBO', 'NACS'] as const
export type Connector = typeof connectors[number]

// Unknown future status/reason codes remain strings for presentation fallbacks.
export interface ChargerInfo {
  chargerId: string
  status: string
  rawStatus: string | null
  sourceObservedAt: string | null
  collectedAt: string
  freshness: string
  reasonCode: string
  connectorCode: string | null
  useTime: string | null
  limitYn: string | null
  limitDetail: string | null
  note: string | null
  sourceStatusChangedAtRaw: string | null
  lastChargingStartedAtRaw: string | null
  lastChargingEndedAtRaw: string | null
  chargingStartedAtRaw: string | null
  delYn: string | null
  delDetail: string | null
}
