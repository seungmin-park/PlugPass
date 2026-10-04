export function parseStationId(value: unknown): number | null {
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null
  const stationId = Number(value)
  return isStationId(stationId) ? stationId : null
}

export function isStationId(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0
}
