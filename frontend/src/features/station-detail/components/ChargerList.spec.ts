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
    expect(wrapper.findAll('.status-badge').map(badge => badge.text())).toEqual(['이용 가능', '사용 중'])
    expect(wrapper.findAllComponents(FreshnessBadge)).toHaveLength(0)
    expect(wrapper.text()).not.toContain('서버의 최신성 기준을 지난 정보')
  })
  it('원본 코드 대신 연결 규격을 보여 주고 원본 상태·삭제 표시를 숨긴다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [{ ...charger, connectors: ['DC_COMBO'], connectorCode: '04', delYn: 'N' }] } })
    expect(wrapper.text()).toContain('DC 콤보')
    expect(wrapper.text()).not.toContain('원본 상태 코드')
    expect(wrapper.text()).not.toContain('원본 커넥터 코드')
    expect(wrapper.text()).not.toContain('공급자 삭제 표시')
    expect(wrapper.text()).not.toContain('최신성 확인 불가')
  })
  it('빈 목록은 정보 부재를 알린다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [] } })
    expect(wrapper.get('[role="status"]').text()).toContain('충전기 정보가 없습니다')
    expect(wrapper.findAll('li')).toHaveLength(0)
  })
  it('운영 조건을 먼저 알리고 기술 코드와 수집 시각은 출처를 열 때만 보인다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [{ ...charger, useTime: '', limitYn: 'Y', limitDetail: '입주자만 이용' }] } })
    const summary = wrapper.get('.charger-summary')
    expect(summary.text()).toContain('운영 시간: 확인 필요')
    expect(summary.text()).toContain('입주자만 이용 · 방문 전 운영 주체에 확인해 주세요')
    expect(summary.text()).not.toContain('현장 관측 시각이 제공되지 않았습니다')
    expect(summary.text()).not.toContain('원본 상태 코드')
    expect(summary.text()).not.toContain('수집 시각')
    expect(wrapper.text()).not.toContain('원본 상태 코드')
    expect(wrapper.get('details.charger-source').text()).toContain('서버 수집 시각')
    expect(wrapper.get('details.charger-source').text()).toContain('현장 관측 시각과 다릅니다')
  })
  it('빈 비고는 숨기고 원본 상태 변경 시각의 시간대를 추정하지 않는다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [{ ...charger, limitYn: 'Y', limitDetail: null, note: '  ', sourceStatusChangedAtRaw: '20260920161911' }] } })
    expect(wrapper.text()).not.toContain('비고:')
    expect(wrapper.get('.charger-summary').text()).toContain('이용 제한이 보고됐습니다. 방문 전 운영 주체에 확인해 주세요')
    expect(wrapper.get('details.charger-source').text()).toContain('2026-09-20 16:19:11 · 시간대 미확인')
    expect(wrapper.get('details.charger-source').text()).toContain('시간대 미확인')
  })
  it('비고와 공급자 삭제 표시는 필요한 사용자 안내로 보여 준다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [{ ...charger, note: '지하 2층', delYn: 'Y' }] } })
    expect(wrapper.get('.charger-summary').text()).toContain('현장 안내: 지하 2층')
    expect(wrapper.get('.charger-summary').text()).toContain('운영 목록에서 제외된 충전기입니다')
  })
  it('깨진 현장 관측 시각과 미래 판정 사유를 확정적인 최신 정보로 표시하지 않는다', () => {
    const wrapper = mount(ChargerList, { props: { chargers: [{ ...charger, sourceObservedAt: 'broken-time', reasonCode: 'SOURCE_OBSERVED_AT_IN_FUTURE' }] } })
    expect(wrapper.get('.charger-summary').text()).toContain('현장 관측 시각을 해석할 수 없습니다')
    expect(wrapper.get('.charger-summary').text()).toContain('관측 시각이 미래여서 확인이 필요합니다')
    expect(wrapper.get('.charger-summary').text()).not.toContain('현장 관측: 확인 불가')
  })
})
