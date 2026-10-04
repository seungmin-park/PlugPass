<script setup lang="ts">
import { ref } from 'vue'
import plugpassLogo from './shared/assets/plugpass-logo.svg'

const mainContent = ref<HTMLElement | null>(null)
</script>

<template>
  <div class="app-shell">
    <a href="#main-content" class="skip-link" @click.prevent="mainContent?.focus()">본문으로 건너뛰기</a>
    <header class="site-header">
      <RouterLink to="/stations" class="brand" aria-label="PlugPass 주변 검색">
        <img :src="plugpassLogo" alt="" width="144" height="44" />
      </RouterLink>
      <nav aria-label="주요 메뉴">
        <RouterLink to="/stations">
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="M6 3h8v18H6zM8 7h4M8 11h4m2-5h2l3 3v8a2 2 0 0 1-4 0v-3h-1m3-9 2 2" /></svg>
          주변 검색
        </RouterLink>
      </nav>
    </header>
    <main id="main-content" ref="mainContent" tabindex="-1"><RouterView /></main>
    <footer class="site-footer">
      <svg aria-hidden="true" viewBox="0 0 24 24"><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6zM8 12l3 3 5-6" /></svg>
      <p>충전기 상태와 관측 시각을 확인하고 이용 전 현장 상황을 확인하세요.</p>
    </footer>
  </div>
</template>
