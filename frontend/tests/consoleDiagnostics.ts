import type { TestDiagnostics } from './vueWarnings'

export function monitorConsole<T extends Pick<Console, 'warn' | 'error'>>(source: T, diagnostics: TestDiagnostics): T {
  const originalWarn = source.warn.bind(source)
  const originalError = source.error.bind(source)
  const warn = (...messages: unknown[]): void => {
    diagnostics.record('console.warn', messages.map(String).join(' '))
    originalWarn(...messages)
  }
  const error = (...messages: unknown[]): void => {
    diagnostics.record('console.error', messages.map(String).join(' '))
    originalError(...messages)
  }
  function recordReplacement(property: string | symbol): void {
    if (property === 'warn' || property === 'error') {
      diagnostics.record('monitoring', `console.${property} 진단 감시를 교체하면 안 됩니다`)
    }
  }
  return new Proxy(source, {
    get(target, property, receiver) {
      if (property === 'warn') return warn
      if (property === 'error') return error
      return Reflect.get(target, property, receiver)
    },
    set(target, property, value: unknown) {
      recordReplacement(property)
      return Reflect.set(target, property, value, target)
    },
    defineProperty(target, property, descriptor) {
      recordReplacement(property)
      return Reflect.defineProperty(target, property, descriptor)
    },
    deleteProperty(target, property) {
      recordReplacement(property)
      return Reflect.deleteProperty(target, property)
    },
  })
}
