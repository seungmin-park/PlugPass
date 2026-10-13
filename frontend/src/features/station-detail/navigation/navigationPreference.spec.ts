import { afterEach, expect, it, vi } from 'vitest'
import { readNavigationPreference, saveNavigationPreference } from './navigationPreference'
const key = 'plugpass.navigation-app'
afterEach(() => localStorage.removeItem(key))
it('선택한 지도 앱을 이 기기에서 다시 읽는다', () => {
  saveNavigationPreference('kakao')
  expect(readNavigationPreference()).toBe('kakao')
  saveNavigationPreference('naver')
  expect(readNavigationPreference()).toBe('naver')
})
it('없는 값이나 알 수 없는 앱은 선택하지 않은 것으로 처리한다', () => {
  expect(readNavigationPreference()).toBeNull()
  localStorage.setItem(key, 'unknown')
  expect(readNavigationPreference()).toBeNull()
})
it('저장소 읽기를 거부해도 앱 선택을 막지 않는다', () => {
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('storage denied') })
  expect(readNavigationPreference()).toBeNull()
})
it('저장소 쓰기를 거부해도 길찾기를 막지 않는다', () => {
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('storage denied') })
  expect(() => saveNavigationPreference('kakao')).not.toThrow()
})
