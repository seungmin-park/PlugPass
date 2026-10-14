const statusLabels = new Map<string, string>([
  ['AVAILABLE', '이용 가능'], ['OCCUPIED', '사용 중'], ['UNAVAILABLE', '이용 불가'], ['UNKNOWN', '상태 확인 불가'],
])
const freshnessLabels = new Map<string, string>([
  ['RECENT', '최근 관측'], ['STALE', '오래된 정보'], ['UNVERIFIED', '최신성 확인 불가'],
])
export function statusLabel(status: string): string { return statusLabels.get(status) ?? '상태 확인 불가' }
export function freshnessLabel(freshness: string): string { return freshnessLabels.get(freshness) ?? '최신성 확인 불가' }
const reasonLabels = new Map<string, string>([
  ['RECENT_AVAILABLE', '최근 확인된 이용 가능 충전기가 있어 먼저 살펴볼 수 있습니다'],
  ['UNVERIFIED_AVAILABLE', '이용 가능한 충전기가 있습니다. 출발 전 상태를 확인해 주세요'],
  ['INCOMPATIBLE_CONNECTOR', '선택한 차량 커넥터와 호환되지 않습니다'],
  ['DELETED_BY_PROVIDER', '공급자가 삭제한 충전기입니다'],
  ['ACCESS_RESTRICTED', '이용 제한이 보고되었습니다'],
  ['STATUS_UNKNOWN', '충전기 상태를 확인할 수 없습니다'],
  ['NOT_AVAILABLE', '이용 가능 상태로 보고되지 않았습니다'],
  ['NO_COMPATIBLE_CHARGER', '호환 충전기 정보가 없습니다'],
  ['ACCESS_CONDITIONS_UNVERIFIED', '출입 조건 확인이 필요합니다'],
  ['OPERATING_HOURS_UNVERIFIED', '운영 시간을 확인할 수 없습니다'],
  ['OPERATING_HOURS_REQUIRE_CHECK', '운영 시간을 확인해 주세요'],
  ['SOURCE_OBSERVED_AT_MISSING', '관측 시각이 제공되지 않았습니다'],
  ['SOURCE_OBSERVED_AT_IN_FUTURE', '관측 시각이 미래여서 확인이 필요합니다'],
  ['WITHIN_MAX_AGE', '서버의 최신성 기준 이내에 관측되었습니다'],
  ['MAX_AGE_EXCEEDED', '서버의 최신성 기준을 지난 정보입니다'],
])
export function reasonLabel(reason: string): string { return reasonLabels.get(reason) ?? '상세 확인이 필요한 사유' }
export function accessConditionLabel(limitYn: string | null, limitDetail: string | null): string {
  const detail = limitDetail?.trim()
  if (detail) return `${detail} · 방문 전 운영 주체에 확인해 주세요`
  if (limitYn === 'Y') return '이용 제한이 보고됐습니다. 방문 전 운영 주체에 확인해 주세요'
  if (limitYn === 'N') return '별도 이용 제한이 보고되지 않았습니다. 현장 안내를 확인해 주세요'
  return '이용 조건 정보가 없습니다. 방문 전 운영 주체에 확인해 주세요'
}
export function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)}m`
  return `${new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 1 }).format(meters / 1000)}km`
}
export function formatTimestamp(time: string | null): string {
  if (!time) return '확인 불가'
  const date = new Date(time)
  if (!Number.isFinite(date.getTime())) return '확인 불가'
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone: 'Asia/Seoul' }).format(date)
}
export function observationSummary(time: string | null): string {
  if (!time) return '현장 관측 시각이 제공되지 않았습니다. 현재 상태는 출발 전 확인해 주세요.'
  const formatted = formatTimestamp(time)
  if (formatted === '확인 불가') return '현장 관측 시각을 해석할 수 없습니다. 현재 상태는 출발 전 확인해 주세요.'
  return `현장 관측: ${formatted}`
}
export function formatSourceStatusChangedAt(raw: string | null): string | null {
  const value = raw?.trim()
  if (!value) return null
  const unparsed = '변경 시각 확인 필요 · 시간대 미확인'
  if (!/^\d{14}$/.test(value)) return unparsed
  const year = Number(value.slice(0, 4))
  const month = Number(value.slice(4, 6))
  const day = Number(value.slice(6, 8))
  const hour = Number(value.slice(8, 10))
  const minute = Number(value.slice(10, 12))
  const second = Number(value.slice(12, 14))
  if (year < 1000) return unparsed
  const calendarDate = new Date(Date.UTC(year, month - 1, day, hour, minute, second))
  if (calendarDate.getUTCFullYear() !== year || calendarDate.getUTCMonth() !== month - 1
    || calendarDate.getUTCDate() !== day || calendarDate.getUTCHours() !== hour
    || calendarDate.getUTCMinutes() !== minute || calendarDate.getUTCSeconds() !== second) return unparsed
  return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)} ${value.slice(8, 10)}:${value.slice(10, 12)}:${value.slice(12, 14)} · 시간대 미확인`
}

const connectorLabels = new Map<string, string>([
  ['DC_COMBO', 'DC 콤보'], ['DC_CHADEMO', 'CHAdeMO'], ['AC_SLOW', 'AC 완속'], ['AC_THREE_PHASE', 'AC 3상'], ['NACS', 'NACS'],
])
export function connectorLabel(connector: string): string { return connectorLabels.get(connector) ?? '규격 확인 필요' }
