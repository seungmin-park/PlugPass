export type NavigationApp = 'kakao' | 'naver'
const key = 'plugpass.navigation-app'
export function readNavigationPreference(): NavigationApp | null {
  try {
    const app = localStorage.getItem(key)
    return app === 'kakao' || app === 'naver' ? app : null
  } catch { return null }
}
export function saveNavigationPreference(app: NavigationApp): void {
  try { localStorage.setItem(key, app) } catch { /* 저장 실패에도 현재 길찾기는 계속한다. */ }
}
