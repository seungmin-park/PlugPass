import { test, expect } from './fixtures'

for (const width of [320, 375]) test(`${width}px에서 GPS 결과를 지도와 하단 목록에 바로 보여 준다`, async ({ page }) => {
  await page.setViewportSize({ width, height: 812 })
  await page.context().grantPermissions(['geolocation'])
  await page.context().setGeolocation({ latitude: 37.5, longitude: 127 })
  await page.addInitScript(() => localStorage.setItem('plugpass.vehicle-profile.v1', JSON.stringify({
    vehicleId: 'kona-electric-2020-kr', adapterIds: [], vehicleSupportConfirmed: false,
  })))
  await page.goto('/app/index.html#/stations')
  await expect(page.getByRole('region', { name: '검색 결과 지도' })).toBeVisible()
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  const layout = await page.evaluate(() => {
    const map = document.querySelector('.station-map-canvas')?.getBoundingClientRect()
    const results = document.querySelector('.search-results')?.getBoundingClientRect()
    const firstCard = document.querySelector('.station-card')?.getBoundingClientRect()
    if (!map || !results || !firstCard) throw new Error('지도 또는 결과가 없습니다')
    return { mapHeight: map.height, overlap: map.bottom - results.top,
      firstCardTop: firstCard.top, horizontalOverflow: document.documentElement.scrollWidth > innerWidth }
  })
  expect(layout.mapHeight).toBeGreaterThanOrEqual(160)
  expect(layout.overlap).toBeLessThanOrEqual(-8)
  expect(layout.firstCardTop).toBeLessThan(750)
  expect(layout.horizontalOverflow).toBe(false)
})

test('자동 GPS가 거부되면 화면 상단에서 이유·재시도·주소 검색을 보여 준다', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 812 })
  await page.addInitScript(() => {
    localStorage.setItem('plugpass.vehicle-profile.v1', JSON.stringify({
      vehicleId: 'kona-electric-2020-kr', adapterIds: [], vehicleSupportConfirmed: false,
    }))
    Object.defineProperty(navigator, 'geolocation', { configurable: true, value: {
      getCurrentPosition: (_success: PositionCallback, failure: PositionErrorCallback) => failure({
        code: 1, message: 'denied', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3,
      }),
    } })
  })
  let searchRequests = 0
  await page.route('**/api/v1/stations?*', route => { searchRequests += 1; return route.continue() })
  await page.goto('/app/index.html#/stations')
  const alert = page.locator('.search-location-alert')
  await expect(alert).toContainText('위치 권한이 거부되었습니다')
  await expect(alert.getByRole('button', { name: '현재 위치 다시 시도' })).toBeVisible()
  await alert.getByRole('button', { name: '주소로 검색하기' }).click()
  await expect(page.getByRole('searchbox', { name: '주소·장소 검색', exact: true })).toBeFocused()
  expect(searchRequests).toBe(0)
})

test('직접 검색 URL에서는 GPS를 요청하지 않고 지도 열기 버튼을 가리지 않는다', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'geolocation', { configurable: true, value: {
      getCurrentPosition: () => { throw new Error('직접 URL은 GPS를 요청하면 안 됩니다') },
    } })
  })
  await page.goto('/app/index.html#/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20')
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  const layout = await page.evaluate(() => {
    const toggle = document.querySelector('.search-map-explorer')?.getBoundingClientRect()
    const results = document.querySelector('.search-results')?.getBoundingClientRect()
    if (!toggle || !results) throw new Error('지도 버튼 또는 결과가 없습니다')
    return { toggleBottom: toggle.bottom, resultsTop: results.top }
  })
  expect(layout.resultsTop).toBeGreaterThanOrEqual(layout.toggleBottom + 8)
})
