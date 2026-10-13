import type { VehicleProfile } from './vehicleCompatibility'
import { adapterCatalog, vehicleCatalog } from './vehicleCatalog'

const storageKey = 'plugpass.vehicle-profile.v1'

function isVehicleProfile(value: unknown): value is VehicleProfile {
  if (!value || typeof value !== 'object') return false
  const profile = value as Record<string, unknown>
  if (typeof profile.vehicleId !== 'string' || !vehicleCatalog.some(vehicle => vehicle.id === profile.vehicleId)
    || !Array.isArray(profile.adapterIds) || new Set(profile.adapterIds).size !== profile.adapterIds.length
    || !profile.adapterIds.every(id => typeof id === 'string'
      && adapterCatalog.some(adapter => adapter.id === id && adapter.vehicleId === profile.vehicleId))) return false
  return profile.vehicleSupportConfirmed === undefined || typeof profile.vehicleSupportConfirmed === 'boolean'
}

export function readVehicleProfile(storage: Pick<Storage, 'getItem'>): VehicleProfile | null {
  try {
    const value = storage.getItem(storageKey)
    if (!value) return null
    const parsed: unknown = JSON.parse(value)
    return isVehicleProfile(parsed) ? parsed : null
  } catch { return null }
}

export function saveVehicleProfile(storage: Pick<Storage, 'setItem'>, profile: VehicleProfile): boolean {
  if (!isVehicleProfile(profile)) return false
  try {
    storage.setItem(storageKey, JSON.stringify(profile))
    return true
  } catch { return false }
}

export function clearVehicleProfile(storage: Pick<Storage, 'removeItem'>): boolean {
  try { storage.removeItem(storageKey); return true } catch { return false }
}
