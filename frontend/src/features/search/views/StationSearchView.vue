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
    <div class="search-panel">
      <SearchForm @submit="guidance = '검색할 위치를 먼저 선택해 주세요.'" />
    </div>
    <p class="initial-guidance" role="status">{{ guidance }}</p>
  </section>
</template>
