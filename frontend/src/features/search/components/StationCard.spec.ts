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
  it('일반 좌클릭은 기본 이동을 막고 상세 선택을 한 번 전달한다', () => {
    const wrapper = mount(StationCard, { props: { station, detailHref: '#/stations/1?radiusMeters=1000' } })
    const click = new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 })
    wrapper.get('a').element.dispatchEvent(click)
    expect(click.defaultPrevented).toBe(true)
    expect(wrapper.emitted('openDetail')).toEqual([[]])
    expect(wrapper.get('a').attributes('href')).toBe('#/stations/1?radiusMeters=1000')
  })
  it.each([
    { label: 'Ctrl', input: { ctrlKey: true } },
    { label: 'Cmd', input: { metaKey: true } },
    { label: 'Shift', input: { shiftKey: true } },
    { label: 'Alt', input: { altKey: true } },
    { label: '가운데 버튼', input: { button: 1 } },
  ])('$label 클릭은 브라우저의 기본 링크 동작을 유지한다', ({ input }) => {
    const wrapper = mount(StationCard, { props: { station, detailHref: '#/stations/1?radiusMeters=1000' } })
    const observedDefaultPrevention: boolean[] = []
    const link = wrapper.get('a')
    link.element.addEventListener('click', event => {
      observedDefaultPrevention.push(event.defaultPrevented)
      // 컴포넌트 처리 결과를 기록한 뒤 테스트 DOM의 실제 이동만 막는다.
      event.preventDefault()
    }, { once: true })
    link.element.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, ...input }))
    expect(observedDefaultPrevention).toEqual([false])
    expect(wrapper.emitted('openDetail')).toBeUndefined()
    expect(link.attributes('href')).toBe('#/stations/1?radiusMeters=1000')
  })
})
