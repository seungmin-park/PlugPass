import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CandidateGroup from './CandidateGroup.vue'

const searchQuery = { latitude: '37.5', longitude: '127', radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' }
describe('후보 그룹 표현과 선택', () => {
  it('서버 순서·이유·텍스트 이름을 유지하고 상세 선택을 전달한다', async () => {
    const wrapper = mount(CandidateGroup, { props: { title: '우선 후보', searchQuery, candidates: [
      { id: 8, name: '<b>첫 후보</b>', distanceMeters: 1000, reasonCodes: ['RECENT_AVAILABLE', 'NEW_REASON'] },
      { id: 7, name: '둘째 후보', distanceMeters: 10, reasonCodes: [] },
    ] } })
    expect(wrapper.findAll('h3').map(heading => heading.text())).toEqual(['<b>첫 후보</b>', '둘째 후보'])
    expect(wrapper.find('b').exists()).toBe(false)
    expect(wrapper.text()).toContain('직선거리 1km')
    expect(wrapper.findAll('.candidate-reasons li').map(reason => reason.text())).toEqual(['최근 관측에서 이용 가능으로 보고되었습니다', '상세 확인이 필요한 사유'])
    expect(wrapper.get('#candidate-8').attributes('href')).toBe('#/stations/8?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20')
    await wrapper.get('#candidate-8').trigger('click')
    expect(wrapper.emitted('openDetail')).toEqual([[8]])
  })
  it('빈 그룹은 후보 부재를 알린다', () => {
    const wrapper = mount(CandidateGroup, { props: { title: '이용 전 확인 필요', searchQuery, candidates: [] } })
    expect(wrapper.get('[role="status"]').text()).toBe('해당 그룹의 후보가 없습니다.')
    expect(wrapper.findAll('a')).toHaveLength(0)
  })
})
