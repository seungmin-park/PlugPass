import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StationCard from './StationCard.vue'
import searchResponse from '../../../../tests/fixtures/search.json'

const station = searchResponse.stations[0]
if (!station) throw new Error('첫 충전소 응답 fixture가 필요합니다')

describe('충전소 검색 카드', () => {
  it('충전소 이름·직선거리·호환 수·보고 수·두 종류 배지를 표시한다', () => {
    const wrapper = mount(StationCard, { props: { station } })
    expect(wrapper.get('h3').text()).toBe('가까운 충전소')
    expect(wrapper.text()).toContain('직선거리 0m')
    expect(wrapper.text()).toContain('호환 충전기 1대')
    expect(wrapper.text()).toContain('이용 가능 보고 1대')
    expect(wrapper.text()).toContain('이용 가능 보고')
    expect(wrapper.text()).toContain('최신성 확인 불가')
    expect(wrapper.text()).not.toContain('충전 가능 확정')
  })
  it('빈 충전기일 때 정보 확인 안내를 제공한다', () => {
    const wrapper = mount(StationCard, { props: { station: { ...station, chargers: [] } } })
    expect(wrapper.text()).toContain('충전기 정보 확인 필요')
  })
})
