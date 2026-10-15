import { test, expect } from './fixtures'
import { mockKakaoSdk } from './kakaoSdkMock'

test('native 지도 자리 밖에 확대와 영역 검색 버튼을 배치한다', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto('/app/index.html#/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20')
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  // Native 화면 배치만 검사한다. SDK/기기 터치는 이 브라우저 검사에서 대체하지 않는다.
  await page.locator('html').evaluate(element => { element.dataset.platform = 'native' })
  const mapBottom = await page.locator('.station-map-canvas').evaluate(element => element.getBoundingClientRect().bottom)
  expect(await page.getByRole('button', { name: '지도 확대', exact: true }).evaluate(element => element.getBoundingClientRect().top)).toBeGreaterThanOrEqual(mapBottom)
  expect(await page.getByRole('button', { name: '이 지역 재검색', exact: true }).evaluate(element => element.getBoundingClientRect().top)).toBeGreaterThanOrEqual(mapBottom)
})

const criteria = 'latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20'
test('마커는 작은 시각 핀과 손가락으로 누를 수 있는 44px 영역을 함께 제공한다', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  const marker = page.getByRole('button', { name: '데모 대체 충전소 지도 마커' })
  await expect(marker).toBeVisible()
  const sizes = await marker.evaluate(element => ({
    hitWidth: element.getBoundingClientRect().width,
    hitHeight: element.getBoundingClientRect().height,
    pinWidth: getComputedStyle(element, '::before').width,
  }))
  expect(sizes.hitWidth).toBeGreaterThanOrEqual(44)
  expect(sizes.hitHeight).toBeGreaterThanOrEqual(44)
  expect(sizes.pinWidth).toBe('20px')
  await marker.click()
  await expect(page.getByRole('dialog')).toContainText('데모 대체 충전소')
})
test('조건·지도·목록은 겹침과 내부 스크롤 없이 첫 결과를 보여 준다', async ({ page }) => {
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.locator('.station-card').first()).toBeVisible()
  for (const width of [320, 375, 430, 1440]) {
    await page.setViewportSize({ width, height: 812 })
    await page.evaluate(() => window.scrollTo(0, 0))
    const layout = await page.evaluate(() => {
      const conditions = document.querySelector('#search-conditions')!.getBoundingClientRect()
      const map = document.querySelector('.search-map-explorer')!.getBoundingClientRect()
      const results = document.querySelector('.search-results')!.getBoundingClientRect()
      const title = document.querySelector('.station-card h3')!.getBoundingClientRect()
      const list = document.querySelector('.station-list')!
      return { conditionsBeforeMap: conditions.bottom <= map.top, mapBeforeResults: map.bottom <= results.top,
        firstTitleVisible: title.bottom <= innerHeight, overflow: getComputedStyle(list).overflowY,
        fits: document.documentElement.scrollWidth <= innerWidth }
    })
    expect(layout).toEqual({ conditionsBeforeMap: true, mapBeforeResults: true, firstTitleVisible: true, overflow: 'visible', fits: true })
    await expect(page.getByRole('button', { name: '지도 접기', exact: true })).toHaveCount(0)
    await page.screenshot({ path: `test-results/redesign-${width}.png`, fullPage: true })
  }
})
test('지도 축척을 조절하고 안내·조작 요소가 작은 화면에서 겹치지 않는다', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('button', { name: '지도 확대' })).toBeVisible()
  for (const width of [320, 375, 768, 1440]) {
    await page.setViewportSize({ width, height: 812 })
    const metrics = await page.evaluate(() => {
      const frame = document.querySelector('.station-map-frame')?.getBoundingClientRect()
      const search = document.getElementById('search-map-area')?.getBoundingClientRect()
      const zoom = document.querySelector('.map-zoom-controls')?.getBoundingClientRect()
      if (!frame || !search || !zoom) return null
      return {
        searchInside: search.bottom <= frame.bottom && search.right <= frame.right,
        controlsSeparated: search.right + 8 <= zoom.left || search.top >= zoom.bottom + 8,
        fitsWidth: document.documentElement.scrollWidth <= innerWidth }
    })
    expect(metrics, `${width}px 지도 요소`).not.toBeNull()
    expect(metrics!.searchInside, `${width}px 재검색 버튼`).toBe(true)
    expect(metrics!.controlsSeparated, `${width}px 지도 조작 버튼 간격`).toBe(true)
    expect(metrics!.fitsWidth, `${width}px 가로 폭`).toBe(true)
  }
  await page.getByRole('button', { name: '지도 확대' }).click()
  await expect(page.locator('.station-map-panel [role="status"]')).toContainText('확대 수준 4단계')
  await page.getByRole('button', { name: '지도 축소' }).click()
  await expect(page.locator('.station-map-panel [role="status"]')).toContainText('확대 수준 5단계')
})
test('실제 검색 결과의 마커↔카드 선택과 조건 변경 초기화를 연결한다', async ({ page }) => {
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  await expect(page.locator('.kakao-test-map')).toHaveCount(1)
  const firstMarker = page.getByRole('button', { name: '데모 가까운 충전소 지도 마커', exact: true })
  const secondMarker = page.getByRole('button', { name: '데모 대체 충전소 지도 마커', exact: true })
  const firstCard = page.getByRole('article', { name: '데모 가까운 충전소', exact: true })
  const secondCard = page.getByRole('article', { name: '데모 대체 충전소', exact: true })
  await expect(page.getByRole('button', { name: /지도에서 선택/ })).toHaveCount(0)
  await secondMarker.click()
  await expect(secondCard).toHaveClass(/station-card-selected/)
  await expect(firstCard).not.toHaveClass(/station-card-selected/)
  await expect(page.getByRole('dialog')).toContainText('데모 대체 충전소')
  await page.getByRole('button', { name: '닫기', exact: true }).click()
  await firstMarker.click()
  await expect(firstMarker).toHaveAttribute('aria-pressed', 'true')
  await expect(secondMarker).toHaveAttribute('aria-pressed', 'false')
  await expect(page.locator('.kakao-test-map')).toBeAttached()
  await page.getByRole('button', { name: '이 지역 재검색' }).click()
  await expect(page).toHaveURL(/south=/)
  await expect(secondMarker).toBeVisible()
  await expect(firstCard).not.toHaveClass(/station-card-selected/)
  await expect(firstMarker).toHaveAttribute('aria-pressed', 'false')
  await secondMarker.click()
  await page.getByRole('button', { name: '새로고침', exact: true }).click()
  await expect(secondCard).not.toHaveClass(/station-card-selected/)
  await expect(secondMarker).toHaveAttribute('aria-pressed', 'false')
})

