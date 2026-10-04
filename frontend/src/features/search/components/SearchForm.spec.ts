import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import SearchForm from './SearchForm.vue'

describe('검색 조건 폼', () => {
  it('반경·커넥터 라벨과 검색 버튼을 제공한다', () => {
    const wrapper = mount(SearchForm)
    expect(wrapper.findAll('label').map(label => label.text())).toEqual(['검색 반경', '차량 커넥터'])
    expect(wrapper.find('button[type="submit"]').text()).toBe('주변 충전소 검색')
    wrapper.unmount()
  })
  it('기본 반경1000m와 DC 콤보를 선택한다', () => {
    const wrapper = mount(SearchForm)
    expect(wrapper.findAll('select').map(select => select.element.value)).toEqual(['1000', 'DC_COMBO'])
    wrapper.unmount()
  })
  it('변경한 조건은 검색 버튼을 누를 때만 전달한다', async () => {
    const wrapper = mount(SearchForm)
    await wrapper.find('select#radius').setValue('3000')
    await wrapper.find('select#connector').setValue('NACS')
    expect(wrapper.emitted('submit')).toBeUndefined()
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toEqual([[{ radiusMeters: 3000, connector: 'NACS' }]])
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
