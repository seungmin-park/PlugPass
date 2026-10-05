import { watchPostEffect, type Ref } from 'vue'

export function useErrorFocus(hasError: () => boolean, target: Ref<HTMLElement | null>): void {
  watchPostEffect(() => { if (hasError()) target.value?.focus() })
}
