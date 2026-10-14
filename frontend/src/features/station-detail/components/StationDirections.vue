<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import { createNavigationLinks, navigationPlatform, type NavigationDestination } from '../navigation/navigationLinks'
import { scheduleIosInstallFallback } from '../navigation/navigationHandoff'

const props = defineProps<{ destination: NavigationDestination; expanded?: boolean }>()
const platform = navigationPlatform(navigator.userAgent, navigator.maxTouchPoints)
const links = computed(() => createNavigationLinks(props.destination, platform))
const primaryHref = computed(() => links.value?.directHref)
function prepareDirections(event: MouseEvent): void {
  if (event.defaultPrevented || platform !== 'ios') return
  const installHref = links.value?.installHref
  if (installHref) scheduleIosInstallFallback(document, installHref, href => window.location.assign(href))
}
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
      <a v-if="primaryHref" id="navigation-primary" class="search-button" :href="primaryHref" :target="platform === 'web' ? '_blank' : undefined" rel="noopener noreferrer" @click="prepareDirections">길찾기</a>
      <button v-if="platform === 'web'" id="choose-directions" type="button" class="secondary-button" @click="openChooser">지도 앱 변경</button>
    </div>
    <section v-show="expanded || chooserOpen" ref="chooser" :class="{ 'directions-sheet': !expanded }" :role="expanded ? undefined : 'dialog'" :aria-labelledby="expanded ? undefined : titleId" tabindex="-1" @keydown.esc="handleEscape">
      <template v-if="!expanded">
        <button type="button" class="secondary-button sheet-close" @click="closeChooser">닫기</button>
        <h3 :id="titleId">{{ destination.name }} 길찾기</h3>
      </template>
      <a v-if="expanded && platform !== 'web'" id="navigation-primary" class="search-button" :href="primaryHref" @click="prepareDirections">길찾기</a>
      <a v-if="platform === 'web'" :href="links.webHref" id="navigation-web" target="_blank" rel="noopener noreferrer">웹 지도에서 길찾기 (카카오맵)</a>
      <details v-if="links.installHref" class="navigation-help">
        <summary>앱이 열리지 않나요?</summary>
        <p>앱이 열리지 않으면 설치하거나 웹 지도를 이용하세요. 출발지는 지도 앱에서 확인해 주세요.</p>
        <div class="directions-actions install-actions"><a :href="links.installHref" id="install-kakao" target="_blank" rel="noopener noreferrer">카카오맵 설치</a><a :href="links.webHref" id="navigation-web" target="_blank" rel="noopener noreferrer">웹 지도에서 길찾기</a></div>
      </details>
    </section>
  </div>
</template>
<style scoped>
.station-directions { margin-block: 12px; }
.station-directions .search-button { text-decoration: none; }
.directions-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-block: 12px; }
.directions-actions a { display: inline-flex; align-items: center; min-height: 44px; padding: 8px 12px; border: 1px solid var(--color-line); border-radius: 8px; font-weight: 600; }
.directions-sheet { position: fixed; z-index: 1100; bottom: 0; left: 50%; transform: translateX(-50%); width: min(100%, 560px); max-height: 60dvh; overflow-y: auto; padding: 20px; background: var(--color-panel); border: 1px solid var(--color-line); border-radius: 20px 20px 0 0; box-shadow: 0 -8px 32px #0b1c3020; }
.sheet-close { float: right; }
.navigation-help { margin-top: 16px; font-size: 13px; }
.navigation-help summary { padding-block: 10px; cursor: pointer; }
.install-actions a { font-size: 13px; }
</style>
