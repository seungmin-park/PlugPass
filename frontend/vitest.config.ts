import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config.ts'

export default mergeConfig(viteConfig, defineConfig({
  test: {
    environment: 'jsdom',
    setupFiles: ['tests/setup.ts'],
    exclude: [...configDefaults.exclude, 'e2e/**'],
    reporters: ['default', 'json'],
    outputFile: 'test-results/unit.json',
  },
}))
