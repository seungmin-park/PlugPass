import type { Connector } from '../charging-info/types'

export interface CatalogVehicle {
  id: string
  label: string
  market: 'KR'
  modelYear: number | null
  directConnectors: readonly Connector[]
  sourceUrl: string
  checkedAt: string
}

export interface CatalogAdapter {
  id: string
  label: string
  connector: Connector
  vehicleId: string
  sourceUrl: string
  caution: string
}

const teslaSource = 'https://www.tesla.com/ko_kr/support/charging/public-charging'

export const vehicleCatalog: readonly CatalogVehicle[] = [
  {
    id: 'kona-electric-2020-kr', label: '현대 코나 일렉트릭 · 2020년 국내형', market: 'KR', modelYear: 2020,
    directConnectors: ['DC_COMBO'],
    sourceUrl: 'https://www.hyundai.com/content/dam/hyundai/kr/ko/data/vehicles/catalog/kona-electric-catalog.pdf',
    checkedAt: '2026-10-13',
  },
  {
    id: 'tesla-kr', label: 'Tesla 국내 차량 · 충전 사양 직접 확인', market: 'KR', modelYear: null,
    directConnectors: [], sourceUrl: teslaSource, checkedAt: '2026-10-13',
  },
]

export const adapterCatalog: readonly CatalogAdapter[] = [
  { id: 'tesla-ccs1', label: 'Tesla CCS 콤보 1 어댑터', connector: 'DC_COMBO', vehicleId: 'tesla-kr',
    sourceUrl: teslaSource, caution: '차량 지원 여부와 충전기 정격을 확인해 주세요. 일부 충전기는 사용할 수 없습니다.' },
  { id: 'tesla-chademo', label: 'Tesla CHAdeMO 어댑터', connector: 'DC_CHADEMO', vehicleId: 'tesla-kr',
    sourceUrl: teslaSource, caution: '제조사·장비 상태에 따라 충전이 실패할 수 있습니다.' },
  { id: 'tesla-j1772', label: 'Tesla J1772 어댑터', connector: 'AC_SLOW', vehicleId: 'tesla-kr',
    sourceUrl: teslaSource, caution: '공공 완속 충전기와 사용 가능 여부를 확인해 주세요.' },
]
