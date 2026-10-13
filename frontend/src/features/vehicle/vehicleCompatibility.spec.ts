import { describe, expect, it } from 'vitest'
import { resolveVehicleCompatibility } from './vehicleCompatibility'

describe('차량과 이번에 사용할 어댑터', () => {
  it('국내 2020 코나 일렉트릭은 공식 카탈로그에서 확인한 직접 규격만 검색한다', () => {
    expect(resolveVehicleCompatibility({ vehicleId: 'kona-electric-2020-kr', adapterIds: [] })).toEqual({
      direct: ['DC_COMBO'], viaAdapter: [], connectors: ['DC_COMBO'], requiresConfirmation: false,
    })
  })
  it('어댑터 보유를 차량 선택만으로 추정하지 않는다', () => {
    expect(resolveVehicleCompatibility({ vehicleId: 'tesla-kr', adapterIds: [] })).toEqual({
      direct: [], viaAdapter: [], connectors: [], requiresConfirmation: true,
    })
  })
  it('Tesla 어댑터는 이번 사용과 차량 지원 확인을 모두 선택해야 검색에 포함한다', () => {
    expect(resolveVehicleCompatibility({ vehicleId: 'tesla-kr', adapterIds: ['tesla-ccs1'], vehicleSupportConfirmed: true })).toEqual({
      direct: [], viaAdapter: ['DC_COMBO'], connectors: ['DC_COMBO'], requiresConfirmation: false,
    })
    expect(resolveVehicleCompatibility({ vehicleId: 'tesla-kr', adapterIds: ['tesla-ccs1'] })).toMatchObject({
      connectors: [], requiresConfirmation: true,
    })
  })
  it('지원하지 않는 차량·어댑터 조합과 오래된 카탈로그 ID는 검색에 섞지 않는다', () => {
    expect(resolveVehicleCompatibility({ vehicleId: 'kona-electric-2020-kr', adapterIds: ['tesla-ccs1'] })).toMatchObject({
      connectors: ['DC_COMBO'], viaAdapter: [], requiresConfirmation: true,
    })
    expect(resolveVehicleCompatibility({ vehicleId: 'unknown', adapterIds: [] })).toMatchObject({
      connectors: [], requiresConfirmation: true,
    })
  })
})
