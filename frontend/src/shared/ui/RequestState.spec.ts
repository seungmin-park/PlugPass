import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/apiError'
import RequestState from './RequestState.vue'

describe('조회 상태 안내', () => {
  it('로딩 안내를 읽을 수 있는 상태 영역으로 제공한다', () => {
    const wrapper = mount(RequestState, { props: { status: 'loading' } })
    expect(wrapper.get('[role="status"]').text()).toContain('주변 충전소를 조회하고 있습니다')
  })
  it('validation 필드 메시지를 보여 주고 재시도 이벤트를 전달한다', async () => {
    const error = new ApiError('validation', '입력 오류', 400, 'INVALID_REQUEST', { radiusMeters: '반경 오류' })
    const wrapper = mount(RequestState, { props: { status: 'error', error } })
    expect(wrapper.get('[role="alert"]').text()).toContain('반경 오류')
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('retry')).toEqual([[]])
  })
  it('조회할 조건이 없으면 재시도 버튼을 제공하지 않는다', () => {
    const wrapper = mount(RequestState, { props: { status: 'error', error: new ApiError('validation', '조건 오류'), retryable: false } })
    expect(wrapper.find('button').exists()).toBe(false)
    expect(wrapper.text()).toContain('검색 조건을 확인해 주세요')
  })
})
