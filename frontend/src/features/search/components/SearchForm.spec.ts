import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import SearchForm from './SearchForm.vue'

describe('검색 조건 폼', () => {
  it('반경·커넥터 선택 그룹과 검색 버튼을 제공한다', () => {
    const wrapper = mount(SearchForm)
    expect(wrapper.findAll('legend').map(legend => legend.text())).toEqual(['검색 반경', '차량 커넥터'])
    expect(wrapper.find('button[type="submit"]').text()).toBe('주변 충전소 검색')
    wrapper.unmount()
  })
  it('기본 반경1000m와 DC 콤보를 선택한다', () => {
    const wrapper = mount(SearchForm)
    expect(wrapper.findAll<HTMLInputElement>('input:checked').map(input => input.element.value)).toEqual(['1000', 'DC_COMBO'])
    wrapper.unmount()
  })
  it('변경한 조건은 검색 버튼을 누를 때만 전달한다', async () => {
    const wrapper = mount(SearchForm)
    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(10)
    await wrapper.get('input[name="radiusMeters"][value="3000"]').setValue()
    await wrapper.get('input[name="connector"][value="NACS"]').setValue()
    expect(wrapper.emitted('submit')).toBeUndefined()
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toEqual([[{ radiusMeters: 3000, connector: 'NACS' }]])
    wrapper.unmount()
  })
  it('검색 반경은 500m부터 10km까지 선택할 수 있다', async () => {
    const wrapper = mount(SearchForm)
    expect(wrapper.findAll<HTMLInputElement>('input[name="radiusMeters"]').map(input => input.element.value))
      .toEqual(['500', '1000', '3000', '5000', '10000'])
    await wrapper.get('input[name="radiusMeters"][value="10000"]').setValue()
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('submit')).toEqual([[{ radiusMeters: 10000, connector: 'DC_COMBO' }]])
    wrapper.unmount()
  })
  it('모든 차량 커넥터를 라벨이 연결된 선택지로 제공한다', () => {
    const wrapper = mount(SearchForm)
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
