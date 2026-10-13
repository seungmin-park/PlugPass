<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import { createNavigationLinks, navigationPlatform, type NavigationDestination } from '../navigation/navigationLinks'
import { useNavigationPreferenceStore } from '../navigation/navigationPreferenceStore'

const props = defineProps<{ destination: NavigationDestination; expanded?: boolean }>()
const preference = useNavigationPreferenceStore()
const platform = navigationPlatform(navigator.userAgent, navigator.maxTouchPoints)
const links = computed(() => createNavigationLinks(props.destination, platform, window.location.href))
const preferredApp = computed(() => links.value?.apps.find(app => app.id === preference.app))
const primaryHref = computed(() => preferredApp.value?.href ?? (platform === 'web' ? links.value?.webHref : undefined))
const chooserOpen = ref(false)
const chooser = ref<HTMLElement | null>(null)
const titleId = useId()
let previousFocus: HTMLElement | null = null
async function openChooser(): Promise<void> {
  previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
  chooserOpen.value = true
  await nextTick()
  chooser.value?.focus()
}
function handleEscape(event: KeyboardEvent): void {
  if (props.expanded) return
  event.stopPropagation()
  event.preventDefault()
  closeChooser()
}
function closeChooser(): void {
  chooserOpen.value = false
  previousFocus?.focus()
}
watch(() => props.destination, closeChooser)
onBeforeUnmount(() => { if (chooserOpen.value) previousFocus?.focus() })
</script>
<template>
  <div v-if="links" class="station-directions">
    <div v-if="!expanded" class="directions-actions">
      <a v-if="primaryHref" id="navigation-primary" :href="primaryHref" :target="platform === 'web' ? '_blank' : undefined" rel="noopener noreferrer">{{ preferredApp ? `${preferredApp.name} 길찾기` : '길찾기' }}</a>
      <button v-else id="choose-directions" type="button" class="search-button" @click="openChooser">길찾기</button>
      <button v-if="primaryHref" id="choose-directions" type="button" class="secondary-button" @click="openChooser">지도 앱 변경</button>
    </div>
    <section v-show="expanded || chooserOpen" ref="chooser" :class="{ 'directions-sheet': !expanded }" :role="expanded ? undefined : 'dialog'" :aria-labelledby="expanded ? undefined : titleId" tabindex="-1" @keydown.esc="handleEscape">
      <template v-if="!expanded">
        <button type="button" class="secondary-button sheet-close" @click="closeChooser">닫기</button>
        <h3 :id="titleId">{{ destination.name }} 길찾기</h3>
      </template>
      <p v-if="links.apps.length">길찾기를 열 지도 앱을 선택하세요. 다음부터 이 앱으로 바로 연결합니다.</p>
      <div v-if="links.apps.length" class="directions-actions">
        <a v-for="app in links.apps" :id="`navigation-${app.id}`" :key="app.id" :href="app.href" @click="preference.selectApp(app.id)">{{ app.name }}{{ app.id === 'naver' ? '로' : '으로' }} 길찾기</a>
      </div>
      <a :href="links.webHref" id="navigation-web" target="_blank" rel="noopener noreferrer">웹 지도에서 길찾기 (카카오맵)</a>
      <details v-if="links.apps.length" class="navigation-help">
        <summary>앱이 열리지 않나요?</summary>
        <p>앱이 열리지 않으면 설치하거나 웹 지도를 이용하세요. 앱 안의 브라우저에서는 외부 브라우저로 열어 다시 시도해 주세요. 출발지는 지도 앱에서 확인해 주세요.</p>
        <div class="directions-actions install-actions"><a v-for="app in links.apps" :key="app.id" :href="app.installHref" :id="`install-${app.id}`" target="_blank" rel="noopener noreferrer">{{ app.name }} 설치</a></div>
      </details>
    </section>
  </div>
</template>
<style scoped>
.station-directions { margin-block: 12px; }
.directions-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-block: 12px; }
.directions-actions a { display: inline-flex; align-items: center; min-height: 44px; padding: 8px 12px; border: 1px solid var(--color-line); border-radius: 8px; font-weight: 600; }
.directions-sheet { position: fixed; z-index: 1100; bottom: 0; left: 50%; transform: translateX(-50%); width: min(100%, 560px); max-height: 60dvh; overflow-y: auto; padding: 20px; background: var(--color-panel); border: 1px solid var(--color-line); border-radius: 20px 20px 0 0; box-shadow: 0 -8px 32px #0b1c3020; }
.sheet-close { float: right; }
.navigation-help { margin-top: 16px; font-size: 13px; }
.navigation-help summary { padding-block: 10px; cursor: pointer; }
.install-actions a { font-size: 13px; }
</style>
