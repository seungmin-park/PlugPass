import { defineConfig } from '@playwright/test'
import developmentConfig from './playwright.config'

export default defineConfig(developmentConfig, {
  testIgnore: [],
  testMatch: '**/packaged-webapp.spec.ts',
  reporter: [['list'], ['json', { outputFile: 'test-results/packaged.json' }],
    ['html', { open: 'never', outputFolder: 'playwright-report/packaged' }]],
  outputDir: 'test-results/packaged',
})
