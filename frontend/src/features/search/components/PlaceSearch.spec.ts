import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, expect, it, vi } from 'vitest'
import PlaceSearch from './PlaceSearch.vue'
import { usePlaceSearchStore } from '../stores/placeSearchStore'

afterEach(() => { vi.useRealTimers(); vi.unstubAllEnvs(); vi.restoreAllMocks() })

it('카카오 지도 키가 있으면 두 글자부터 잠시 멈춘 검색어의 후보를 요청한다', async () => {
  vi.stubEnv('VITE_KAKAO_JS_KEY', 'test-js-key')
  vi.useFakeTimers()
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = usePlaceSearchStore()
  const search = vi.spyOn(store, 'search').mockResolvedValue()
  const wrapper = mount(PlaceSearch, { global: { plugins: [pinia] } })
  await wrapper.get('#place-query').setValue('서')
  await vi.advanceTimersByTimeAsync(300)
  expect(search).not.toHaveBeenCalled()
  await wrapper.get('#place-query').setValue('서울')
  await vi.advanceTimersByTimeAsync(299)
  expect(search).not.toHaveBeenCalled()
  await vi.advanceTimersByTimeAsync(1)
  expect(search).toHaveBeenCalledExactlyOnceWith('서울')
  wrapper.unmount()
})
