import { describe, expect, it, vi } from 'vitest'
import { clearVehicleProfile, readVehicleProfile, saveVehicleProfile } from './vehicleProfile'

describe('내 차량 저장', () => {
  it('선택한 차량과 이번 이용 어댑터만 저장하고 다시 읽는다', () => {
    const storage = new Map<string, string>()
    const target = { getItem: (key: string) => storage.get(key) ?? null, setItem: (key: string, value: string) => { storage.set(key, value) } }
    const profile = { vehicleId: 'tesla-kr', adapterIds: ['tesla-ccs1'], vehicleSupportConfirmed: true }
    expect(saveVehicleProfile(target, profile)).toBe(true)
    expect(readVehicleProfile(target)).toEqual(profile)
  })
  it('오래된 차량·어댑터 ID와 깨진 저장값은 사용하지 않는다', () => {
    expect(readVehicleProfile({ getItem: () => '{broken' })).toBeNull()
    expect(readVehicleProfile({ getItem: () => JSON.stringify({ vehicleId: 'retired', adapterIds: [] }) })).toBeNull()
    expect(readVehicleProfile({ getItem: () => JSON.stringify({ vehicleId: 'tesla-kr', adapterIds: ['retired'] }) })).toBeNull()
  })
  it('저장소 접근 실패는 예외를 던지지 않고 새 입력을 계속 허용한다', () => {
    const blocked = { getItem: vi.fn(() => { throw new Error('blocked') }), setItem: vi.fn(() => { throw new Error('blocked') }) }
    expect(readVehicleProfile(blocked)).toBeNull()
    expect(saveVehicleProfile(blocked, { vehicleId: 'kona-electric-2020-kr', adapterIds: [] })).toBe(false)
  })
  it('직접 입력으로 바꾼 뒤 저장된 다른 차량을 다시 기본값으로 사용하지 않는다', () => {
    const storage = new Map<string, string>()
    const target = { getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => { storage.set(key, value) }, deleteItem: (key: string) => { storage.delete(key) } }
    saveVehicleProfile(target, { vehicleId: 'kona-electric-2020-kr', adapterIds: [] })
    expect(clearVehicleProfile({ removeItem: target.deleteItem })).toBe(true)
    expect(readVehicleProfile(target)).toBeNull()
  })
})
