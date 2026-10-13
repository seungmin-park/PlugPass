import { describe, expect, it } from 'vitest'
import { parseSearchCriteria, toSearchQuery } from './criteria'

describe('URL 검색 조건', () => {
  const query = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO' }
  it('조건이 없으면 초기 상태다', () => {
    expect(parseSearchCriteria({})).toEqual({ kind: 'empty' })
    expect(parseSearchCriteria({ unrelated: 'ok' })).toEqual({ kind: 'empty' })
  })
  it('일반 조건을 숫자로 변환하고 limit 기본값20을 적용한다', () => {
    expect(parseSearchCriteria(query)).toEqual({ kind: 'valid', criteria: {
      latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO', limit: 20,
    } })
  })
  it.each([
    ['latitude', null], ['longitude', undefined], ['radiusMeters', ''], ['connector', ''],
    ['latitude', ['37.5', '37.5']], ['longitude', ['127']], ['radiusMeters', ['1000', '3000']],
    ['connector', ['DC_COMBO']], ['limit', ['20', '20']],
    ['latitude', '-90.001'], ['latitude', '90.001'], ['longitude', '-180.001'], ['longitude', '180.001'],
    ['latitude', 'NaN'], ['longitude', 'Infinity'], ['latitude', '0x20'], ['longitude', '1e999'],
    ['radiusMeters', '99'], ['radiusMeters', '10001'], ['radiusMeters', '100.5'], ['radiusMeters', '100.0'],
    ['limit', '0'], ['limit', '51'], ['limit', '1.5'], ['connector', 'UNKNOWN'],
  ])('%s=%j는 해당 필드 오류다', (field, value) => {
    const result = parseSearchCriteria({ ...query, [field]: value })
    expect(result.kind).toBe('invalid')
    expect(result).toMatchObject({ fields: { [field]: expect.any(String) } })
  })
  it('부분 조건이면 빠진 필드를 모두 안내한다', () => {
    expect(parseSearchCriteria({ latitude: '37.5' })).toMatchObject({
      kind: 'invalid', fields: { longitude: expect.any(String), radiusMeters: expect.any(String), connector: expect.any(String) },
    })
  })
  it.each([
    ['-90', '-180', '100', '1'], ['90', '180', '10000', '50'], ['0', '0', '1000', '20'],
  ])('위경도·반경·limit 경계 %s/%s/%s/%s를 허용한다', (latitude, longitude, radiusMeters, limit) => {
    expect(parseSearchCriteria({ ...query, latitude, longitude, radiusMeters, limit })).toEqual({
      kind: 'valid', criteria: { latitude: Number(latitude), longitude: Number(longitude), radiusMeters: Number(radiusMeters), connector: 'DC_COMBO', limit: Number(limit) },
    })
  })
  it.each(['DC_CHADEMO', 'AC_SLOW', 'AC_THREE_PHASE', 'DC_COMBO', 'NACS'])('%s 커넥터를 허용한다', connector => {
    expect(parseSearchCriteria({ ...query, connector })).toMatchObject({ kind: 'valid', criteria: { connector } })
  })
  it('차량에서 고른 여러 규격을 URL로 왕복하고 기존 단일 규격은 보존한다', () => {
    const multi = { latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO' as const,
      connectors: ['DC_COMBO', 'AC_SLOW'] as const, vehicleId: 'tesla-kr', adapterIds: ['tesla-ccs1', 'tesla-j1772'],
      vehicleSupportConfirmed: true, limit: 20 }
    const encoded = toSearchQuery(multi)
    expect(encoded).toMatchObject({ connectors: 'DC_COMBO,AC_SLOW', vehicleId: 'tesla-kr', adapterIds: 'tesla-ccs1,tesla-j1772', vehicleSupportConfirmed: '1' })
    expect(encoded).not.toHaveProperty('connector')
    expect(parseSearchCriteria(encoded)).toEqual({ kind: 'valid', criteria: { ...multi, connectors: [...multi.connectors] } })
    expect(toSearchQuery({ latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO', limit: 20 })).toHaveProperty('connector', 'DC_COMBO')
  })
  it('어댑터가 없는 확인된 차량 URL도 왕복한다', () => {
    const criteria = { latitude: 37.5, longitude: 127, radiusMeters: 1000, connector: 'DC_COMBO' as const,
      connectors: ['DC_COMBO'] as const, vehicleId: 'kona-electric-2020-kr', adapterIds: [] as const, limit: 20 }
    expect(parseSearchCriteria(toSearchQuery(criteria))).toEqual({ kind: 'valid', criteria: { ...criteria, connectors: ['DC_COMBO'], adapterIds: [] } })
  })
  it('빈·중복·미지원 규격과 단일/복수 혼합 URL을 거부한다', () => {
    for (const connectors of ['', 'DC_COMBO,DC_COMBO', 'DC_COMBO,UNKNOWN']) {
      expect(parseSearchCriteria({ ...query, connector: undefined, connectors })).toMatchObject({ kind: 'invalid', fields: { connectors: expect.any(String) } })
    }
    expect(parseSearchCriteria({ ...query, connectors: 'DC_COMBO,AC_SLOW' })).toMatchObject({ kind: 'invalid', fields: { connectors: expect.any(String) } })
  })
})
