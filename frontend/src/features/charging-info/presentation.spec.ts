import { describe, expect, it } from 'vitest'
import { statusLabel, freshnessLabel, reasonLabel, formatDistance, formatTimestamp } from './presentation'

describe('서버 판단을 유지하는 표시', () => {
  it.each([
    ['RECENT_AVAILABLE', '최근 관측에서 이용 가능으로 보고되었습니다'],
    ['UNVERIFIED_AVAILABLE', '이용 가능 보고가 있으나 최신성을 확인할 수 없습니다'],
    ['INCOMPATIBLE_CONNECTOR', '선택한 차량 커넥터와 호환되지 않습니다'],
    ['DELETED_BY_PROVIDER', '공급자가 삭제한 충전기입니다'],
    ['ACCESS_RESTRICTED', '이용 제한이 보고되었습니다'],
    ['STATUS_UNKNOWN', '충전기 상태를 확인할 수 없습니다'],
    ['NOT_AVAILABLE', '이용 가능 상태로 보고되지 않았습니다'],
    ['NO_COMPATIBLE_CHARGER', '호환 충전기 정보가 없습니다'],
    ['ACCESS_CONDITIONS_UNVERIFIED', '출입 조건 확인이 필요합니다'],
    ['OPERATING_HOURS_UNVERIFIED', '운영 시간을 확인할 수 없습니다'],
    ['OPERATING_HOURS_REQUIRE_CHECK', '운영 시간을 확인해 주세요'],
    ['NEW_REASON', '상세 확인이 필요한 사유'],
    ['constructor', '상세 확인이 필요한 사유'],
  ])('사유 %s는 서버 규칙을 재계산하지 않고 설명한다', (reason, expected) => {
    expect(reasonLabel(reason)).toBe(expected)
  })
  it.each([['AVAILABLE', '이용 가능 보고'], ['OCCUPIED', '사용 중'], ['UNAVAILABLE', '이용 불가'], ['UNKNOWN', '상태 확인 불가'], ['NEW_STATUS', '상태 확인 불가'], ['constructor', '상태 확인 불가'], ['toString', '상태 확인 불가']])('상태 %s를 %s로 표시한다', (status, label) => {
    expect(statusLabel(status)).toBe(label)
  })
  it.each([['RECENT', '최근 관측'], ['STALE', '오래된 정보'], ['UNVERIFIED', '최신성 확인 불가'], ['NEW_FRESHNESS', '최신성 확인 불가'], ['__proto__', '최신성 확인 불가']])('최신성 %s를 %s로 표시한다', (freshness, label) => {
    expect(freshnessLabel(freshness)).toBe(label)
  })
  it.each([[0, '0m'], [88.2169, '88m'], [1000, '1km'], [1500, '1.5km']])('직선거리 %s미터를 %s로 표시한다', (meters, label) => {
    expect(formatDistance(meters)).toBe(label)
  })
  it.each([null, '', 'broken-time'])('누락·깨진 시각 %s는 확인 불가다', time => {
    expect(formatTimestamp(time)).toBe('확인 불가')
  })
  it('유효한 UTC 시각은 로컬 시간대 이름을 포함하여 표시한다', () => {
    const formatted = formatTimestamp('2026-10-05T00:00:00Z')
    expect(formatted).toContain('2026')
    expect(formatted).toMatch(/UTC|GMT|KST/)
    expect(formatted).not.toContain('T00:00:00Z')
  })
})
