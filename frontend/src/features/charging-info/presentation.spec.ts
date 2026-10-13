import { describe, expect, it } from 'vitest'
import { accessConditionLabel, formatSourceStatusChangedAt, observationSummary, statusLabel, freshnessLabel, reasonLabel, formatDistance, formatTimestamp } from './presentation'

describe('서버 판단을 유지하는 표시', () => {
  it.each([
    ['Y', '입주자만 이용', '입주자만 이용 · 방문 전 운영 주체에 확인해 주세요'],
    ['Y', '시설 상황에 따라 이용이 제한될 수 있음', '시설 상황에 따라 이용이 제한될 수 있음 · 방문 전 운영 주체에 확인해 주세요'],
    ['Y', '  ', '이용 제한이 보고됐습니다. 방문 전 운영 주체에 확인해 주세요'],
    ['N', null, '별도 이용 제한이 보고되지 않았습니다. 현장 안내를 확인해 주세요'],
    ['N', '방문객은 안내 데스크 문의', '방문객은 안내 데스크 문의 · 방문 전 운영 주체에 확인해 주세요'],
    [null, null, '이용 조건 정보가 없습니다. 방문 전 운영 주체에 확인해 주세요'],
  ])('이용 제한 %s와 설명 %s를 행동 가능한 안내로 표시한다', (limitYn, detail, expected) => {
    expect(accessConditionLabel(limitYn, detail)).toBe(expected)
  })
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
  it('관측 시각 누락과 깨진 값을 사용자 안내로 구분한다', () => {
    expect(observationSummary(null)).toBe('현장 관측 시각이 제공되지 않았습니다. 현재 상태는 출발 전 확인해 주세요.')
    expect(observationSummary('broken-time')).toBe('현장 관측 시각을 해석할 수 없습니다. 현재 상태는 출발 전 확인해 주세요.')
  })
  it.each([
    ['20260920161911', '2026-09-20 16:19:11 · 시간대 미확인'],
    ['20240229010203', '2024-02-29 01:02:03 · 시간대 미확인'],
    ['20260229010203', '원본 시각: 20260229010203 · 형식 해석 불가, 시간대 미확인'],
    ['broken', '원본 시각: broken · 형식 해석 불가, 시간대 미확인'],
  ])('공급자 상태 변경 시각 %s는 관측/시간대를 추정하지 않고 %s로 표시한다', (raw, expected) => {
    expect(formatSourceStatusChangedAt(raw)).toBe(expected)
  })
  it('빈 공급자 상태 변경 시각은 표시하지 않는다', () => {
    expect(formatSourceStatusChangedAt('  ')).toBeNull()
    expect(formatSourceStatusChangedAt(null)).toBeNull()
  })
  it('유효한 UTC 시각은 로컬 시간대 이름을 포함하여 표시한다', () => {
    const formatted = formatTimestamp('2026-10-05T00:00:00Z')
    expect(formatted).toContain('2026')
    expect(formatted).toMatch(/UTC|GMT|KST/)
    expect(formatted).not.toContain('T00:00:00Z')
  })
})
