// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { build, resolveConfig } from 'vite'
import { fileURLToPath } from 'node:url'

const configFile = fileURLToPath(new URL('../vite.config.ts', import.meta.url))

describe('웹과 설치 앱의 빌드 계약', () => {
  it('웹은 기존 /app/ 경로와 dist 산출물을 유지한다', async () => {
    const config = await resolveConfig({ configFile }, 'build', 'production')
    expect(config.base).toBe('/app/')
    expect(config.build.outDir).toBe('dist')
  })

  it('native 모드는 웹 산출물을 덮어쓰지 않는다', async () => {
    const config = await resolveConfig({ configFile, mode: 'native' }, 'build')
    expect(config.build.outDir).toBe('dist-native')
  })

  it('앱의 실제 HTML은 기기 내부에서 읽을 수 있는 상대 자산을 참조한다', async () => {
    const bundle = await build({ configFile, mode: 'native', logLevel: 'silent', build: { write: false } })
    if (Array.isArray(bundle) || !('output' in bundle)) throw new Error('단일 앱 묶음이 필요합니다')
    const html = bundle.output.find(asset => asset.fileName === 'index.html')
    if (!html || html.type !== 'asset' || typeof html.source !== 'string') throw new Error('앱 index.html 누락')
    expect(html.source).toMatch(/src="\.\/assets\//)
    expect(html.source).not.toContain('="/app/')
  })
})
