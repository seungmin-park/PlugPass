import { test, expect, type Page } from './fixtures'

const criteria = 'latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20'
// 자동 반복 검증은 공용 타일 서버를 호출하지 않는다. 실제 외부 타일은 cmux에서 별도로 확인한다.
const tileImage = '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"><rect width="256" height="256" fill="#eff4ff"/><path d="M0 128H256M128 0V256" stroke="#dce9ff"/></svg>'
async function serveControlledTiles(page: Page): Promise<void> {
  await page.route('https://tile.openstreetmap.org/**', route => route.fulfill({ contentType: 'image/svg+xml', body: tileImage }))
}

test('실제 검색 결과의 마커↔카드 선택과 조건 변경 초기화를 연결한다', async ({ page }) => {
  let tileRequests = 0
  await page.route('https://tile.openstreetmap.org/**', route => {
    tileRequests += 1
    return route.fulfill({ contentType: 'image/svg+xml', body: tileImage })
  })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  expect(tileRequests).toBe(0)
  await page.getByRole('button', { name: '지도 보기', exact: true }).click()
  const firstMarker = page.getByRole('button', { name: '데모 가까운 충전소 지도 마커', exact: true })
  const secondMarker = page.getByRole('button', { name: '데모 대체 충전소 지도 마커', exact: true })
  const firstCard = page.getByRole('button', { name: '데모 가까운 충전소 지도에서 선택', exact: true })
  const secondCard = page.getByRole('button', { name: '데모 대체 충전소 지도에서 선택', exact: true })
  await secondMarker.click()
  await expect(secondCard).toHaveAttribute('aria-pressed', 'true')
  await expect(firstCard).toHaveAttribute('aria-pressed', 'false')
  await firstCard.click()
  await expect(firstMarker).toHaveAttribute('aria-pressed', 'true')
  await expect(secondMarker).toHaveAttribute('aria-pressed', 'false')
  await expect(page.getByRole('link', { name: 'OpenStreetMap', exact: true })).toHaveAttribute('href', 'https://www.openstreetmap.org/copyright')
  await page.getByText('3km', { exact: true }).click()
  await expect(page.getByRole('radio', { name: '3km', exact: true })).toBeChecked()
  await page.getByRole('button', { name: '주변 충전소 검색' }).click()
  await expect(page).toHaveURL(/radiusMeters=3000/)
  await expect(secondMarker).toBeVisible()
  await expect(firstCard).toHaveAttribute('aria-pressed', 'false')
  await expect(firstMarker).toHaveAttribute('aria-pressed', 'false')
  await secondMarker.click()
  await page.getByRole('button', { name: '새로고침', exact: true }).click()
  await expect(secondCard).toHaveAttribute('aria-pressed', 'false')
  await expect(secondMarker).toHaveAttribute('aria-pressed', 'false')
})

test('SDK 다운로드 실패에도 목록·상세 링크를 유지하고 복구한다', async ({ page, diagnostics }) => {
  await serveControlledTiles(page)
  await page.route('**/leafletStationMap.ts*', async route => {
    diagnostics.expectHttpError(new URL(route.request().url()).pathname, 503)
    await route.fulfill({ status: 503, body: 'SDK failure' })
  }, { times: 1 })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await page.getByRole('button', { name: '지도 보기', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('지도를 불러오지 못했습니다')
  await expect(page.getByRole('alert')).toBeFocused()
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  await page.getByRole('button', { name: '화면 새로고침', exact: true }).click()
  await expect(page).toHaveURL(/latitude=37\.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20$/)
  await page.getByRole('button', { name: '지도 보기', exact: true }).click()
  await expect(page.getByRole('button', { name: '데모 가까운 충전소 지도 마커', exact: true })).toBeVisible()
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('타일 실패는 목록을 유지하고 지도 재시도로 복구한다', async ({ page, diagnostics }) => {
  let failNextTile = true
  await page.route('https://tile.openstreetmap.org/**', async route => {
    if (failNextTile) {
      failNextTile = false
      diagnostics.expectHttpError(new URL(route.request().url()).pathname, 503)
      await route.fulfill({ status: 503, body: 'tile failure' })
      return
    }
    await route.fulfill({ contentType: 'image/svg+xml', body: tileImage })
  })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await page.getByRole('button', { name: '지도 보기', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('배경 지도를 불러오지 못했습니다')
  await expect(page.getByRole('link', { name: '데모 대체 충전소 상세 보기' })).toBeVisible()
  await page.getByRole('button', { name: '지도 다시 시도' }).click()
  await expect(page.getByRole('button', { name: '데모 대체 충전소 지도 마커', exact: true })).toBeVisible()
  await expect(page.getByRole('region', { name: '검색 결과 지도' })).toHaveAttribute('aria-busy', 'false')
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('모바일·데스크톱의 지도는 가로 넘침 없이 키보드로 마커를 선택한다', async ({ page }) => {
  await serveControlledTiles(page)
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  const showMap = page.getByRole('button', { name: '지도 보기', exact: true })
  await showMap.focus()
  await page.keyboard.press('Enter')
  const marker = page.getByRole('button', { name: '데모 가까운 충전소 지도 마커', exact: true })
  await marker.focus()
  await page.keyboard.press('Space')
  await expect(page.getByRole('button', { name: '데모 가까운 충전소 지도에서 선택', exact: true })).toHaveAttribute('aria-pressed', 'true')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.setViewportSize({ width: 1440, height: 900 })
  await marker.focus()
  await page.keyboard.press('Enter')
  await expect(marker).toHaveAttribute('aria-pressed', 'true')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.getByRole('button', { name: '지도 접기', exact: true }).click()
  await expect(page.getByRole('region', { name: '검색 결과 지도' })).toHaveCount(0)
  await page.getByRole('button', { name: '지도 보기', exact: true }).click()
  await expect(marker).toHaveAttribute('aria-pressed', 'false')
})