test('SDK 다운로드 실패에도 목록·상세 링크를 유지하고 복구한다', async ({ page, diagnostics }) => {
  await page.route('https://dapi.kakao.com/v2/maps/sdk.js?**', async route => {
    diagnostics.expectHttpError(new URL(route.request().url()).pathname, 503)
    await route.fulfill({ status: 503, body: 'SDK failure' })
  }, { times: 1 })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('alert')).toContainText('지도를 불러오지 못했습니다')
  await expect(page.getByRole('alert')).toBeFocused()
  await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
  await page.getByRole('button', { name: '화면 새로고침', exact: true }).click()
  await expect(page).toHaveURL(/latitude=37\.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20$/)
  await expect(page.getByRole('button', { name: '데모 가까운 충전소 지도 마커', exact: true })).toBeVisible()
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('지도 배경 준비가 늦어져도 목록을 유지하고 재시도로 복구한다', async ({ page }) => {
  await mockKakaoSdk(page, { failFirstTiles: true })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByRole('alert')).toContainText('배경 지도를 불러오지 못했습니다', { timeout: 15000 })
  await expect(page.getByRole('link', { name: '데모 대체 충전소 상세 보기' })).toBeVisible()
  await page.getByRole('button', { name: '지도 다시 시도' }).click()
  await expect(page.getByRole('button', { name: '데모 대체 충전소 지도 마커', exact: true })).toBeVisible()
  await expect(page.getByRole('region', { name: '검색 결과 지도' })).toHaveAttribute('aria-busy', 'false')
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('모바일·데스크톱의 지도는 가로 넘침 없이 키보드로 마커를 선택한다', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  const marker = page.getByRole('button', { name: '데모 가까운 충전소 지도 마커', exact: true })
  await marker.focus()
  await page.keyboard.press('Space')
  await expect(page.getByRole('article', { name: '데모 가까운 충전소', exact: true })).toHaveClass(/station-card-selected/)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.setViewportSize({ width: 1440, height: 900 })
  await marker.focus()
  await page.keyboard.press('Enter')
  await expect(marker).toHaveAttribute('aria-pressed', 'true')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)

})

test('지도 마커를 키보드로 선택하면 하단 패널을 열고 닫은 뒤 마커에 돌아온다', async ({ page }) => {
  await page.goto(`/app/index.html#/stations?${criteria}`)
  const marker = page.getByRole('button', { name: '데모 가까운 충전소 지도 마커', exact: true })
  await marker.focus()
  await marker.press('Enter')
  await expect(page.getByRole('dialog')).toBeFocused()
  await expect(page.getByRole('dialog')).toContainText('데모 가까운 충전소')
  await expect(page.getByRole('link', { name: '웹 지도에서 길찾기 (카카오맵)', exact: true })).toBeVisible()
  await page.keyboard.press('Tab')
  await expect(page.getByRole('button', { name: '닫기', exact: true })).toBeFocused()
  await page.keyboard.press('Tab')
  await expect(page.getByRole('link', { name: '웹 지도에서 길찾기 (카카오맵)', exact: true })).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(marker).toBeFocused()
})

 test('카드 제목 선택과 확대 글꼴에서도 지도 선택·정보 접근을 유지한다', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 812 })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  const title = page.locator('.station-card h3 button').first()
  await title.focus()
  await title.press('Enter')
  await expect(page.getByRole('button', { name: '데모 가까운 충전소 지도 마커', exact: true })).toHaveAttribute('aria-pressed', 'true')
  await page.keyboard.press('Escape')
  await page.evaluate(() => { document.documentElement.style.fontSize = '200%'; document.body.style.fontSize = '200%' })
  await expect(page.locator('.station-card').first()).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.locator('.charger-toggle').first().click()
  await expect(page.locator('.charger-badges').first()).toBeVisible()
  await page.screenshot({ path: 'test-results/redesign-large-text.png', fullPage: true })
})
