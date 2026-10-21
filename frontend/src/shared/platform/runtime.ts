import { Capacitor } from '@capacitor/core'

export const appPlatform = Capacitor.getPlatform()

export function initializePlatform(): void {
  document.documentElement.dataset.platform = appPlatform === 'web' ? 'web' : 'native'
}
