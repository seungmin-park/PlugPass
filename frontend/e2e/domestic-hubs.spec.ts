import { test, expect } from './fixtures'
import searchResponse from '../tests/fixtures/search.json' with { type: 'json' }

const firstStation = searchResponse.stations[0]
if (!firstStation) throw new Error('검증할 충전소 fixture가 필요합니다')

for (const hub of [
  { name: '서울', latitude: 37.5665, longitude: 126.978 },
  { name: '부산', latitude: 35.1796, longitude: 129.0756 },
  { name: '대전', latitude: 36.3504, longitude: 127.3845 },
  { name: '대구', latitude: 35.8714, longitude: 128.6014 },
  { name: '광주', latitude: 35.1601, longitude: 126.8514 },
  { name: '제주', latitude: 33.4996, longitude: 126.5312 },
]) {
  test(`${hub.name} 기준 좌표로 검색하고 같은 충전소를 지도에서 선택한다`, async ({ page }) => {
    const requestedLocations: Array<{ latitude: string | null; longitude: string | null }> = []
    await page.route('**/api/v1/stations?**', route => {
      const params = new URL(route.request().url()).searchParams
      requestedLocations.push({ latitude: params.get('latitude'), longitude: params.get('longitude') })
      return route.fulfill({ json: {
        ...searchResponse,
        stations: [{ ...firstStation, name: `${hub.name} 검증 충전소`, latitude: hub.latitude, longitude: hub.longitude }],
      } })
    })
    const query = new URLSearchParams({ latitude: String(hub.latitude), longitude: String(hub.longitude), radiusMeters: '1000', connector: 'DC_COMBO', limit: '20' })
    await page.goto(`/app/index.html#/stations?${query}`)
    await expect(page.getByRole('link', { name: `${hub.name} 검증 충전소 상세 보기` })).toBeVisible()
    expect(requestedLocations).toEqual([{ latitude: String(hub.latitude), longitude: String(hub.longitude) }])
      await page.getByRole('button', { name: `${hub.name} 검증 충전소 지도 마커` }).click()
    await expect(page.getByRole('dialog')).toContainText(`${hub.name} 검증 충전소`)
  })
}
