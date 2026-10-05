import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusBadge from './StatusBadge.vue'
import FreshnessBadge from './FreshnessBadge.vue'

describe('상태와 최신성을 별도로 읽는 배지', () => {
  it('이용 가능 보고와 최신성 확인 불가를 독립적인 문구로 표시한다', () => {
    const status = mount(StatusBadge, { props: { status: 'AVAILABLE' } })
    const freshness = mount(FreshnessBadge, { props: { freshness: 'UNVERIFIED' } })
    expect(status.text()).toBe('이용 가능 보고')
    expect(freshness.text()).toBe('최신성 확인 불가')
  })
  it('새 코드도 색뿐 아니라 보수적인 텍스트를 제공한다', () => {
    const status = mount(StatusBadge, { props: { status: 'FUTURE' } })
    const freshness = mount(FreshnessBadge, { props: { freshness: 'FUTURE' } })
    expect(status.text()).toBe('상태 확인 불가')
    expect(freshness.text()).toBe('최신성 확인 불가')
  })
})
