import { test, expect } from './fixtures'

const criteria = 'latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20'

for (const viewport of [{ width: 375, height: 812 }, { width: 1440, height: 900 }]) {
  test(`${viewport.width}×${viewport.height}에서 검색·상세·후보가 가로로 넘치지 않는다`, async ({ page }) => {
    await page.setViewportSize(viewport)
    await page.goto(`/app/index.html#/stations?${criteria}`)
    await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' }).click()
    await expect(page.getByRole('heading', { name: '데모 가까운 충전소', exact: true })).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.getByRole('link', { name: '다른 충전소 찾기' }).click()
    await expect(page.getByRole('link', { name: '데모 대체 충전소 상세 보기' })).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  })
}

test('키보드로 본문·예시 위치·반경·커넥터·검색을 조작한다', async ({ page }) => {
  await page.goto('/app/index.html#/stations')
  await page.keyboard.press('Tab')
  await expect(page.getByRole('link', { name: '본문으로 건너뛰기' })).toBeFocused()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('main')).toBeFocused()
  await page.keyboard.press('Tab')
  await expect(page.getByRole('button', { name: '현재 위치 사용' })).toBeFocused()
  await page.keyboard.press('Tab')
  await page.keyboard.press('Enter')
  await expect(page.getByRole('status')).toContainText('예시 위치를 선택했습니다')
  await page.keyboard.press('Tab')
  await expect(page.getByRole('radio', { name: '1km', exact: true })).toBeFocused()
  await page.keyboard.press('ArrowRight')
  await expect(page.getByRole('radio', { name: '3km', exact: true })).toBeChecked()
  await page.keyboard.press('Tab')
  await expect(page.getByRole('radio', { name: 'DC 콤보', exact: true })).toBeFocused()
  await page.keyboard.press('Tab')
  await expect(page.getByRole('button', { name: '주변 충전소 검색' })).toBeFocused()
  await page.keyboard.press('Enter')
  await expect(page.getByText('2개 표시 · 최대 20개 표시')).toBeVisible()
  await expect(page).toHaveURL(/radiusMeters=3000/)
})

test('400 반경 오류는 해당 라디오의 설명과 포커스로 연결한다', async ({ page, diagnostics }) => {
  diagnostics.expectHttpError('/api/v1/stations', 400)
  await page.route('**/api/v1/stations?*', route => route.fulfill({ status: 400,
    json: { code: 'INVALID_REQUEST', message: '검색 조건 오류', fields: { radiusMeters: '반경 오류' } } }))
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('alert')).toContainText('반경 오류')
  const radius = page.getByRole('radio', { name: '1km', exact: true })
  await expect(radius).toBeFocused()
  await expect(radius).toHaveAttribute('aria-invalid', 'true')
  await expect(radius).toHaveAccessibleDescription('반경 오류')
})

test('서버 오류 요약은 포커스를 받아 키보드로 재시도한다', async ({ page, diagnostics }) => {
  diagnostics.expectHttpError('/api/v1/stations', 503)
  await page.route('**/api/v1/stations?*', route => route.fulfill({ status: 503,
    json: { code: 'DEMO_ERROR', message: '검색 서버 장애', fields: {} } }))
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('alert')).toBeFocused()
  await page.unroute('**/api/v1/stations?*')
  await page.keyboard.press('Tab')
  await expect(page.getByRole('button', { name: '다시 조회', exact: true })).toBeFocused()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
})

test('없는 상세는 오류 요약으로 포커스를 옮긴다', async ({ page, diagnostics }) => {
  diagnostics.expectHttpError('/api/v1/stations/999999', 404)
  await page.goto(`/app/index.html#/stations/999999?${criteria}`)
  await expect(page.getByRole('alert')).toBeFocused()
})

test('후보 메타 오류는 요약 포커스와 독립 재시도를 제공한다', async ({ page, diagnostics }) => {
  diagnostics.expectHttpError('/api/v1/stations', 503)
  await page.route('**/api/v1/stations?*', route => route.fulfill({ status: 503,
    json: { code: 'DEMO_ERROR', message: '메타 장애', fields: {} } }))
  await page.goto(`/app/index.html#/alternatives?${criteria}&excludeStationId=1`)
  await expect(page.getByRole('alert')).toBeFocused()
  await expect(page.getByRole('button', { name: '수집 상태 다시 조회' })).toBeVisible()
  await expect(page.getByRole('link', { name: '데모 대체 충전소 상세 보기' })).toBeVisible()
})
