import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: [['list'], ['json', { outputFile: 'test-results/e2e.json' }], ['html', { open: 'never' }]],
  outputDir: 'test-results/e2e',
  use: {
    baseURL: process.env.PLUGPASS_E2E_BASE_URL ?? 'http://127.0.0.1:5183',
    headless: Boolean(process.env.CI),
    trace: 'on',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium', viewport: { width: 1440, height: 900 } } }],
})
