import { test, expect } from './fixtures'

const criteria = 'latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20'

test('배포 JAR의 JS·CSS와 같은 origin 검색 API가 실제 화면을 만든다', async ({ page }) => {
  const assets: { url: string; status: number }[] = []
  page.on('response', response => { if (response.url().includes('/app/assets/')) assets.push({ url: response.url(), status: response.status() }) })
  const searchResponse = page.waitForResponse(response => response.url().includes('/api/v1/stations?'))
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('heading', { name: '주변 충전소 찾기', exact: true })).toBeVisible()
  await expect(page.getByRole('status').filter({ hasText: '아직 전체 수집이 완료되지 않았습니다' })).toBeVisible()
  const response = await searchResponse
  expect(response.status()).toBe(200)
  expect(new URL(response.url()).origin).toBe(new URL(page.url()).origin)
  expect(await response.json()).toEqual({ dataReady: false, lastSuccessfulRunAt: null, stations: [] })
  expect(assets.some(asset => new URL(asset.url).pathname.endsWith('.js') && asset.status === 200)).toBe(true)
  expect(assets.some(asset => new URL(asset.url).pathname.endsWith('.css') && asset.status === 200)).toBe(true)
  await page.reload()
  await expect(page.getByText('0개 표시 · 최대 20개 표시')).toBeVisible()
})

test('배포 JAR의 상세 hash 직접 링크와 새로고침은 같은 API 404를 안내한다', async ({ page, diagnostics }) => {
  diagnostics.expectHttpError('/api/v1/stations/999999', 404, 2)
  await page.goto(`/app/index.html#/stations/999999?${criteria}`)
  await expect(page.getByRole('heading', { name: '충전소 상세', exact: true })).toBeVisible()
  await expect(page.getByRole('alert')).toContainText('충전소를 찾을 수 없습니다')
  await page.reload()
  await expect(page.getByRole('alert')).toBeFocused()
  await page.getByRole('link', { name: '검색 목록으로 돌아가기' }).click()
  await expect(page.getByRole('heading', { name: '주변 충전소 찾기', exact: true })).toBeVisible()
  await expect(page).toHaveURL(/latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20$/)
})

test('배포 JAR의 지도 JS·CSS를 불러와 빈 검색 결과의 지도와 출처를 표시한다', async ({ page }) => {
  await page.route('https://tile.openstreetmap.org/**', route => route.fulfill({ contentType: 'image/svg+xml',
    body: '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"><rect width="256" height="256" fill="#eff4ff"/></svg>' }))
  const loadedAssets: string[] = []
  page.on('response', response => {
    if (response.status() === 200 && response.url().includes('/app/assets/')) loadedAssets.push(response.url())
  })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByText('0개 표시 · 최대 20개 표시')).toBeVisible()
  loadedAssets.length = 0
  await page.getByRole('button', { name: '지도 보기', exact: true }).click()
  await expect(page.getByRole('region', { name: '검색 결과 지도' })).toHaveAttribute('aria-busy', 'false')
  await expect(page.getByRole('link', { name: 'OpenStreetMap', exact: true })).toBeVisible()
  await expect(page.locator('.leaflet-marker-icon')).toHaveCount(0)
  expect(loadedAssets.some(url => new URL(url).pathname.endsWith('.js'))).toBe(true)
  expect(loadedAssets.some(url => new URL(url).pathname.endsWith('.css'))).toBe(true)
  await expect(page.getByRole('alert')).toHaveCount(0)
})
