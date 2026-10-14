export function scheduleIosInstallFallback(page: Document, storeHref: string, openStore: (href: string) => void): void {
  const browser = page.defaultView
  if (!browser) return
  const cancel = (): void => {
    browser.clearTimeout(timer)
    page.removeEventListener('visibilitychange', onVisibilityChange)
    browser.removeEventListener('pagehide', cancel)
  }
  const onVisibilityChange = (): void => { if (page.hidden) cancel() }
  const timer = browser.setTimeout(() => {
    cancel()
    if (!page.hidden) openStore(storeHref)
  }, 2500)
  page.addEventListener('visibilitychange', onVisibilityChange)
  browser.addEventListener('pagehide', cancel)
}
