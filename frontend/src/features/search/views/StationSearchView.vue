<script setup lang="ts">
import { ref } from 'vue'
import SearchForm from '../components/SearchForm.vue'

const guidance = ref('검색할 위치와 조건을 선택해 주세요.')
</script>

<template>
  <section aria-labelledby="search-heading" class="search-view">
    <div class="intro">
      <h1 id="search-heading">주변 충전소 찾기</h1>
      <p>차량에 맞는 충전소와 정보의 최신성을 함께 확인하세요.</p>
    </div>
    <div class="search-layout">
      <div class="search-panel">
        <div class="panel-heading">
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h16M8 3v6m8 0v6M8 15v6" /></svg>
          <h2>검색 조건</h2>
        </div>
        <p class="panel-description">내 차량에 맞는 커넥터와 검색 범위를 선택하세요.</p>
        <SearchForm @submit="guidance = '검색할 위치를 먼저 선택해 주세요.'" />
        <div class="location-guidance">
          <svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="7" /><circle cx="12" cy="12" r="2.5" /><path d="M12 2v3m0 14v3M2 12h3m14 0h3" /></svg>
          <div>
            <h3>위치 선택 안내</h3>
            <p class="initial-guidance" role="status">{{ guidance }}</p>
          </div>
        </div>
      </div>
      <aside class="search-explainer" aria-labelledby="information-heading">
        <div class="explainer-heading">
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6zM8 12l3 3 5-6" /></svg>
          <h2 id="information-heading">헛걸음을 줄이는 정보 확인</h2>
        </div>
        <p class="explainer-intro">이용 가능 보고만으로 도착 시점의 빈자리를 보장할 수는 없습니다.</p>
        <dl class="information-guide">
          <div>
            <dt><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>관측 시각</dt>
            <dd>현장에서 상태를 확인한 시각입니다. 정보가 언제 관측됐는지 확인하세요.</dd>
          </div>
          <div>
            <dt><svg aria-hidden="true" viewBox="0 0 24 24"><path d="M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5M4 16a8 8 0 0 0 14 3l3-3m0 5v-5h-5" /></svg>수집 시각</dt>
            <dd>서버가 정보를 받은 시각입니다. 최근에 수집됐어도 관측 정보는 오래됐을 수 있습니다.</dd>
          </div>
          <div>
            <dt><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="M12 10v7m0-10v.1" /></svg>이용 전 확인</dt>
            <dd>차량 커넥터와 함께 운영 시간·출입 조건을 확인하세요.</dd>
          </div>
        </dl>
      </aside>
    </div>
  </section>
</template>
