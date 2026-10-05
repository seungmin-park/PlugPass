import { test as base, expect, type ConsoleMessage } from '@playwright/test'

class BrowserDiagnostics {
  private readonly messages: { type: string; text: string; url: string }[] = []
  private readonly expectedHttpErrors: { pathname: string; status: number; count: number }[] = []

  expectHttpError(pathname: string, status: number, count = 1): void {
    this.expectedHttpErrors.push({ pathname, status, count })
  }
  recordConsole(message: ConsoleMessage): void {
    if (message.type() === 'warning' || message.type() === 'error') {
      this.messages.push({ type: message.type(), text: message.text(), url: message.location().url })
    }
  }
  recordException(error: Error): void { this.messages.push({ type: 'pageerror', text: error.message, url: '' }) }
  assertExpected(): void {
    const remaining = [...this.messages]
    for (const expected of this.expectedHttpErrors) {
      const matching = remaining.filter(message => message.type === 'error' && !message.text.includes('[Vue warn]')
        && message.text.includes('Failed to load resource:') && message.text.includes(`status of ${expected.status}`)
        && URL.canParse(message.url) && new URL(message.url).pathname === expected.pathname)
      expect(matching, `예상 HTTP ${expected.status} 진단 수`).toHaveLength(expected.count)
      for (const message of matching) remaining.splice(remaining.indexOf(message), 1)
    }
    expect(remaining, '예상 밖 console 경고·오류 또는 처리되지 않은 예외').toEqual([])
  }
}

export const test = base.extend<{ diagnostics: BrowserDiagnostics }>({
  diagnostics: async ({ browserName }, use) => {
    expect(browserName).toBe('chromium')
    await use(new BrowserDiagnostics())
  },
  page: async ({ context, diagnostics }, use) => {
    const page = await context.newPage()
    page.on('console', message => diagnostics.recordConsole(message))
    page.on('pageerror', error => diagnostics.recordException(error))
    try { await use(page) }
    finally {
      await page.close()
      diagnostics.assertExpected()
    }
  },
})
export { expect }
export type { Page } from '@playwright/test'
