import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ChargerList from './ChargerList.vue'
import FreshnessBadge from '../../charging-info/components/FreshnessBadge.vue'
import detail from '../../../../tests/fixtures/detail.json'

const charger = detail.chargers[0]
if (!charger) throw new Error('충전기 응답 필요')
describe('충전기 목록의 근거 표현', () => {
  it('여러 충전기의 ID·서로 다른 상태·최신성을 모두 표시한다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [charger,
      { ...charger, chargerId: '02', status: 'OCCUPIED', freshness: 'STALE', reasonCode: 'MAX_AGE_EXCEEDED' }] } })
    expect(wrapper.findAll('h3').map(heading => heading.text())).toEqual(['충전기 01', '충전기 02'])
    expect(wrapper.findAll('.status-badge').map(badge => badge.text())).toEqual(['이용 가능 보고', '사용 중'])
    expect(wrapper.findAllComponents(FreshnessBadge).map(badge => badge.text())).toEqual(['최신성 확인 불가', '오래된 정보'])
    expect(wrapper.text()).toContain('서버의 최신성 기준을 지난 정보')
  })
  it('빈 목록은 정보 부재를 알린다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [] } })
    expect(wrapper.get('[role="status"]').text()).toContain('충전기 정보가 없습니다')
    expect(wrapper.findAll('li')).toHaveLength(0)
  })
  it('빈 운영 정보도 확인 필요로 표시하고 이용 제한을 읽을 수 있게 풀어 쓴다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [{ ...charger, useTime: '', limitYn: 'Y', limitDetail: '입주자만 이용' }] } })
    expect(wrapper.text()).toContain('운영 시간: 이용 조건 확인 필요')
    expect(wrapper.text()).toContain('이용 제한: 제한 있음 (Y)')
    expect(wrapper.text()).toContain('입주자만 이용')
  })
})
