import { isNonblankString, isNumberInRange } from '../../../shared/api/responseValidation'

export interface NavigationDestination {
  name: string
  latitude: number
  longitude: number
}

export type NavigationPlatform = 'android' | 'ios' | 'web'

export function navigationPlatform(userAgent: string, maxTouchPoints: number): NavigationPlatform {
  if (/android/i.test(userAgent)) return 'android'
  if (/iPhone|iPad|iPod/i.test(userAgent) || (/Macintosh/i.test(userAgent) && maxTouchPoints > 1)) return 'ios'
  return 'web'
}

export function createNavigationLinks(destination: NavigationDestination, platform: NavigationPlatform) {
  const { name, latitude, longitude } = destination
  if (!isNonblankString(name) || !isNumberInRange(latitude, -90, 90) || !isNumberInRange(longitude, -180, 180)) return null
  const webHref = `https://map.kakao.com/link/to/${encodeURIComponent(name)},${latitude},${longitude}`
  if (platform === 'web') return { webHref, directHref: webHref, installHref: undefined }

  const installHref = platform === 'android'
    ? 'https://play.google.com/store/apps/details?id=net.daum.android.map'
    : 'https://apps.apple.com/app/id304608425'
  const directHref = platform === 'android'
    ? `intent://0,0?q=${encodeURIComponent(`${latitude},${longitude}(${name})`)}#Intent;scheme=geo;action=android.intent.action.VIEW;S.browser_fallback_url=${encodeURIComponent(installHref)};end`
    : `kakaomap://route?ep=${latitude},${longitude}&by=car`
  return { webHref, directHref, installHref }
}
