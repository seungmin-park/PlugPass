<template>
  <div class="app-shell">
    <header class="site-header">
      <RouterLink to="/stations" class="brand" aria-label="PlugPass 주변 검색">
        <svg aria-hidden="true" viewBox="0 0 32 32" width="32" height="32">
          <rect x="2" y="2" width="28" height="28" rx="8" fill="currentColor" />
          <path d="M12 7v6m8-6v6M10 13h12v3a6 6 0 0 1-6 6v4m-6-13v3a6 6 0 0 0 6 6" fill="none" stroke="white" stroke-width="2.3" stroke-linecap="round" />
        </svg>
        <span>PlugPass</span>
      </RouterLink>
      <nav aria-label="주요 메뉴"><RouterLink to="/stations">주변 검색</RouterLink></nav>
    </header>
    <main id="main-content"><RouterView /></main>
    <footer>충전기 상태와 관측 시각을 확인하고 이용 전 현장 상황을 확인하세요.</footer>
  </div>
</template>
