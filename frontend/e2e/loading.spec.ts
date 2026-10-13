import { test, expect } from './fixtures'

const criteria = 'latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20'

test('모바일 검색 대기 중 원이 실제 회전하고 실제 응답 뒤 사라진다', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.emulateMedia({ reducedMotion: 'no-preference' })
  let release!: () => void
  const pending = new Promise<void>(resolve => { release = resolve })
  await page.route('**/api/v1/stations?*', async route => { await pending; await route.continue() })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  try {
    const indicator = page.getByRole('status').filter({ hasText: '주변 충전소를 조회하고 있습니다.' })
    await expect(indicator).toBeVisible()
    await expect(indicator).toHaveAttribute('aria-atomic', 'true')
    const spinner = indicator.locator('.loading-spinner')
    await expect(spinner).toBeVisible()
    await expect(spinner).toHaveAttribute('aria-hidden', 'true')
    const firstTransform = await spinner.evaluate(element => getComputedStyle(element).transform)
    await expect.poll(() => spinner.evaluate(element => getComputedStyle(element).transform)).not.toBe(firstTransform)
    expect(await spinner.evaluate(element => getComputedStyle(element).animationIterationCount)).toBe('infinite')
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await expect(page.getByRole('button', { name: '새로고침', exact: true })).toBeDisabled()
  } finally { release() }
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  await expect(page.locator('.loading-spinner')).toHaveCount(0)
  await expect(page.getByRole('button', { name: '새로고침', exact: true })).toBeEnabled()
})

test('움직임 감소 설정은 회전을 멈추고 진행 문구를 유지한다', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  let release!: () => void
  const pending = new Promise<void>(resolve => { release = resolve })
  await page.route('**/api/v1/stations?*', async route => { await pending; await route.continue() })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  try {
    await expect(page.getByRole('status').filter({ hasText: '주변 충전소를 조회하고 있습니다.' })).toBeVisible()
    await expect(page.locator('.loading-spinner')).toBeVisible()
    expect(await page.locator('.loading-spinner').evaluate(element => getComputedStyle(element).animationName)).toBe('none')
  } finally { release() }
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  await expect(page.locator('.loading-spinner')).toHaveCount(0)
})

test('조회 실패 시 회전이 끝나고 재시도로 실제 결과를 보여 준다', async ({ page, diagnostics }) => {
  diagnostics.expectHttpError('/api/v1/stations', 503)
  let release!: () => void
  const pending = new Promise<void>(resolve => { release = resolve })
  await page.route('**/api/v1/stations?*', async route => {
    await pending
    await route.fulfill({ status: 503, json: { code: 'DEMO_ERROR', message: '조회 서버가 응답하지 않습니다', fields: {} } })
  }, { times: 1 })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  try { await expect(page.locator('.loading-spinner')).toBeVisible() }
  finally { release() }
  await expect(page.getByRole('alert')).toContainText('조회 서버가 응답하지 않습니다')
  await expect(page.locator('.loading-spinner')).toHaveCount(0)
  await page.getByRole('button', { name: '다시 조회', exact: true }).click()
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  await expect(page.locator('.loading-spinner')).toHaveCount(0)
})
