import { test, expect } from './fixtures'
import searchResponse from '../tests/fixtures/search.json' with { type: 'json' }
import alternativeResponse from '../tests/fixtures/alternative.json' with { type: 'json' }

const criteria = 'latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20'

test('위치 권한을 거부해도 예시 위치를 선택할 수 있다', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'geolocation', { configurable: true, value: {
      getCurrentPosition: (_success: PositionCallback, failure: PositionErrorCallback) => failure({
        code: 1, message: '검증용 권한 거부', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3,
      }),
    } })
  })
  await page.goto('/app/index.html#/stations')
  await page.getByRole('button', { name: '현재 위치 사용' }).click()
  await expect(page.getByRole('alert')).toContainText('위치 권한이 거부되었습니다')
  await page.getByRole('button', { name: '검증용 예시 위치 사용' }).click()
  await expect(page.getByRole('status')).toContainText('예시 위치를 선택했습니다')
})

test('빈 검색 결과는 조건 변경 안내와 정확한 0개를 표시한다', async ({ page }) => {
  await page.route('**/api/v1/stations?*', route => route.fulfill({ json: { ...searchResponse, stations: [] } }))
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByText('0개 표시 · 최대 20개 표시')).toBeVisible()
  await expect(page.getByRole('status').filter({ hasText: '조건에 맞는 충전소가 없습니다' })).toBeVisible()
})

for (const failure of [
  { status: 400, message: '검색 값이 잘못되었습니다', display: '검색 조건을 확인해 주세요' },
  { status: 429, message: '잠시 후 다시 조회해 주세요', display: '잠시 후 다시 조회해 주세요' },
  { status: 503, message: '조회 서버가 응답하지 않습니다', display: '조회 서버가 응답하지 않습니다' },
]) {
  test(`HTTP ${failure.status} 오류와 재시도 복구를 표시한다`, async ({ page, diagnostics }) => {
    diagnostics.expectHttpError('/api/v1/stations', failure.status)
    await page.route('**/api/v1/stations?*', route => route.fulfill({ status: failure.status,
      json: { code: 'DEMO_ERROR', message: failure.message, fields: { radiusMeters: '반경을 확인해 주세요' } } }))
    await page.goto(`/app/index.html#/stations?${criteria}`)
    await expect(page.getByRole('alert')).toContainText(failure.display)
    await expect(page.getByRole('alert')).toContainText('반경을 확인해 주세요')
    await page.unroute('**/api/v1/stations?*')
    await page.getByRole('button', { name: '다시 조회', exact: true }).click()
    await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
    await expect(page.getByRole('alert')).toHaveCount(0)
  })
}

test('없는 충전소 상세의 HTTP 404를 안내한다', async ({ page, diagnostics }) => {
  diagnostics.expectHttpError('/api/v1/stations/999999', 404)
  await page.goto(`/app/index.html#/stations/999999?${criteria}`)
  await expect(page.getByRole('alert')).toContainText('충전소를 찾을 수 없습니다')
  await expect(page.getByRole('link', { name: '검색 목록으로 돌아가기' })).toBeVisible()
})

test('응답 timeout은 로딩을 끝내고 대기 초과를 안내한다', async ({ page }) => {
  await page.route('**/api/v1/stations?*', () => { /* 외부 응답을 보내지 않아 실제 Axios timeout을 검증한다. */ })
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await expect(page.getByText('주변 충전소를 조회하고 있습니다.')).toBeVisible()
  await expect(page.getByRole('alert')).toContainText('응답 대기 시간을 초과했습니다', { timeout: 15_000 })
  await expect(page.getByText('주변 충전소를 조회하고 있습니다.')).toHaveCount(0)
})

test('이전 검색이 늦게 도착해도 현재 조건의 목록을 유지한다', async ({ page }) => {
  let releaseFirst: (() => void) | undefined
  let completeFirst: (() => void) | undefined
  const firstReleased = new Promise<void>(resolve => { releaseFirst = resolve })
  const firstCompleted = new Promise<void>(resolve => { completeFirst = resolve })
  await page.route('**/api/v1/stations?*', async route => {
    const radius = new URL(route.request().url()).searchParams.get('radiusMeters')
    if (radius === '1000') {
      await firstReleased
      await route.fulfill({ json: searchResponse })
      completeFirst?.()
      return
    }
    await route.fulfill({ json: { ...searchResponse, stations: [searchResponse.stations[1]] } })
  })
  const initialRequest = page.waitForRequest(request => request.url().includes('radiusMeters=1000'))
  await page.goto(`/app/index.html#/stations?${criteria}`)
  await initialRequest
  await page.getByText('3km', { exact: true }).click()
  await expect(page.getByRole('radio', { name: '3km', exact: true })).toBeChecked()
  await page.getByRole('button', { name: '주변 충전소 검색' }).click()
  await expect(page.getByRole('link', { name: '대체 충전소 상세 보기' })).toBeVisible()
  releaseFirst?.()
  await firstCompleted
  await expect(page.getByRole('link', { name: '가까운 충전소 상세 보기', exact: true })).toHaveCount(0)
  await expect(page).toHaveURL(/radiusMeters=3000/)
})

test('수집 메타 오류와 재시도는 성공한 후보를 유지한다', async ({ page, diagnostics }) => {
  diagnostics.expectHttpError('/api/v1/stations', 503)
  await page.route('**/api/v1/recommendations?*', route => route.fulfill({ json: alternativeResponse }))
  await page.route('**/api/v1/stations?*', route => route.fulfill({ status: 503,
    json: { code: 'DEMO_ERROR', message: '메타 서버 장애', fields: {} } }))
  await page.goto(`/app/index.html#/alternatives?${criteria}&excludeStationId=2`)
  await expect(page.getByRole('alert')).toContainText('수집 상태 확인 실패: 메타 서버 장애')
  await expect(page.getByRole('link', { name: '대체 충전소 상세 보기' })).toBeVisible()
  await page.unroute('**/api/v1/stations?*')
  await page.getByRole('button', { name: '수집 상태 다시 조회' }).click()
  await expect(page.getByRole('alert')).toHaveCount(0)
  await expect(page.getByRole('link', { name: '대체 충전소 상세 보기' })).toBeVisible()
})
