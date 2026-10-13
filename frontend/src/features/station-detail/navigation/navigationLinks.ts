import { isNonblankString, isNumberInRange } from '../../../shared/api/responseValidation'

export interface NavigationDestination {
  name: string
  latitude: number
  longitude: number
}

export type NavigationPlatform = 'android' | 'ios' | 'web'

export interface NavigationAppLink {
  id: 'kakao' | 'naver'
  name: string
  href: string
  installHref: string
}

export function navigationPlatform(userAgent: string, maxTouchPoints: number): NavigationPlatform {
  if (/android/i.test(userAgent)) return 'android'
  if (/iPhone|iPad|iPod/i.test(userAgent) || (/Macintosh/i.test(userAgent) && maxTouchPoints > 1)) return 'ios'
  return 'web'
}

export function createNavigationLinks(destination: NavigationDestination, platform: NavigationPlatform, pageUrl: string) {
  const { name, latitude, longitude } = destination
  if (!isNonblankString(name) || !isNumberInRange(latitude, -90, 90) || !isNumberInRange(longitude, -180, 180)) return null
  const webHref = `https://map.kakao.com/link/to/${encodeURIComponent(name)},${latitude},${longitude}`
  const apps: NavigationAppLink[] = []
  if (platform === 'web') return { webHref, apps }

  const page = new URL(pageUrl)
  const appname = encodeURIComponent(page.origin + page.pathname)
  const kakaoRoute = `route?ep=${latitude},${longitude}&by=car`
  const naverRoute = `route/car?dlat=${latitude}&dlng=${longitude}&dname=${encodeURIComponent(name)}&appname=${appname}`
  apps.push(appLink('kakao', '카카오맵', kakaoRoute, 'kakaomap', 'net.daum.android.map', '304608425', platform))
  if (isNumberInRange(latitude, 31.43, 44.35) && isNumberInRange(longitude, 122.37, 132)) {
    apps.push(appLink('naver', '네이버 지도', naverRoute, 'nmap', 'com.nhn.android.nmap', '311867728', platform))
  }
  return { webHref, apps }
}

function appLink(id: NavigationAppLink['id'], name: string, route: string, scheme: string,
  androidPackage: string, appleId: string, platform: 'android' | 'ios'): NavigationAppLink {
  const installHref = platform === 'android'
    ? `https://play.google.com/store/apps/details?id=${androidPackage}`
    : `https://apps.apple.com/app/id${appleId}`
  const href = platform === 'android'
    ? `intent://${route}#Intent;scheme=${scheme};package=${androidPackage};S.browser_fallback_url=${encodeURIComponent(installHref)};end`
    : `${scheme}://${route}`
  return { id, name, href, installHref }
}
