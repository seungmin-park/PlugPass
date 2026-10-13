<script setup lang="ts">
import { computed } from 'vue'
import { createNavigationLinks, navigationPlatform, type NavigationDestination } from '../navigation/navigationLinks'

const props = defineProps<{ destination: NavigationDestination }>()
const platform = navigationPlatform(navigator.userAgent, navigator.maxTouchPoints)
const links = computed(() => createNavigationLinks(props.destination, platform, window.location.href))
</script>

<template>
  <details v-if="links" class="station-directions">
    <summary>길찾기</summary>
    <div class="directions-content">
      <p><strong>{{ destination.name }}</strong>까지 길찾기</p>
      <template v-if="links.apps.length">
        <p>자동차 길찾기를 열 앱을 선택하세요. 출발지는 지도 앱에서 확인해 주세요.</p>
        <div class="directions-actions">
          <a v-for="app in links.apps" :id="`navigation-${app.id}`" :key="app.id" :href="app.href">{{ app.name }}{{ app.id === 'naver' ? '로' : '으로' }} 길찾기</a>
        </div>
        <p>앱이 열리지 않으면 설치하거나 웹 지도를 이용하세요. 앱 안의 브라우저에서는 외부 브라우저로 열어 다시 시도해 주세요.</p>
        <div class="directions-actions install-actions">
          <a v-for="app in links.apps" :key="app.id" :href="app.installHref" :id="`install-${app.id}`" target="_blank" rel="noopener noreferrer">{{ app.name }} 설치</a>
        </div>
      </template>
      <p v-else>휴대폰에서는 카카오맵·네이버 지도 앱으로 연결할 수 있습니다.</p>
      <a :href="links.webHref" id="navigation-web" target="_blank" rel="noopener noreferrer">웹 지도에서 길찾기 (카카오맵)</a>
    </div>
  </details>
</template>

<style scoped>
.station-directions { margin-block: 1rem; border: 1px solid var(--border, #cbd5e1); border-radius: 12px; }
summary { padding: 14px 16px; cursor: pointer; font-weight: 700; }
summary:focus-visible, a:focus-visible { outline: 3px solid #2563eb; outline-offset: 3px; }
.directions-content { padding: 0 16px 16px; overflow-wrap: anywhere; }
.directions-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-block: 12px; }
.directions-actions a { display: inline-flex; align-items: center; min-height: 44px; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 8px; }
.install-actions a { font-size: .9rem; }
</style>
