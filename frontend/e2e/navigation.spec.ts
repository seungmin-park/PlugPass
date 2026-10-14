import { test, expect } from './fixtures'

for (const device of [
  { name: 'iPhone', userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1', expected: 'kakaomap://route?ep=37.5,127&by=car' },
  { name: 'Android', userAgent: 'Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36', expected: 'intent:0,0?q=37.5%2C127' },
]) {
  test.describe(device.name, () => {
    test.use({ userAgent: device.userAgent, viewport: { width: 375, height: 812 }, hasTouch: true, isMobile: true })

    test('검색 카드에서 한 번에 외부 지도 요청을 전달하고 상세에서도 같은 목적지를 유지한다', async ({ page }) => {
      await page.goto('/app/index.html#/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20')
      const directions = page.getByRole('link', { name: '데모 가까운 충전소 길찾기', exact: true })
      await expect(directions).toHaveAttribute('href', new RegExp(device.expected.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
      await expect(page.getByRole('dialog')).toHaveCount(0)
      const [handoff] = await Promise.all([
        page.evaluate(() => new Promise<string>(resolve => {
          document.addEventListener('click', event => {
            const anchor = event.target instanceof Element ? event.target.closest<HTMLAnchorElement>('a[id^="station-directions-"]') : null
            if (!anchor) return
            event.preventDefault()
            resolve(anchor.href)
          }, { capture: true, once: true })
        })),
        directions.click(),
      ])
      expect(handoff).toContain(device.expected)
      await expect(page.getByRole('dialog')).toHaveCount(0)
      await page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' }).click()
      await expect(page.locator('#navigation-primary')).toHaveAttribute('href', new RegExp(device.expected.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
      await expect(page.getByRole('button', { name: '지도 앱 변경' })).toHaveCount(0)
    })
  })
}

test('데스크톱에서도 목록에서 지도 선택 패널을 열고 상세에서는 키보드로 앱 선택을 조작한다', async ({ page }) => {
  await page.goto('/app/index.html#/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20')
  await page.getByRole('button', { name: '데모 가까운 충전소 길찾기', exact: true }).click()
  await expect(page.getByRole('dialog').getByRole('link', { name: '웹 지도에서 길찾기 (카카오맵)' })).toHaveAttribute('href', 'https://map.kakao.com/link/to/%EB%8D%B0%EB%AA%A8%20%EA%B0%80%EA%B9%8C%EC%9A%B4%20%EC%B6%A9%EC%A0%84%EC%86%8C,37.5,127')
  await page.getByRole('button', { name: '닫기', exact: true }).click()
  await page.getByRole('link', { name: '데모 가까운 충전소 상세 보기' }).click()
  await expect(page.locator('#navigation-primary')).toHaveAttribute('target', '_blank')
  const change = page.getByRole('button', { name: '지도 앱 변경', exact: true })
  await change.focus()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('dialog')).toBeFocused()
  await expect(page.getByRole('link', { name: '웹 지도에서 길찾기 (카카오맵)' })).toBeVisible()
  await expect(page.getByRole('link', { name: '카카오맵으로 길찾기', exact: true })).toHaveCount(0)
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(change).toBeFocused()
})
