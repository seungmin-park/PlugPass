import { describe, expect, it } from 'vitest'
import { parseStationId, isStationId } from './stationId'

describe('충전소 식별자', () => {
  it.each(['1', '9007199254740991'])('양의 안전 정수 %s를 변환한다', value => {
    expect(parseStationId(value)).toBe(Number(value))
    expect(isStationId(Number(value))).toBe(true)
  })
  it.each([undefined, null, '', '0', '-1', '1.5', '1e3', 'Infinity', '9007199254740992', ['1'], ' 1 '])('%j는 URL ID로 거부한다', value => {
    expect(parseStationId(value)).toBeNull()
  })
  it.each([0, -1, 1.5, NaN, Infinity, Number.MAX_SAFE_INTEGER + 1, '1'])('%j는 API ID로 거부한다', value => {
    expect(isStationId(value)).toBe(false)
  })
})
