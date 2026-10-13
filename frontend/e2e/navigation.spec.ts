import { test, expect } from './fixtures'

for (const device of [
  { name: 'iPhone', userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1', prefix: 'kakaomap://' },
  { name: 'Android', userAgent: 'Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36', prefix: 'intent://' },
]) {
  test.describe(device.name, () => {
    test.use({ userAgent: device.userAgent, viewport: { width: 375, height: 812 }, hasTouch: true, isMobile: true })

    test('실제 충전소 목적지를 앱 링크로 전달하고 웹 대안과 검색 복귀를 유지한다', async ({ page }) => {
      await page.goto('/app/index.html#/stations')
      await page.context().grantPermissions(['geolocation'])
  await page.context().setGeolocation({ latitude: 37.5, longitude: 127 })
  await page.getByRole('button', { name: '현재 위치 사용', exact: true }).click()
  await expect(page.getByText('현재 위치: 37.5 / 127', { exact: true })).toBeVisible()
      await page.getByRole('button', { name: '주변 충전소 검색' }).click()
      await page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' }).click()
      await page.locator('summary').filter({ hasText: '길찾기' }).click()
      const kakao = page.getByRole('link', { name: '카카오맵으로 길찾기', exact: true })
      const naver = page.getByRole('link', { name: '네이버 지도로 길찾기', exact: true })
      await expect(kakao).toBeVisible()
      await expect(naver).toBeVisible()
      await expect(page.getByRole('link', { name: '카카오맵 설치', exact: true })).toBeVisible()
      await expect(page.getByRole('link', { name: '네이버 지도 설치', exact: true })).toBeVisible()
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)

      // OS 실행 경계만 가로채 전달 URL을 assertion한다. 실기기 앱 실행을 증명하지 않는다.
      const [handoff] = await Promise.all([
        page.evaluate(() => new Promise<string>(resolve => {
          document.addEventListener('click', event => {
            const anchor = event.target instanceof Element ? event.target.closest<HTMLAnchorElement>('a[id^="navigation-"]') : null
            if (!anchor) return
            event.preventDefault()
            resolve(anchor.href)
          }, { once: true })
        })),
        kakao.click(),
      ])
      expect(handoff).toContain(`${device.prefix}route?ep=37.5,127&by=car`)
      const naverHref = await naver.getAttribute('href')
      expect(naverHref).toContain('route/car?dlat=37.5&dlng=127&dname=')
      expect(naverHref).not.toMatch(/[?&]slat=/)
      await expect(page.getByText('앱이 열리지 않으면', { exact: false })).toBeVisible()

      // 외부 사이트 내용은 통제하고, 실제 클릭·새 탭에 전달되는 목적지는 검증한다.
      await page.context().route('https://map.kakao.com/link/to/**', route => route.fulfill({ contentType: 'text/html', body: '<h1>외부 지도 경계</h1>' }))
      const popupPromise = page.waitForEvent('popup')
      await page.getByRole('link', { name: '웹 지도에서 길찾기 (카카오맵)', exact: true }).click()
      const popup = await popupPromise
      await popup.waitForLoadState()
      expect(decodeURIComponent(popup.url())).toBe('https://map.kakao.com/link/to/데모 가까운 충전소,37.5,127')
      await popup.close()
      await page.getByRole('link', { name: '검색 목록으로 돌아가기' }).click()
      await expect(page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' })).toBeVisible()
      await page.getByRole('link', { name: '데모 대체 충전소 상세 보기' }).click()
      await expect(page.locator('details')).not.toHaveAttribute('open', '')
      await page.locator('summary').click()
      await expect(kakao).toHaveAttribute('href', new RegExp(`${device.prefix}route\\?ep=37\\.5,127\\.001&by=car`))
    })
  })
}

test('데스크톱 키보드로 길찾기를 열고 닫을 수 있다', async ({ page }) => {
  await page.goto('/app/index.html#/stations')
  await page.context().grantPermissions(['geolocation'])
  await page.context().setGeolocation({ latitude: 37.5, longitude: 127 })
  await page.getByRole('button', { name: '현재 위치 사용', exact: true }).click()
  await expect(page.getByText('현재 위치: 37.5 / 127', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '주변 충전소 검색' }).click()
  await page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' }).click()
  await page.locator('summary').focus()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('link', { name: '웹 지도에서 길찾기 (카카오맵)' })).toBeVisible()
  await expect(page.getByRole('link', { name: '카카오맵으로 길찾기', exact: true })).toHaveCount(0)
  await page.keyboard.press('Enter')
  await expect(page.getByRole('link', { name: '웹 지도에서 길찾기 (카카오맵)' })).not.toBeVisible()
})
