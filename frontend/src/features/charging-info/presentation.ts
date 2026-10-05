const statusLabels = new Map<string, string>([
  ['AVAILABLE', '이용 가능 보고'], ['OCCUPIED', '사용 중'], ['UNAVAILABLE', '이용 불가'], ['UNKNOWN', '상태 확인 불가'],
])
const freshnessLabels = new Map<string, string>([
  ['RECENT', '최근 관측'], ['STALE', '오래된 정보'], ['UNVERIFIED', '최신성 확인 불가'],
])
export function statusLabel(status: string): string { return statusLabels.get(status) ?? '상태 확인 불가' }
export function freshnessLabel(freshness: string): string { return freshnessLabels.get(freshness) ?? '최신성 확인 불가' }
export function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)}m`
  return `${new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 1 }).format(meters / 1000)}km`
}
export function formatTimestamp(time: string | null): string {
  if (!time) return '확인 불가'
  const date = new Date(time)
  if (!Number.isFinite(date.getTime())) return '확인 불가'
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', timeZoneName: 'short' }).format(date)
}
