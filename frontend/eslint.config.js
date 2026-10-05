import js from '@eslint/js'
import globals from 'globals'
import vue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'

export default [
  { ignores: ['dist/**', 'node_modules/**', 'test-results/**', 'playwright-report/**'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs['flat/essential'],
  {
    files: ['**/*.{ts,vue,js}'],
    languageOptions: {
      globals: { ...globals.browser, ...globals.node },
      parserOptions: { parser: tseslint.parser },
    },
    rules: { 'vue/multi-word-component-names': ['error', { ignores: ['App'] }] },
  },
  {
    files: ['src/**/*.vue'],
    rules: { 'no-restricted-imports': ['error', { patterns: [
      { group: ['axios', 'axios/**', '**/api/httpClient'], message: 'Vue는 Store/props/event 경계를 사용합니다.' },
    ] }] },
  },
  {
    files: ['src/**/api/**/*.ts'],
    rules: { 'no-restricted-imports': ['error', { patterns: [
      { group: ['**/stores/**', '**/views/**', '**/components/**'], message: 'API는 Store/View/Component에 역의존하지 않습니다.' },
    ] }] },
  },
  {
    files: ['e2e/**/*.spec.ts'],
    rules: { 'no-restricted-imports': ['error', { paths: [
      { name: '@playwright/test', message: '공용 fixtures의 test/expect로 진단 감시를 유지합니다.' },
    ] }] },
  },
]
