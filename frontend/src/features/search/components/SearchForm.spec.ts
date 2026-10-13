import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import SearchForm from './SearchForm.vue'

describe('검색 조건 폼', () => {
  it('첫 방문에는 차량 선택을 먼저 요구하며 임의의 차종을 가정하지 않는다', async () => {
    const wrapper = mount(SearchForm)
    expect(wrapper.get<HTMLSelectElement>('#vehicle-choice').element.value).toBe('')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('차량을 먼저 선택해 주세요')
  })
  it('확인된 국내 차량은 커넥터 이름을 몰라도 차량 조건으로 제출한다', async () => {
    const wrapper = mount(SearchForm)
    await wrapper.get('#vehicle-choice').setValue('kona-electric-2020-kr')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('submit')).toEqual([[{ connector: 'DC_COMBO', connectors: ['DC_COMBO'], vehicleId: 'kona-electric-2020-kr', adapterIds: [] }]])
  })
  it('Tesla 어댑터는 이번 사용과 차량 지원 확인을 마쳐야 제출한다', async () => {
    const wrapper = mount(SearchForm)
    await wrapper.get('#vehicle-choice').setValue('tesla-kr')
    await wrapper.get('input[value="tesla-ccs1"]').setValue(true)
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('submit')).toBeUndefined()
    await wrapper.get('#vehicle-support-confirmed').setValue(true)
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('submit')).toEqual([[{ connector: 'DC_COMBO', connectors: ['DC_COMBO'],
      vehicleId: 'tesla-kr', adapterIds: ['tesla-ccs1'], vehicleSupportConfirmed: true }]])
  })
  it('URL에서 복원한 커넥터를 선택하고 제출한다', async () => {
    const wrapper = mount(SearchForm, { props: { initialMode: 'manual', initialConnector: 'NACS' } })
    expect(wrapper.findAll<HTMLInputElement>('input:checked').map(input => input.element.value)).toEqual(['NACS'])
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('submit')).toEqual([[{ connector: 'NACS' }]])
  })
  it('커넥터 선택 그룹과 검색 버튼을 제공한다', () => {
    const wrapper = mount(SearchForm, { props: { initialMode: 'manual' } })
    expect(wrapper.findAll('legend').map(legend => legend.text())).toEqual(['내 차량 충전구 직접 확인'])
    expect(wrapper.find('button[type="submit"]').text()).toBe('주변 충전소 검색')
    wrapper.unmount()
  })
  it('기본 DC 콤보를 선택한다', () => {
    const wrapper = mount(SearchForm, { props: { initialMode: 'manual' } })
    expect(wrapper.findAll<HTMLInputElement>('input:checked').map(input => input.element.value)).toEqual(['DC_COMBO'])
    wrapper.unmount()
  })
  it('변경한 조건은 검색 버튼을 누를 때만 전달한다', async () => {
    const wrapper = mount(SearchForm, { props: { initialMode: 'manual' } })
    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(5)
    await wrapper.get('input[name="connector"][value="NACS"]').setValue()
    expect(wrapper.emitted('submit')).toBeUndefined()
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toEqual([[{ connector: 'NACS' }]])
    wrapper.unmount()
  })
  it('모든 차량 커넥터를 라벨이 연결된 선택지로 제공한다', () => {
    const wrapper = mount(SearchForm, { props: { initialMode: 'manual' } })
    const connectorInputs = wrapper.findAll<HTMLInputElement>('input[name="connector"]')
    expect(connectorInputs.map(input => input.element.value))
      .toEqual(['DC_COMBO', 'DC_CHADEMO', 'AC_SLOW', 'AC_THREE_PHASE', 'NACS'])
    expect(connectorInputs.map(input => input.element.labels?.[0]?.textContent?.trim()))
      .toEqual(['DC 콤보', 'CHAdeMO', 'AC 완속', 'AC 3상', 'NACS'])
    wrapper.unmount()
  })
  it('마운트할 때 위치 권한을 자동 요청하지 않는다', () => {
    const getCurrentPosition = vi.fn()
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } })
    const wrapper = mount(SearchForm)
    expect(getCurrentPosition).not.toHaveBeenCalled()
    wrapper.unmount()
    vi.unstubAllGlobals()
  })
})
