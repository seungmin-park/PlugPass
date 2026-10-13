import { defineConfig } from '@playwright/test'
import developmentConfig from './playwright.config'

export default defineConfig(developmentConfig, {
  testIgnore: [],
  testMatch: ['**/charging-journey.spec.ts', '**/navigation.spec.ts', '**/loading.spec.ts'],
  reporter: [['list'], ['json', { outputFile: 'test-results/mysql.json' }]],
  outputDir: 'test-results/mysql',
})
