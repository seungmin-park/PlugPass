import type { Connector } from '../charging-info/types'
import { adapterCatalog, vehicleCatalog } from './vehicleCatalog'

export interface VehicleProfile {
  vehicleId: string
  adapterIds: string[]
  vehicleSupportConfirmed?: boolean
}

export interface VehicleCompatibility {
  direct: Connector[]
  viaAdapter: Connector[]
  connectors: Connector[]
  requiresConfirmation: boolean
}

export function resolveVehicleCompatibility(profile: VehicleProfile): VehicleCompatibility {
  const vehicle = vehicleCatalog.find(item => item.id === profile.vehicleId)
  if (!vehicle) return { direct: [], viaAdapter: [], connectors: [], requiresConfirmation: true }
  const unsupportedAdapter = profile.adapterIds.some(id => !adapterCatalog.some(adapter => adapter.id === id && adapter.vehicleId === vehicle.id))
  const direct = [...vehicle.directConnectors]
  const viaAdapter = profile.vehicleSupportConfirmed
    ? adapterCatalog.filter(adapter => adapter.vehicleId === vehicle.id && profile.adapterIds.includes(adapter.id)).map(adapter => adapter.connector)
    : []
  const connectors = [...new Set([...direct, ...viaAdapter])]
  return { direct, viaAdapter, connectors, requiresConfirmation: unsupportedAdapter || connectors.length === 0 }
}
